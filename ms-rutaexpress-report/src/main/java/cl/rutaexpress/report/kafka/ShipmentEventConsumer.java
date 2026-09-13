package cl.rutaexpress.report.kafka;

import cl.rutaexpress.report.domain.ShipmentEventLog;
import cl.rutaexpress.report.dto.EventEnvelope;
import cl.rutaexpress.report.repository.ShipmentEventLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Consume solo shipments.events (ver reparto de carga de trabajo: "Consume
 * shipments.events para agregaciones"), a diferencia de audit que también
 * escucha audit.timeline.
 */
@Component
public class ShipmentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ShipmentEventConsumer.class);

    private final ShipmentEventLogRepository repository;

    public ShipmentEventConsumer(ShipmentEventLogRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(
            topics = "shipments.events",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onMessage(EventEnvelope envelope, Acknowledgment ack, ConsumerRecord<String, EventEnvelope> record) {
        try {
            if (envelope.getEventId() == null) {
                log.warn("Evento sin eventId, se descarta: {}", envelope.getType());
                ack.acknowledge();
                return;
            }

            if (repository.existsByEventId(envelope.getEventId())) {
                log.info("Evento {} ya procesado (dedup), se ignora", envelope.getEventId());
                ack.acknowledge();
                return;
            }

            JsonNode payload = envelope.getPayload();
            String shipmentId = extractText(payload, "shipmentId");
            String status = extractText(payload, "status");
            String serviceType = extractText(payload, "serviceType");
            Instant occurredAt = envelope.getTimestamp() != null ? envelope.getTimestamp() : Instant.now();

            if (shipmentId == null || status == null) {
                log.warn("Evento {} sin shipmentId/status en el payload, se descarta", envelope.getEventId());
                ack.acknowledge();
                return;
            }

            repository.save(new ShipmentEventLog(envelope.getEventId(), shipmentId, status, serviceType, occurredAt));
            ack.acknowledge();
        } catch (DataIntegrityViolationException dup) {
            log.info("Evento {} ya existía (constraint UQ_SHIPMENT_EVENT_ID), se ignora", envelope.getEventId());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Error procesando evento {}: {}", envelope.getEventId(), ex.getMessage(), ex);
            throw ex; // el error handler reintenta y luego manda a shipments.events.DLT
        }
    }

    private String extractText(JsonNode payload, String field) {
        if (payload == null || !payload.hasNonNull(field)) {
            return null;
        }
        return payload.get(field).asText();
    }
}

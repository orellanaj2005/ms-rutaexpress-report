package cl.rutaexpress.report.kafka;

import cl.rutaexpress.report.dto.EventEnvelope;
import cl.rutaexpress.report.service.ReportAggregationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consume shipments.events para las agregaciones del reporte. Solo recibe, deja el contexto
 * de log (MDC), delega en {@link ReportAggregationService} y hace ack.
 *
 * Si el servicio lanza una excepción NO se hace ack: el error handler (KafkaConsumerConfig)
 * decide si reintenta o manda directo a report.DLT.
 */
@Component
public class ShipmentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ShipmentEventConsumer.class);

    private final ReportAggregationService service;

    public ShipmentEventConsumer(ReportAggregationService service) {
        this.service = service;
    }

    @KafkaListener(
            topics = "shipments.events",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, EventEnvelope> record, Acknowledgment ack) {
        EventEnvelope envelope = record.value();
        putMdc(record, envelope);
        try {
            ReportAggregationService.Result result = service.aggregate(envelope);
            log.info("Evento {} tipo={} -> {}", envelope.eventId(), envelope.type(), result);
            ack.acknowledge();
        } catch (DataIntegrityViolationException dup) {
            log.info("Evento {} ya existía (constraint única), se ignora", envelope.eventId());
            ack.acknowledge();
        } finally {
            MDC.clear();
        }
    }

    private static void putMdc(ConsumerRecord<String, EventEnvelope> record, EventEnvelope envelope) {
        MDC.put("topic", record.topic());
        MDC.put("partition", String.valueOf(record.partition()));
        MDC.put("offset", String.valueOf(record.offset()));
        if (envelope != null) {
            if (envelope.eventId() != null) {
                MDC.put("eventId", envelope.eventId());
            }
            if (envelope.traceId() != null) {
                MDC.put("traceId", envelope.traceId());
            }
        }
    }
}

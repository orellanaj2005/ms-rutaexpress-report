package cl.rutaexpress.report.service;

import cl.rutaexpress.report.domain.ShipmentEventLog;
import cl.rutaexpress.report.dto.EventEnvelope;
import cl.rutaexpress.report.dto.payload.ShipmentCreatedPayload;
import cl.rutaexpress.report.dto.payload.ShipmentStatusChangedPayload;
import cl.rutaexpress.report.kafka.EventTypes;
import cl.rutaexpress.report.kafka.EventValidator;
import cl.rutaexpress.report.repository.ShipmentEventLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Alimenta el read-model de reportes (SHIPMENT_EVENT_LOG) a partir de shipments.events:
 *  - ShipmentCreated       -> una fila con status CREADO y su serviceType (base de "total" y top-services)
 *  - ShipmentStatusChanged -> una fila con el nuevo status
 *  - cualquier otro tipo   -> se ignora (no es un error: otros servicios también publican en el tópico)
 */
@Service
public class ReportAggregationService {

    public enum Result { SAVED, DUPLICATE, IGNORED }

    public static final String STATUS_CREADO = "CREADO";

    private static final Logger log = LoggerFactory.getLogger(ReportAggregationService.class);

    private final ShipmentEventLogRepository repository;
    private final EventValidator validator;

    public ReportAggregationService(ShipmentEventLogRepository repository, EventValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    /**
     * @throws cl.rutaexpress.report.exception.InvalidEventException si el evento no cumple el contrato
     *         (no reintentable: va directo a la DLT)
     */
    @Transactional
    public Result aggregate(EventEnvelope envelope) {
        validator.validateEnvelope(envelope);

        boolean created = EventTypes.SHIPMENT_CREATED.equals(envelope.type());
        boolean statusChanged = EventTypes.SHIPMENT_STATUS_CHANGED.equals(envelope.type());
        if (!created && !statusChanged) {
            log.debug("Tipo {} no interesa a report, se ignora", envelope.type());
            return Result.IGNORED;
        }

        if (repository.existsByEventId(envelope.eventId())) {
            log.info("Evento {} ya procesado (dedup), se ignora", envelope.eventId());
            return Result.DUPLICATE;
        }

        ShipmentEventLog row;
        if (created) {
            ShipmentCreatedPayload p = validator.payload(envelope, ShipmentCreatedPayload.class);
            row = new ShipmentEventLog(envelope.eventId(), p.shipmentId(), STATUS_CREADO,
                    p.serviceType(), envelope.timestamp());
        } else {
            ShipmentStatusChangedPayload p = validator.payload(envelope, ShipmentStatusChangedPayload.class);
            row = new ShipmentEventLog(envelope.eventId(), p.shipmentId(),
                    p.status().trim().toUpperCase(Locale.ROOT), p.serviceType(), envelope.timestamp());
        }

        repository.saveAndFlush(row);
        return Result.SAVED;
    }
}

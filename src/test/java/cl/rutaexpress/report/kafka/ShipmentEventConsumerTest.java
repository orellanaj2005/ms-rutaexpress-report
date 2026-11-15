package cl.rutaexpress.report.kafka;

import cl.rutaexpress.report.dto.EventEnvelope;
import cl.rutaexpress.report.exception.InvalidEventException;
import cl.rutaexpress.report.repository.ShipmentEventLogRepository;
import cl.rutaexpress.report.service.ReportAggregationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.support.Acknowledgment;

import static cl.rutaexpress.report.kafka.EventTestSupport.envelope;
import static cl.rutaexpress.report.kafka.EventTestSupport.validator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Se usa el servicio real y solo se simula el repositorio (una interfaz): así la prueba
 * no depende de que Mockito pueda instrumentar clases con la versión de Java instalada.
 */
class ShipmentEventConsumerTest {

    private final ShipmentEventLogRepository repository = mock(ShipmentEventLogRepository.class);
    private final Acknowledgment ack = mock(Acknowledgment.class);
    private final ShipmentEventConsumer consumer =
            new ShipmentEventConsumer(new ReportAggregationService(repository, validator()));

    private final EventEnvelope env = envelope("evt-1", "ShipmentCreated", "{\"shipmentId\":\"SHP-1\"}");
    private final ConsumerRecord<String, EventEnvelope> record =
            new ConsumerRecord<>("shipments.events", 1, 7L, "SHP-1", env);

    @Test
    void agregaYHaceAck() {
        consumer.onMessage(record, ack);

        verify(repository).saveAndFlush(any());
        verify(ack).acknowledge();
    }

    @Test
    void tipoQueNoInteresaSeIgnoraPeroSiHaceAck() {
        EventEnvelope otro = envelope("evt-2", "AuditAction", "{\"action\":\"X\",\"actor\":\"a\"}");
        var rec = new ConsumerRecord<String, EventEnvelope>("shipments.events", 1, 8L, "SHP-1", otro);

        consumer.onMessage(rec, ack);

        verify(repository, never()).saveAndFlush(any());
        verify(ack).acknowledge();
    }

    @Test
    void duplicadoNoGuardaPeroSiHaceAck() {
        when(repository.existsByEventId("evt-1")).thenReturn(true);

        consumer.onMessage(record, ack);

        verify(repository, never()).saveAndFlush(any());
        verify(ack).acknowledge();
    }

    @Test
    void carreraPorConstraintUnicaHaceAck() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("UQ_SHIPMENT_EVENT_ID"));

        consumer.onMessage(record, ack);

        verify(ack).acknowledge();
    }

    @Test
    void eventoInvalidoPropagaYNoHaceAckParaQueElErrorHandlerLoMandeALaDlt() {
        EventEnvelope malo = envelope("evt-3", "ShipmentStatusChanged",
                "{\"shipmentId\":\"SHP-1\",\"newStatus\":\"EN_RUTA\"}");
        var rec = new ConsumerRecord<String, EventEnvelope>("shipments.events", 0, 1L, "SHP-1", malo);

        assertThatThrownBy(() -> consumer.onMessage(rec, ack)).isInstanceOf(InvalidEventException.class);

        verify(ack, never()).acknowledge();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void limpiaElMdcAlTerminarAunqueFalle() {
        EventEnvelope malo = envelope("evt-4", "ShipmentCreated", "{}");
        var rec = new ConsumerRecord<String, EventEnvelope>("shipments.events", 0, 1L, "SHP-1", malo);

        assertThatThrownBy(() -> consumer.onMessage(rec, ack)).isInstanceOf(InvalidEventException.class);

        assertThat(MDC.get("eventId")).isNull();
    }
}
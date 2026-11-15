package cl.rutaexpress.report.service;

import cl.rutaexpress.report.domain.ShipmentEventLog;
import cl.rutaexpress.report.exception.InvalidEventException;
import cl.rutaexpress.report.repository.ShipmentEventLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static cl.rutaexpress.report.kafka.EventTestSupport.TS;
import static cl.rutaexpress.report.kafka.EventTestSupport.envelope;
import static cl.rutaexpress.report.kafka.EventTestSupport.validator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportAggregationServiceTest {

    private final ShipmentEventLogRepository repository = mock(ShipmentEventLogRepository.class);
    private final ReportAggregationService service = new ReportAggregationService(repository, validator());

    private ShipmentEventLog saved() {
        ArgumentCaptor<ShipmentEventLog> captor = ArgumentCaptor.forClass(ShipmentEventLog.class);
        verify(repository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    void shipmentCreatedSeCuentaComoCreadoConSuServiceType() {
        var env = envelope("evt-1", "ShipmentCreated",
                "{\"shipmentId\":\"SHP-9\",\"serviceType\":\"EXPRESS\"}");

        var result = service.aggregate(env);

        assertThat(result).isEqualTo(ReportAggregationService.Result.SAVED);
        ShipmentEventLog row = saved();
        assertThat(row.getEventId()).isEqualTo("evt-1");
        assertThat(row.getShipmentId()).isEqualTo("SHP-9");
        assertThat(row.getStatus()).isEqualTo("CREADO");
        assertThat(row.getServiceType()).isEqualTo("EXPRESS");
        assertThat(row.getOccurredAt()).isEqualTo(TS);
    }

    @Test
    void statusChangedUsaElCampoStatusYLoNormalizaAMayusculas() {
        var env = envelope("evt-2", "ShipmentStatusChanged",
                "{\"shipmentId\":\"SHP-9\",\"status\":\" en_ruta \"}");

        service.aggregate(env);

        assertThat(saved().getStatus()).isEqualTo("EN_RUTA");
    }

    @Test
    void statusChangedConNewStatusEnVezDeStatusEsInvalido() {
        var env = envelope("evt-3", "ShipmentStatusChanged",
                "{\"shipmentId\":\"SHP-9\",\"newStatus\":\"EN_RUTA\"}");

        assertThatThrownBy(() -> service.aggregate(env)).isInstanceOf(InvalidEventException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void statusDemasiadoLargoParaLaColumnaEsInvalidoYNoLlegaALaBd() {
        var env = envelope("evt-4", "ShipmentStatusChanged",
                "{\"shipmentId\":\"SHP-9\",\"status\":\"" + "X".repeat(31) + "\"}");

        assertThatThrownBy(() -> service.aggregate(env)).isInstanceOf(InvalidEventException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void tiposQueNoSonDeEnviosSeIgnoran() {
        var env = envelope("evt-5", "AuditAction", "{\"action\":\"X\",\"actor\":\"a\"}");

        var result = service.aggregate(env);

        assertThat(result).isEqualTo(ReportAggregationService.Result.IGNORED);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void eventoDuplicadoNoSeVuelveAGuardar() {
        when(repository.existsByEventId("evt-1")).thenReturn(true);
        var env = envelope("evt-1", "ShipmentCreated", "{\"shipmentId\":\"SHP-9\"}");

        var result = service.aggregate(env);

        assertThat(result).isEqualTo(ReportAggregationService.Result.DUPLICATE);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void eventoSinEventIdEsInvalido() {
        var env = envelope(null, "ShipmentCreated", "{\"shipmentId\":\"SHP-9\"}");

        assertThatThrownBy(() -> service.aggregate(env)).isInstanceOf(InvalidEventException.class);
    }
}

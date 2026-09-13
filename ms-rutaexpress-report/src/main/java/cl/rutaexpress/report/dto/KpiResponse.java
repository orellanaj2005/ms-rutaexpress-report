package cl.rutaexpress.report.dto;

import java.util.List;

public record KpiResponse(
        String range,
        List<StatusCountRow> shipmentsByStatus,
        Double averageLeadTimeMinutes,
        long activeShipments
) {
}

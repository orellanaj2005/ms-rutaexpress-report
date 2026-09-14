package cl.rutaexpress.report.service;

import cl.rutaexpress.report.domain.ShipmentEventLog;
import cl.rutaexpress.report.dto.KpiResponse;
import cl.rutaexpress.report.dto.StatusCountRow;
import cl.rutaexpress.report.dto.TopServiceRow;
import cl.rutaexpress.report.repository.ShipmentEventLogRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReportService {

    private static final Set<String> ACTIVE_STATUSES = Set.of("ACEPTADO", "EN_BODEGA", "EN_RUTA");

    private final ShipmentEventLogRepository repository;

    public ReportService(ShipmentEventLogRepository repository) {
        this.repository = repository;
    }

    public KpiResponse kpis(String range) {
        Instant from = resolveFrom(range);

        List<StatusCountRow> byStatus = repository.countByStatusSince(from);

        long active = byStatus.stream()
                .filter(row -> ACTIVE_STATUSES.contains(row.status()))
                .mapToLong(StatusCountRow::count)
                .sum();

        Double avgLeadTimeMinutes = computeAverageLeadTimeMinutes(from);

        return new KpiResponse(range, byStatus, avgLeadTimeMinutes, active);
    }

    public List<TopServiceRow> topServices(String range) {
        return repository.topServicesSince(resolveFrom(range));
    }

    private Double computeAverageLeadTimeMinutes(Instant from) {
        List<ShipmentEventLog> rows = repository.findCreatedAndDeliveredSince(from);

        Map<String, Instant> createdAt = new HashMap<>();
        List<Duration> leadTimes = new java.util.ArrayList<>();

        for (ShipmentEventLog row : rows) {
            if ("CREADO".equals(row.getStatus())) {
                createdAt.putIfAbsent(row.getShipmentId(), row.getOccurredAt());
            } else if ("ENTREGADO".equals(row.getStatus())) {
                Instant created = createdAt.get(row.getShipmentId());
                if (created != null) {
                    leadTimes.add(Duration.between(created, row.getOccurredAt()));
                }
            }
        }

        if (leadTimes.isEmpty()) {
            return null;
        }

        double avgSeconds = leadTimes.stream()
                .mapToLong(Duration::getSeconds)
                .average()
                .orElse(0);

        return avgSeconds / 60.0;
    }

    private Instant resolveFrom(String range) {
        // Rangos que pide la pauta: last24h, last7d. Se agrega last30d por si acaso.
        return switch (range == null ? "last24h" : range) {
            case "last7d" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "last30d" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default -> Instant.now().minus(24, ChronoUnit.HOURS);
        };
    }
}

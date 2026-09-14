package cl.rutaexpress.report.repository;

import cl.rutaexpress.report.domain.ShipmentEventLog;
import cl.rutaexpress.report.dto.StatusCountRow;
import cl.rutaexpress.report.dto.TopServiceRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ShipmentEventLogRepository extends JpaRepository<ShipmentEventLog, Long> {

    boolean existsByEventId(String eventId);

    // GET /api/report/kpis?range=... -> envíos por estado en la ventana
    @Query("""
            SELECT new cl.rutaexpress.report.dto.StatusCountRow(s.status, COUNT(s))
            FROM ShipmentEventLog s
            WHERE s.occurredAt >= :from
            GROUP BY s.status
            """)
    List<StatusCountRow> countByStatusSince(@Param("from") Instant from);

    // Lead time promedio: diferencia entre el primer CREADO y el ENTREGADO de cada envío,
    // promediada en segundos. Se calcula en memoria porque Oracle/JPQL no tiene una función
    // portable para restar timestamps entre filas distintas sin una subconsulta correlacionada.
    @Query("""
            SELECT s FROM ShipmentEventLog s
            WHERE s.status IN ('CREADO', 'ENTREGADO')
              AND s.occurredAt >= :from
            ORDER BY s.shipmentId, s.occurredAt
            """)
    List<ShipmentEventLog> findCreatedAndDeliveredSince(@Param("from") Instant from);

    // GET /api/report/top-services?range=... -> servicios más usados (por envíos CREADOS)
    @Query("""
            SELECT new cl.rutaexpress.report.dto.TopServiceRow(s.serviceType, COUNT(s))
            FROM ShipmentEventLog s
            WHERE s.status = 'CREADO'
              AND s.occurredAt >= :from
              AND s.serviceType IS NOT NULL
            GROUP BY s.serviceType
            ORDER BY COUNT(s) DESC
            """)
    List<TopServiceRow> topServicesSince(@Param("from") Instant from);
}

package cl.rutaexpress.report.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Read-model alimentado por shipments.events. No es la fuente de verdad
 * (esa es shipments/Oracle de Jassack) — es solo lo necesario para
 * calcular KPIs y top-services sin pegarle en caliente al servicio
 * transaccional (ver Caso 3, sección 3: "Datos por streaming, sin
 * bloquear el core").
 */
@Entity
@Table(name = "SHIPMENT_EVENT_LOG", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_SHIPMENT_EVENT_ID", columnNames = "EVENT_ID")
})
public class ShipmentEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shipment_event_seq")
    @SequenceGenerator(name = "shipment_event_seq", sequenceName = "SHIPMENT_EVENT_SEQ", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "EVENT_ID", nullable = false, length = 100)
    private String eventId;

    @Column(name = "SHIPMENT_ID", nullable = false, length = 100)
    private String shipmentId;

    @Column(name = "STATUS", nullable = false, length = 30)
    private String status;

    @Column(name = "SERVICE_TYPE", length = 100)
    private String serviceType;

    @Column(name = "OCCURRED_AT", nullable = false)
    private Instant occurredAt;

    protected ShipmentEventLog() {
    }

    public ShipmentEventLog(String eventId, String shipmentId, String status, String serviceType, Instant occurredAt) {
        this.eventId = eventId;
        this.shipmentId = shipmentId;
        this.status = status;
        this.serviceType = serviceType;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public String getStatus() {
        return status;
    }

    public String getServiceType() {
        return serviceType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}

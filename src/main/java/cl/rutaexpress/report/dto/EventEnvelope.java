package cl.rutaexpress.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Mismo contrato común acordado con el equipo (ver ms-rutaexpress-audit,
 * que usa una copia idéntica de este DTO). Si más adelante conviene, se
 * puede mover a una librería compartida entre audit y report.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventEnvelope {

    private String type;
    private String eventId;
    private Instant timestamp;
    private String traceId;
    private String correlationId;
    private JsonNode payload;

    public EventEnvelope() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}

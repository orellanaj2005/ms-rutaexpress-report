package cl.rutaexpress.report.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload de {@code ShipmentCreated}: report cuenta el envío como CREADO y su tipo de servicio. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShipmentCreatedPayload(
        @NotBlank String shipmentId,
        @Size(max = 100) String serviceType
) {
}

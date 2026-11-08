package cl.rutaexpress.report.dto.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload de {@code ShipmentStatusChanged}. El nuevo estado viaja en {@code status} (no en newStatus). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShipmentStatusChangedPayload(
        @NotBlank String shipmentId,
        @NotBlank @Size(max = 30) String status,
        @Size(max = 100) String serviceType
) {
}

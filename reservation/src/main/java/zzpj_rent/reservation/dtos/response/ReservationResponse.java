package zzpj_rent.reservation.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationResponse {
    private Long id;
    private Long propertyId;
    private Long tenantId;
    private String tenantName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}

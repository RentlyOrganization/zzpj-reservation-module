package zzpj_rent.reservation.dtos.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReservationRequest {
    private Long propertyId;
    private Long tenantId;
    private LocalDate startDate;
    private LocalDate endDate;
}

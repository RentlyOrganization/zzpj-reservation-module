package zzpj_rent.reservation.dtos.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateReservationRequest {
    private LocalDate startDate;
    private LocalDate endDate;
}

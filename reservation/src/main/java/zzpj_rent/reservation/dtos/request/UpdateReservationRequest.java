package zzpj_rent.reservation.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateReservationRequest {
    private LocalDate startDate;
    private LocalDate endDate;
}

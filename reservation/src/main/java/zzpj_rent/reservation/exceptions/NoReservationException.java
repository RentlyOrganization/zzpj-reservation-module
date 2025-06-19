package zzpj_rent.reservation.exceptions;

import org.springframework.http.HttpStatus;

public final class NoReservationException extends ReservationException {
    public NoReservationException() {
        super(HttpStatus.NOT_FOUND, "Reservation not found");
    }
}

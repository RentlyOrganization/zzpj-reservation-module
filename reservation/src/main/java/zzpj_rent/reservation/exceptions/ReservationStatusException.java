package zzpj_rent.reservation.exceptions;

import org.springframework.http.HttpStatus;

public final class ReservationStatusException extends ReservationException {
    public ReservationStatusException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}

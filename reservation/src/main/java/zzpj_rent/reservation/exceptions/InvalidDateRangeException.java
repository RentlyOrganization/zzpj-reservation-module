package zzpj_rent.reservation.exceptions;

import org.springframework.http.HttpStatus;

public final class InvalidDateRangeException extends ReservationException {
    public InvalidDateRangeException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}

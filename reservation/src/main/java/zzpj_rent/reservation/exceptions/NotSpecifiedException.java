package zzpj_rent.reservation.exceptions;

import org.springframework.http.HttpStatus;

public final class NotSpecifiedException extends ReservationException {
    public NotSpecifiedException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}

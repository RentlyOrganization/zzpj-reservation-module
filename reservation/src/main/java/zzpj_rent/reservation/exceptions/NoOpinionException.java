package zzpj_rent.reservation.exceptions;

import org.springframework.http.HttpStatus;

public final class NoOpinionException extends ReservationException {
    public NoOpinionException() {
        super(HttpStatus.NOT_FOUND, "Nie znaleziono opinii");
    }
}

package zzpj_rent.reservation.exceptions;

import org.springframework.http.HttpStatus;

public final class NoPropertyException extends ReservationException {
    public NoPropertyException() {
        super(HttpStatus.NOT_FOUND, "Property not found with given ID.");
    }
}

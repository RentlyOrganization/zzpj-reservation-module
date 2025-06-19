package zzpj_rent.reservation.exceptions;

import org.springframework.http.HttpStatus;

public final class OwnerException extends ReservationException {
  public OwnerException(String message) {
    super(HttpStatus.BAD_REQUEST, message);
  }
}

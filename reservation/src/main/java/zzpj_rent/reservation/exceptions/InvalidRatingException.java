package zzpj_rent.reservation.exceptions;

import org.springframework.http.HttpStatus;

public final class InvalidRatingException extends ReservationException {
  public InvalidRatingException() {
    super(HttpStatus.BAD_REQUEST, "Invalid rating");
  }
}

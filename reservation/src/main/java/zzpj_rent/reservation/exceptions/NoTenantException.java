package zzpj_rent.reservation.exceptions;

import org.springframework.http.HttpStatus;

public final class NoTenantException extends ReservationException {
  public NoTenantException() {
    super(HttpStatus.NOT_FOUND, "No tenant found with given ID.");
  }
}

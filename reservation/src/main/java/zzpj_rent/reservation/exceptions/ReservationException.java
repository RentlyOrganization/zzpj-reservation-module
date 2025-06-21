package zzpj_rent.reservation.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public sealed class ReservationException extends RuntimeException permits InvalidDateRangeException,
    NoPropertyException, ReservationStatusException, NoReservationException, NoTenantException,
    OwnerException, NotSpecifiedException, InvalidRatingException, NoOpinionException {
    @Getter
    private final HttpStatus status;
    private final String message;
    @Getter
    private final LocalDateTime timestamp;

    protected ReservationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String getMessage() {
        return message;
    }

}
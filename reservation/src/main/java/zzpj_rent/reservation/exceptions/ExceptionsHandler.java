package zzpj_rent.reservation.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import zzpj_rent.reservation.dtos.response.ErrorMessage;

@RestControllerAdvice
public class ExceptionsHandler {

    @ExceptionHandler(ReservationException.class)
    public ResponseEntity<ErrorMessage> handleReservationException(ReservationException ex) {
        ErrorMessage errorResponse = new ErrorMessage(
                ex.getStatus(),
                ex.getMessage(),
                ex.getTimestamp()
        );
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }
}

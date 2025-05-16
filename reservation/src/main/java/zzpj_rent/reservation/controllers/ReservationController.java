package zzpj_rent.reservation.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zzpj_rent.reservation.dtos.request.ReservationRequest;
import zzpj_rent.reservation.model.Reservation;
import zzpj_rent.reservation.services.ReservationService;

@RestController
@RequestMapping("/api/rent")
public class ReservationController {
    @Autowired
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/create")
    public ResponseEntity<Reservation> reserve(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.createReservation(request));
    }
}

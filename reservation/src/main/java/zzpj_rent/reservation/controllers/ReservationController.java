package zzpj_rent.reservation.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zzpj_rent.reservation.dtos.request.ReservationRequest;
import zzpj_rent.reservation.dtos.request.UpdateReservationRequest;
import zzpj_rent.reservation.dtos.response.ReservationResponse;
import zzpj_rent.reservation.dtos.response.SuccessMessage;
import zzpj_rent.reservation.model.Reservation;
import zzpj_rent.reservation.services.ReservationService;

import java.util.List;

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

    @GetMapping("/reservations/tenant/all")
    public ResponseEntity<List<ReservationResponse>> getAllReservationsForTenant(@RequestParam Long tenantId,
                                                                                 @RequestParam(required = false) Reservation.Status status) {

        List<ReservationResponse> reservations = (status == null)
                ? reservationService.getAllReservationsForTenant(tenantId)
                : reservationService.getReservationsForTenantByStatus(tenantId, status);

        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/reservations/{id}/tenant/{tenantId}")
    public ResponseEntity<ReservationResponse> getReservationByIdTenant(@PathVariable Long id,
                                                                  @PathVariable Long tenantId) {
        return ResponseEntity.ok(reservationService.getReservationByIdTenant(id, tenantId));
    }

    @PatchMapping("/status/{id}")
    public ResponseEntity<SuccessMessage> updateReservationStatus(@PathVariable Long id,
                                                                  @RequestParam Reservation.Status status) {
        return ResponseEntity.ok(new SuccessMessage(reservationService.updateReservationStatus(id, status)));
    }

    @DeleteMapping("/delete/{id}/tenant/{tenantId}")
    public ResponseEntity<SuccessMessage> deleteReservation(@PathVariable Long id,
                                                            @PathVariable Long tenantId) {
        return ResponseEntity.ok(new SuccessMessage(reservationService.deleteReservation(id, tenantId)));
    }

    @PatchMapping("update/{id}/tenant/{tenantId}")
    public ResponseEntity<SuccessMessage> updateReservation(@PathVariable Long id,
                                                            @PathVariable Long tenantId,
                                                            @RequestBody UpdateReservationRequest request) {
        return ResponseEntity.ok(new SuccessMessage(reservationService.updateReservation(id, tenantId, request)));
    }

    @GetMapping("/reservations/owner/all")
    public ResponseEntity<List<ReservationResponse>> getAllReservationsForOwner(@RequestParam Long propertyId,
                                                                                 @RequestParam Long ownerId) {
        return ResponseEntity.ok(reservationService.getAllReservationsForOwner(propertyId, ownerId));
    }

    @GetMapping("/reservations/{id}/owner/{ownerId}")
    public ResponseEntity<ReservationResponse> getReservationByIdOwner(@PathVariable Long id,
                                                                  @PathVariable Long ownerId) {
        return ResponseEntity.ok(reservationService.getReservationByIdOwner(id, ownerId));
    }

}

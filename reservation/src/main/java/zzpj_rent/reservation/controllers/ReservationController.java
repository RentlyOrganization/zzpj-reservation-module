package zzpj_rent.reservation.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zzpj_rent.reservation.dtos.request.OpinionRequest;
import zzpj_rent.reservation.dtos.request.ReservationRequest;
import zzpj_rent.reservation.dtos.request.UpdateReservationRequest;
import zzpj_rent.reservation.dtos.response.ErrorMessage;
import zzpj_rent.reservation.dtos.response.OpinionResponse;
import zzpj_rent.reservation.dtos.response.ReservationResponse;
import zzpj_rent.reservation.dtos.response.SuccessMessage;
import zzpj_rent.reservation.model.Opinion;
import zzpj_rent.reservation.model.Reservation;
import zzpj_rent.reservation.services.ReservationService;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600)
@RestController
@RequestMapping("/api/rent")
@Tag(name = "Reservation API", description = "Endpoints for managing reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "Create a new reservation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pomyślnie utworzono rezerwację",
                    content = @Content(schema = @Schema(implementation = Reservation.class))),
            @ApiResponse(responseCode = "400", description = "Niepoprawne dane",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Nie znaleziono zasobu",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Błąd serwera",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PostMapping("/create")
    public ResponseEntity<Reservation> reserve(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.createReservation(request));
    }

    @Operation(summary = "Get all reservations for a tenant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pomyślnie pobrano listę rezerwacji",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "500", description = "Błąd serwera",
                    content = @Content)
    })
    @GetMapping("/reservations/tenant/all")
    public ResponseEntity<List<ReservationResponse>> getAllReservationsForTenant(
            @Parameter(description = "ID of the tenant") @RequestParam Long tenantId,
            @Parameter(description = "Status of the reservation") @RequestParam(required = false) Reservation.Status status) {

        List<ReservationResponse> reservations = (status == null)
                ? reservationService.getAllReservationsForTenant(tenantId)
                : reservationService.getReservationsForTenantByStatus(tenantId, status);

        return ResponseEntity.ok(reservations);
    }

    @Operation(summary = "Get a reservation by ID for a tenant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pomyślnie pobrano rezerwację",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Nie znaleziono zasobu",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
    })
    @GetMapping("/reservations/{id}/tenant/{tenantId}")
    public ResponseEntity<ReservationResponse> getReservationByIdTenant(
            @Parameter(description = "Reservation ID") @PathVariable Long id,
            @Parameter(description = "Tenant ID") @PathVariable Long tenantId) {
        return ResponseEntity.ok(reservationService.getReservationByIdForTenant(id, tenantId));
    }

    @Operation(summary = "Update reservation status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pomyślnie zaktualizowano status",
                    content = @Content(schema = @Schema(implementation = SuccessMessage.class))),
            @ApiResponse(responseCode = "400", description = "Niepoprawny status",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Nie znaleziono rezerwacji",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PatchMapping("/status/{id}")
    public ResponseEntity<SuccessMessage> updateReservationStatus(
            @Parameter(description = "Reservation ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam Reservation.Status status) {
        return ResponseEntity.ok(new SuccessMessage(reservationService.updateReservationStatus(id, status)));
    }

    @Operation(summary = "Delete a reservation for a tenant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pomyślnie usunięto rezerwację",
                    content = @Content(schema = @Schema(implementation = SuccessMessage.class))),
            @ApiResponse(responseCode = "400", description = "Nie znaleziono rezerwacji",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Nie można usunąć rezerwacji",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @DeleteMapping("/delete/{id}/tenant/{tenantId}")
    public ResponseEntity<SuccessMessage> deleteReservation(
            @Parameter(description = "Reservation ID") @PathVariable Long id,
            @Parameter(description = "Tenant ID") @PathVariable Long tenantId) {
        return ResponseEntity.ok(new SuccessMessage(reservationService.deleteReservation(id, tenantId)));
    }

    @Operation(summary = "Update a reservation for a tenant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pomyślnie zaktualizowano rezerwację",
                    content = @Content(schema = @Schema(implementation = SuccessMessage.class))),
            @ApiResponse(responseCode = "400", description = "Niepoprawne dane",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Nie znaleziono rezerwacji",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PatchMapping("update/{id}/tenant/{tenantId}")
    public ResponseEntity<SuccessMessage> updateReservation(
            @Parameter(description = "Reservation ID") @PathVariable Long id,
            @Parameter(description = "Tenant ID") @PathVariable Long tenantId,
            @RequestBody UpdateReservationRequest request) {
        return ResponseEntity.ok(new SuccessMessage(reservationService.updateReservation(id, tenantId, request)));
    }

    @Operation(summary = "Get all reservations for an owner")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pomyślnie pobrano listę rezerwacji",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Nie jesteś właścicielem",
            content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Błąd serwera",
                    content = @Content)
    })
    @GetMapping("/reservations/owner/all")
    public ResponseEntity<List<ReservationResponse>> getAllReservationsForOwner(
            @Parameter(description = "Property ID") @RequestParam Long propertyId,
            @Parameter(description = "Owner ID") @RequestParam Long ownerId)
    {
        return ResponseEntity.ok(reservationService.getAllReservationsForOwner(propertyId, ownerId));
    }

    @Operation(summary = "Get a reservation by ID for an owner")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pomyślnie pobrano listę rezerwacji",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Nie jesteś właścicielem",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Nie znaleziono zasobu",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "500", description = "Błąd serwera",
                    content = @Content)
    })
    @GetMapping("/reservations/{id}/owner/{ownerId}")
    public ResponseEntity<ReservationResponse> getReservationByIdOwner(
            @Parameter(description = "Reservation ID") @PathVariable Long id,
            @Parameter(description = "Owner ID") @PathVariable Long ownerId) {
        return ResponseEntity.ok(reservationService.getReservationByIdForOwner(id, ownerId));
    }

    @Operation(summary = "Add an opinion to a reservation")
    @PostMapping("/opinion/{reservationId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pomyślnie dodano opinię",
                    content = @Content(schema = @Schema(implementation = SuccessMessage.class))),
            @ApiResponse(responseCode = "400", description = "Niepoprawne dane",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Nie udało się dodać opini",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    public ResponseEntity<Opinion> createOpinion(
            @Parameter(description = "Reservation ID") @PathVariable Long reservationId,
            @Parameter(description = "Creator ID") @RequestParam Long userId,
            @Parameter(description = "Opinion content") @RequestBody OpinionRequest opinion) {
        return ResponseEntity.ok(reservationService.createOpinion(reservationId, userId, opinion));
    }

    @Operation(summary = "Get all opinions for user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pomyślnie wyszukano opinii",
                    content = @Content(schema = @Schema(implementation = SuccessMessage.class))),
            @ApiResponse(responseCode = "500", description = "Błąd serwera",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @GetMapping("/opinion/{userId}")
    public ResponseEntity<List<OpinionResponse>> getAllOpinionsByUser(
            @Parameter(description = "UserId") @PathVariable Long userId) {
        return ResponseEntity.ok(reservationService.getAllOpinionsByUser(userId));
    }

    @Operation(summary = "Delete opinion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pomyślnie usunięto opinię",
                    content = @Content(schema = @Schema(implementation = SuccessMessage.class))),
            @ApiResponse(responseCode = "400", description = "Nie znaleziono opinii",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Nie można usunąć opinii",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @DeleteMapping("/opinion/{opinionId}")
    public ResponseEntity<SuccessMessage> deleteOpinion(
            @Parameter(description = "Opinion ID") @PathVariable Long opinionId,
            @Parameter(description = "User ID") @RequestParam Long userId) {
        return ResponseEntity.ok(new SuccessMessage(reservationService.deleteOpinion(userId, opinionId)));
    }

}

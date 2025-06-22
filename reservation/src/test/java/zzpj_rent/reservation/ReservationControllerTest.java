package zzpj_rent.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import zzpj_rent.reservation.controllers.ReservationController;
import zzpj_rent.reservation.dtos.request.OpinionRequest;
import zzpj_rent.reservation.dtos.request.ReservationRequest;
import zzpj_rent.reservation.dtos.request.UpdateReservationRequest;
import zzpj_rent.reservation.dtos.response.OpinionResponse;
import zzpj_rent.reservation.dtos.response.ReservationResponse;
import zzpj_rent.reservation.exceptions.NoOpinionException;
import zzpj_rent.reservation.exceptions.NoReservationException;
import zzpj_rent.reservation.model.Opinion;
import zzpj_rent.reservation.model.Reservation;
import zzpj_rent.reservation.model.User;
import zzpj_rent.reservation.services.ReservationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @Test
    void reserve_ShouldReturn200AndReservation() throws Exception {
        // given
        Reservation reservation = new Reservation();
        reservation.setId(1L);

        when(reservationService.createReservation(any(ReservationRequest.class)))
                .thenReturn(reservation);

        // when + then
        mockMvc.perform(post("/api/rent/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "someField": "someValue"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getAllReservationsForTenant_WithoutStatus_ShouldReturnList() throws Exception {
        // given
        ReservationResponse resp1 = new ReservationResponse();
        resp1.setId(1L);
        List<ReservationResponse> responses = List.of(resp1);

        when(reservationService.getAllReservationsForTenant(99L)).thenReturn(responses);

        // when + then
        mockMvc.perform(get("/api/rent/reservations/tenant/all")
                        .param("tenantId", "99"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getAllReservationsForTenant_WithStatus_ShouldReturnList() throws Exception {
        // given
        ReservationResponse resp1 = new ReservationResponse();
        resp1.setId(1L);
        List<ReservationResponse> responses = List.of(resp1);

        when(reservationService.getReservationsForTenantByStatus(99L, Reservation.Status.CONFIRMED))
                .thenReturn(responses);

        // when + then
        mockMvc.perform(get("/api/rent/reservations/tenant/all")
                        .param("tenantId", "99")
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void getReservationByIdTenant_ShouldReturnReservationResponse() throws Exception {
        Long reservationId = 1L;
        Long tenantId = 10L;
        ReservationResponse response = new ReservationResponse(
                reservationId,
                123L,
                tenantId,
                "Name",
                "Surname",
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                Reservation.Status.CONFIRMED.toString(),
                Reservation.Payment.ONE_TIME.name(),
                BigDecimal.valueOf(1000)
        );

        when(reservationService.getReservationByIdForTenant(reservationId, tenantId))
                .thenReturn(response);

        mockMvc.perform(get("/api/rent/reservations/{id}/tenant/{tenantId}", reservationId, tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.status").value(Reservation.Status.CONFIRMED.name()));
    }

    @Test
    void updateReservationStatus_ShouldReturnSuccessMessage() throws Exception {
        Long reservationId = 1L;
        String newStatus = Reservation.Status.CANCELLED.name();

        when(reservationService.updateReservationStatus(reservationId, Reservation.Status.CANCELLED))
                .thenReturn("Status updated successfully");

        mockMvc.perform(patch("/api/rent/status/{id}", reservationId)
                        .param("status", newStatus))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Status updated successfully"));
    }

    @Test
    void deleteReservation_ShouldReturnSuccessMessage() throws Exception {
        Long reservationId = 1L;
        Long tenantId = 10L;

        when(reservationService.deleteReservation(reservationId, tenantId))
                .thenReturn("Reservation deleted successfully");

        mockMvc.perform(delete("/api/rent/delete/{id}/tenant/{tenantId}", reservationId, tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservation deleted successfully"));
    }

    @Test
    void getReservationByIdTenant_ShouldReturnNotFound_whenServiceThrowsException() throws Exception {
        Long reservationId = 1L;
        Long tenantId = 10L;

        when(reservationService.getReservationByIdForTenant(reservationId, tenantId))
                .thenThrow(new NoReservationException());

        mockMvc.perform(get("/api/rent/reservations/{id}/tenant/{tenantId}", reservationId, tenantId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReservation_ShouldReturnSuccessMessage() throws Exception {
        Long reservationId = 1L;
        Long tenantId = 10L;
        UpdateReservationRequest request = new UpdateReservationRequest(
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(5)
        );

        when(reservationService.updateReservation(reservationId, tenantId, request))
                .thenReturn("Reservation updated successfully");

        mockMvc.perform(patch("/api/rent/update/{id}/tenant/{tenantId}", reservationId, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "%s",
                                  "endDate": "%s"
                                }
                                """.formatted(
                                request.getStartDate(),
                                request.getEndDate()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservation updated successfully"));
    }

    @Test
    void getAllReservationsForOwner_ShouldReturnReservationList() throws Exception {
        Long propertyId = 100L;
        Long ownerId = 200L;

        List<ReservationResponse> responses = List.of(
                new ReservationResponse(1L, 200L, 100L, "Name", "Surname", LocalDate.now(), LocalDate.now().plusDays(2), Reservation.Status.CONFIRMED.toString(), Reservation.Payment.ONE_TIME.name(),
                        BigDecimal.valueOf(1000)),
                new ReservationResponse(2L, 201L, 100L, "Name", "Surname",  LocalDate.now().minusDays(5), LocalDate.now().minusDays(2), Reservation.Status.CANCELLED.toString(), Reservation.Payment.ONE_TIME.name(),
                        BigDecimal.valueOf(1000))
        );

        when(reservationService.getAllReservationsForOwner(propertyId, ownerId)).thenReturn(responses);

        mockMvc.perform(get("/api/rent/reservations/owner/all")
                        .param("propertyId", propertyId.toString())
                        .param("ownerId", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].status").value(Reservation.Status.CANCELLED.name()));
    }

    @Test
    void getReservationByIdOwner_ShouldReturnReservationResponse() throws Exception {
        Long reservationId = 1L;
        Long ownerId = 200L;

        ReservationResponse response = new ReservationResponse(
                reservationId,
                123L,
                ownerId,
                "Name",
                "Surname",
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                Reservation.Status.CONFIRMED.toString(),
                Reservation.Payment.ONE_TIME.name(),
                BigDecimal.valueOf(1000)
        );

        when(reservationService.getReservationByIdForOwner(reservationId, ownerId)).thenReturn(response);

        mockMvc.perform(get("/api/rent/reservations/{id}/owner/{ownerId}", reservationId, ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId))
                .andExpect(jsonPath("$.status").value(Reservation.Status.CONFIRMED.name()));
    }

    @Test
    void getReservationByIdOwner_ShouldReturnNotFoundWhenMissing() throws Exception {
        Long reservationId = 99L;
        Long ownerId = 200L;

        when(reservationService.getReservationByIdForOwner(reservationId, ownerId))
                .thenThrow(new NoReservationException());

        mockMvc.perform(get("/api/rent/reservations/{id}/owner/{ownerId}", reservationId, ownerId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOpinion_ShouldReturnOpinion() throws Exception {
        Long reservationId = 123L;
        Long userId = 10L;
        OpinionRequest request = new OpinionRequest(
                "Świetna rezerwacja, wszystko super!",
                5 // rating
                 // comment
        );

        User user = new User();
        User creator = new User();

        Opinion opinion = new Opinion(
                1L,
                request.getContent(),
                request.getRating(),
                user,
                creator,
                LocalDateTime.now()
        );

        when(reservationService.createOpinion(reservationId, userId, request))
                .thenReturn(opinion);

        mockMvc.perform(post("/api/rent/opinion/{reservationId}", reservationId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": %d,
                                  "content": "%s"
                                }
                                """.formatted(request.getRating(), request.getContent())))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(request.getRating()))
                .andExpect(jsonPath("$.content").value(request.getContent()));
    }

    @Test
    void getAllOpinionsByUser_ShouldReturnOpinions() throws Exception {
        Long userId = 10L;

        List<OpinionResponse> responses = List.of(
                new OpinionResponse(1L, 5, "Wspaniała!", "Name", "Surname"),
                new OpinionResponse(2L, 4, "Dobrze, polecam.", "Name", "Surname")
        );

        when(reservationService.getAllOpinionsByUser(userId)).thenReturn(responses);

        mockMvc.perform(get("/api/rent/opinion/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].rating").value(4));
    }

    @Test
    void deleteOpinion_ShouldReturnSuccessMessage() throws Exception {
        Long opinionId = 5L;
        Long userId = 10L;

        when(reservationService.deleteOpinion(userId, opinionId))
                .thenReturn("Opinion successfully deleted");

        mockMvc.perform(delete("/api/rent/opinion/{opinionId}", opinionId)
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Opinion successfully deleted"));
    }

    @Test
    void deleteOpinion_ShouldReturnErrorWhenNotFound() throws Exception {
        Long opinionId = 999L;
        Long userId = 10L;

        when(reservationService.deleteOpinion(userId, opinionId))
                .thenThrow(new NoOpinionException());

        mockMvc.perform(delete("/api/rent/opinion/{opinionId}", opinionId)
                        .param("userId", userId.toString()))
                .andExpect(status().isNotFound());
    }

}

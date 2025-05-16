package zzpj_rent.reservation;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import zzpj_rent.reservation.controllers.ReservationController;
import zzpj_rent.reservation.dtos.request.ReservationRequest;
import zzpj_rent.reservation.exceptions.BadRequestException;
import zzpj_rent.reservation.exceptions.ForbiddenException;
import zzpj_rent.reservation.exceptions.NotFoundException;
import zzpj_rent.reservation.model.Property;
import zzpj_rent.reservation.model.Reservation;
import zzpj_rent.reservation.model.User;
import zzpj_rent.reservation.services.ReservationService;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

//@WebMvcTest(ReservationController.class)
//@AutoConfigureMockMvc
//class ReservationControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private ReservationService reservationService;
//
//    @TestConfiguration
//    static class TestConfig {
//        @Bean
//        public ReservationService reservationService() {
//            return mock(ReservationService.class);
//        }
//    }
//
//    private ReservationRequest validRequest() {
//        ReservationRequest request = new ReservationRequest();
//        request.setPropertyId(1L);
//        request.setTenantId(2L);
//        request.setStartDate(LocalDate.of(2025, 6, 1));
//        request.setEndDate(LocalDate.of(2025, 6, 5));
//        return request;
//    }
//
//    @Test
//    void shouldCreateReservation() throws Exception {
//        // given
//        ReservationRequest request = new ReservationRequest();
//        request.setPropertyId(1L);
//        request.setTenantId(2L);
//        request.setStartDate(LocalDate.of(2025, 6, 1));
//        request.setEndDate(LocalDate.of(2025, 6, 5));
//
//        User owner = User.builder().id(1L).fullName("Owner User").build();
//        Property property = Property.builder().id(1L).address("Test 1").city("City").owner(owner).build();
//        User tenant = User.builder().id(2L).fullName("Jan Kowalski").build();
//
//        Reservation reservation = Reservation.builder()
//                .id(100L)
//                .property(property)
//                .tenant(tenant)
//                .startDate(request.getStartDate())
//                .endDate(request.getEndDate())
//                .status(Reservation.Status.PENDING)
//                .createdAt(LocalDateTime.of(2025, 5, 16, 12, 0))
//                .build();
//
//        given(reservationService.createReservation(any())).willReturn(reservation);
//
//        // when + then
//        mockMvc.perform(post("/api/rent/create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(100))
//                .andExpect(jsonPath("$.property.id").value(1))
//                .andExpect(jsonPath("$.tenant.id").value(2))
//                .andExpect(jsonPath("$.status").value("PENDING"));
//    }
//
//    @Test
//    void shouldReturn404WhenPropertyNotFound() throws Exception {
//        given(reservationService.createReservation(any()))
//                .willThrow(new NotFoundException("Property not found"));
//
//        mockMvc.perform(post("/api/rent/create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(validRequest())))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void shouldReturn404WhenTenantNotFound() throws Exception {
//        given(reservationService.createReservation(any()))
//                .willThrow(new NotFoundException("Tenant not found"));
//
//        mockMvc.perform(post("/api/rent/create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(validRequest())))
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void shouldReturn400WhenPropertyUnavailable() throws Exception {
//        given(reservationService.createReservation(any()))
//                .willThrow(new BadRequestException("Property is not available for the selected dates"));
//
//        mockMvc.perform(post("/api/rent/create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(validRequest())))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void shouldReturn403WhenOwnerTriesToReserve() throws Exception {
//        given(reservationService.createReservation(any()))
//                .willThrow(new ForbiddenException("Owner cannot reserve their own property"));
//
//        mockMvc.perform(post("/api/rent/create")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(validRequest())))
//                .andExpect(status().isForbidden());
//    }
//}

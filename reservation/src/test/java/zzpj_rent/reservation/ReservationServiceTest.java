package zzpj_rent.reservation;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import zzpj_rent.reservation.dtos.request.*;
import zzpj_rent.reservation.dtos.response.OpinionResponse;
import zzpj_rent.reservation.dtos.response.ReservationResponse;
import zzpj_rent.reservation.exceptions.*;
import zzpj_rent.reservation.microservices.ApartmentClient;
import zzpj_rent.reservation.microservices.UserClient;
import zzpj_rent.reservation.model.Opinion;
import zzpj_rent.reservation.model.Property;
import zzpj_rent.reservation.model.Reservation;
import zzpj_rent.reservation.model.User;
import zzpj_rent.reservation.repository.OpinionRepository;
import zzpj_rent.reservation.repository.ReservationRepository;
import zzpj_rent.reservation.services.ReservationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private ReservationRepository reservationRepository;
    private ReservationService reservationService;
    private ApartmentClient apartmentClient;
    private UserClient userClient;
    private OpinionRepository opinionRepository;

    @BeforeEach
    void setup() {
        reservationRepository = mock(ReservationRepository.class);
        apartmentClient = mock(ApartmentClient.class);
        userClient = mock(UserClient.class);
        opinionRepository = mock(OpinionRepository.class);
        reservationService = new ReservationService(reservationRepository, opinionRepository, apartmentClient, userClient);
    }

    @Test
    void shouldCreateReservationWhenDataIsValid() {
        // given
        ReservationRequest request = new ReservationRequest();
        request.setPropertyId(1L);
        request.setTenantId(2L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(2));
        ApartmentDTO property = new ApartmentDTO();
        property.setId(1L);
        property.setOwnerId(3L);
        property.setRentalType("DAILY");
        property.setPrice(BigDecimal.valueOf(100));

        UserDTO tenantDto = new UserDTO();
        tenantDto.setId(2L);

        when(apartmentClient.getApartmentById(1L)).thenReturn((property));
        when(userClient.getUserById(2L)).thenReturn((tenantDto));
        when(reservationRepository.findByPropertyIdAndDateRangeOverlap(anyLong(), any(), any()))
                .thenReturn(List.of()); // empty list -> available
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0)); // return the same entity

        // when
        Reservation result = reservationService.createReservation(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getProperty()).isNotNull();
        assertThat(result.getTenant()).isNotNull();
        assertThat(result.getProperty().getId()).isEqualTo(1L);
        assertThat(result.getTenant().getId()).isEqualTo(2L);
        assertThat(result.getProperty().getOwnerId()).isEqualTo(3L);
    }

    @Test
    void shouldThrowWhenStartDateIsNull() {
        ReservationRequest request = new ReservationRequest();
        request.setEndDate(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("Sart date and end date are required");
    }

    @Test
    void shouldThrowWhenStartDateIsAfterEndDate() {
        ReservationRequest request = new ReservationRequest();
        request.setStartDate(LocalDate.now().plusDays(2));
        request.setEndDate(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }

    @Test
    void shouldThrowWhenStartDateIsInThePast() {
        ReservationRequest request = new ReservationRequest();
        request.setStartDate(LocalDate.now().minusDays(1));
        request.setEndDate(LocalDate.now().plusDays(2));
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("Start date or end date cannot be in the past");
    }

    @Test
    void shouldThrowWhenStartDateEqualsEndDate() {
        ReservationRequest request = new ReservationRequest();
        LocalDate date = LocalDate.now().plusDays(2);
        request.setStartDate(date);
        request.setEndDate(date);
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("Start date and end date cannot be the same");
    }

    @Test
    void shouldThrowWhenPropertyNotFound() {
        ReservationRequest request = new ReservationRequest();
        request.setPropertyId(1L);
        request.setTenantId(2L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(2));
        when(apartmentClient.getApartmentById(anyLong())).thenThrow(FeignException.NotFound.class);
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(NoPropertyException.class);
    }

    @Test
    void shouldThrowWhenTenantNotFound() {
        ReservationRequest request = new ReservationRequest();
        request.setPropertyId(1L);
        request.setTenantId(2L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(2));
        when(apartmentClient.getApartmentById(1L)).thenReturn((new ApartmentDTO()));
        when(userClient.getUserById(2L)).thenThrow(FeignException.NotFound.class);
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(NoTenantException.class);
    }

    @Test
    void shouldThrowWhenNotAvailable() {
        ReservationRequest request = new ReservationRequest();
        request.setPropertyId(1L);
        request.setTenantId(2L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(2));

        ApartmentDTO property = new ApartmentDTO();
        property.setId(1L); // <--- ustaw ID
        property.setOwnerId(1L);

        UserDTO tenant = new UserDTO();
        tenant.setId(2L); // opcjonalnie też ustaw ID tenantowi, jeśli gdzieś jest używane

        when(apartmentClient.getApartmentById(1L)).thenReturn(property);
        when(userClient.getUserById(2L)).thenReturn(tenant);
        when(reservationRepository.findByPropertyIdAndDateRangeOverlap(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(new Reservation()));

        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("Property is not available for the selected dates");
    }

    @Test
    void shouldThrowWhenTenantIsOwner() {
        ReservationRequest request = new ReservationRequest();
        request.setPropertyId(1L);
        request.setTenantId(2L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(2));
 // same as tenant id
        ApartmentDTO property = new ApartmentDTO();
        property.setId(2L);
        property.setOwnerId(2L);
        UserDTO tenant = new UserDTO();
        tenant.setId(2L);

        when(apartmentClient.getApartmentById(1L)).thenReturn(property);
        when(userClient.getUserById(2L)).thenReturn(tenant);
        when(reservationRepository.findByPropertyIdAndDateRangeOverlap(anyLong(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(OwnerException.class)
                .hasMessageContaining("Owner cannot reserve their own property");
    }

    @Test
    void shouldThrowNotSpecifiedWhenDataAccessExceptionOccurs() {
        ReservationRequest request = new ReservationRequest();
        request.setPropertyId(1L);
        request.setTenantId(2L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(2));
        when(apartmentClient.getApartmentById(anyLong())).thenThrow(new org.springframework.dao.DataAccessException("db error") {});
        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(NotSpecifiedException.class)
                .hasMessageContaining("An error occurred while creating the reservation");
    }

    @Test
    void getAllReservationsForOwner_ShouldReturnReservations() {
        // given
        Long propertyId = 1L;
        Long ownerId = 99L;

        User owner = new User();
        owner.setId(ownerId);

        ApartmentDTO property = new ApartmentDTO();
        property.setId(propertyId);
        property.setOwnerId(99L);

        Property property1 = new Property(property.getId(), BigDecimal.valueOf(1000.0), "RENTAL_TYPE", ownerId);

        User tenant = new User();
        tenant.setId(10L);
        tenant.setFirstName("Jan Kowalski");

        Reservation reservation = new Reservation();
        reservation.setId(123L);
        reservation.setProperty(property1);
        reservation.setTenant(tenant);
        reservation.setStatus(Reservation.Status.CONFIRMED); // załóżmy, że takie istnieje
        reservation.setStartDate(LocalDate.now());
        reservation.setEndDate(LocalDate.now().plusDays(7));
        reservation.setPayment(Reservation.Payment.ONE_TIME);
        reservation.setPrice(BigDecimal.valueOf(1000.0));

        given(apartmentClient.getApartmentById(propertyId)).willReturn(property);
        given(reservationRepository.findByPropertyId(propertyId)).willReturn(List.of(reservation));

        // when
        List<ReservationResponse> result = reservationService.getAllReservationsForOwner(propertyId, ownerId);

        // then
        assertThat(result).hasSize(1);
        ReservationResponse response = result.getFirst();
        assertThat(response.getId()).isEqualTo(123L);
        assertThat(response.getTenantId()).isEqualTo(10L);
        assertThat(response.getTenantName()).isEqualTo("Jan Kowalski");
        assertThat(response.getPropertyId()).isEqualTo(propertyId);
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(response.getEndDate()).isEqualTo(LocalDate.now().plusDays(7));
    }

    @Test
    void getAllReservationsForOwner_ShouldThrowOwnerException_WhenUserIsNotOwner() {
        Long propertyId = 1L;
        Long ownerId = 99L;

        User actualOwner = new User();
        actualOwner.setId(50L); // inny właściciel
        ApartmentDTO property = new ApartmentDTO();
        property.setId(propertyId);
        property.setOwnerId(actualOwner.getId());

        given(apartmentClient.getApartmentById(propertyId)).willReturn(property);

        assertThatThrownBy(() -> reservationService.getAllReservationsForOwner(propertyId, ownerId))
                .isInstanceOf(OwnerException.class)
                .hasMessageContaining("You are not the owner of this property");
    }

    @Test
    void getAllReservationsForOwner_ShouldThrowNoPropertyException_WhenPropertyNotFound() {
        Long propertyId = 1L;
        Long ownerId = 99L;

        when(apartmentClient.getApartmentById(propertyId)).thenThrow(FeignException.NotFound.class);

        assertThatThrownBy(() -> reservationService.getAllReservationsForOwner(propertyId, ownerId))
                .isInstanceOf(NoPropertyException.class);
    }

    @Test
    void getReservationByIdForOwner_ShouldReturnReservation() {
        // given
        Long reservationId = 1L;
        Long ownerId = 99L;

        User owner = new User();
        owner.setId(ownerId);

        User tenant = new User();
        tenant.setId(10L);
        tenant.setFirstName("John Doe");

        ApartmentDTO property = new ApartmentDTO();
        property.setId(200L);
        property.setOwnerId(ownerId);

        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setProperty(new Property(property.getId(),  BigDecimal.valueOf(1000.0), "RENTAL_TYPE", ownerId));
        reservation.setTenant(tenant);
        reservation.setStatus(Reservation.Status.CONFIRMED);
        reservation.setStartDate(LocalDate.of(2025, 8, 1));
        reservation.setEndDate(LocalDate.of(2025, 8, 5));
        reservation.setPayment(Reservation.Payment.ONE_TIME);
        reservation.setPrice(BigDecimal.valueOf(1000.0));

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(apartmentClient.getApartmentById(property.getId())).willReturn(property);

        // when
        ReservationResponse response = reservationService.getReservationByIdForOwner(reservationId, ownerId);

        // then
        assertThat(response.getId()).isEqualTo(reservationId);
        assertThat(response.getTenantId()).isEqualTo(10L);
        assertThat(response.getTenantName()).isEqualTo("John Doe");
        assertThat(response.getPropertyId()).isEqualTo(200L);
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2025, 8, 1));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2025, 8, 5));
    }

    @Test
    void getReservationByIdForOwner_ShouldThrowNoPropertyException_WhenReservationNotFound() {
        Long reservationId = 1L;
        Long ownerId = 99L;

        given(reservationRepository.findById(reservationId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.getReservationByIdForOwner(reservationId, ownerId))
                .isInstanceOf(NoPropertyException.class);
    }

    @Test
    void getReservationByIdForOwner_ShouldThrowNoPropertyException_WhenPropertyNotFound() {
        Long reservationId = 1L;
        Long ownerId = 99L;

        Property property = new Property();
        property.setId(123L);

        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setProperty(property);

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        when(apartmentClient.getApartmentById(property.getId())).thenThrow(FeignException.NotFound.class);

        assertThatThrownBy(() -> reservationService.getReservationByIdForOwner(reservationId, ownerId))
                .isInstanceOf(NoPropertyException.class);
    }

    @Test
    void getReservationByIdForOwner_ShouldThrowOwnerException_WhenUserIsNotOwner() {
        Long reservationId = 1L;
        Long ownerId = 99L;

        User propertyOwner = new User();
        propertyOwner.setId(50L); // different owner

        ApartmentDTO property = new ApartmentDTO();
        property.setId(200L);
        property.setOwnerId(propertyOwner.getId());

        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setProperty(new Property(property.getId(),  BigDecimal.valueOf(1000.0), "RENTAL_TYPE", propertyOwner.getId()));

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(apartmentClient.getApartmentById(property.getId())).willReturn(property);

        assertThatThrownBy(() -> reservationService.getReservationByIdForOwner(reservationId, ownerId))
                .isInstanceOf(OwnerException.class)
                .hasMessageContaining("You are not the owner of this property");
    }

    @Test
    void getReservationsForTenantByStatus_ShouldReturnListOfResponses() {
        // given
        Long tenantId = 10L;

        User tenant = new User();
        tenant.setId(tenantId);
        tenant.setFirstName("Alice Smith");

        Property property = new Property();
        property.setId(300L);

        Reservation reservation1 = new Reservation();
        reservation1.setId(1L);
        reservation1.setTenant(tenant);
        reservation1.setProperty(property);
        reservation1.setStatus(Reservation.Status.PENDING);
        reservation1.setStartDate(LocalDate.now());
        reservation1.setEndDate(LocalDate.now().plusDays(1));
        reservation1.setPayment(Reservation.Payment.ONE_TIME);
        reservation1.setPrice(BigDecimal.valueOf(1000.0));

        Reservation reservation2 = new Reservation();
        reservation2.setId(2L);
        reservation2.setTenant(tenant);
        reservation2.setProperty(property);
        reservation2.setStatus(Reservation.Status.PENDING);
        reservation2.setStartDate(LocalDate.now().plusDays(10));
        reservation2.setEndDate(LocalDate.now().plusDays(12));
        reservation2.setPayment(Reservation.Payment.ONE_TIME);
        reservation2.setPrice(BigDecimal.valueOf(1000.0));

        given(reservationRepository.findByStatusAndTenantId(Reservation.Status.PENDING, tenantId))
                .willReturn(List.of(reservation1, reservation2));

        // when
        List<ReservationResponse> responses = reservationService.getReservationsForTenantByStatus(tenantId, Reservation.Status.PENDING);

        // then
        assertThat(responses).hasSize(2);

        ReservationResponse response1 = responses.getFirst();
        assertThat(response1.getId()).isEqualTo(1L);
        assertThat(response1.getTenantId()).isEqualTo(10L);
        assertThat(response1.getTenantName()).isEqualTo("Alice Smith");
        assertThat(response1.getPropertyId()).isEqualTo(300L);
        assertThat(response1.getStatus()).isEqualTo("PENDING");
        assertThat(response1.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(response1.getEndDate()).isEqualTo(LocalDate.now().plusDays(1));

        ReservationResponse response2 = responses.get(1);
        assertThat(response2.getId()).isEqualTo(2L);
        assertThat(response2.getStartDate()).isEqualTo(LocalDate.now().plusDays(10));
        assertThat(response2.getEndDate()).isEqualTo(LocalDate.now().plusDays(12));
    }

    @Test
    void updateReservationStatus_ShouldConfirmPendingReservation() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(Reservation.Status.PENDING);

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));
        given(reservationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        String result = reservationService.updateReservationStatus(1L, Reservation.Status.CONFIRMED);

        assertThat(result).isEqualTo("Reservation status updated to CONFIRMED");
        assertThat(reservation.getStatus()).isEqualTo(Reservation.Status.CONFIRMED);
    }

    @Test
    void updateReservationStatus_ShouldThrowWhenConfirmingNonPending() {
        Reservation reservation = new Reservation();
        reservation.setStatus(Reservation.Status.CONFIRMED);

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.updateReservationStatus(1L, Reservation.Status.CONFIRMED))
                .isInstanceOf(ReservationStatusException.class)
                .hasMessageContaining("Reservation can only be accepted if it is pending");
    }

    @Test
    void updateReservationStatus_ShouldRejectPendingReservation() {
        Reservation reservation = new Reservation();
        reservation.setStatus(Reservation.Status.PENDING);

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));
        given(reservationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        String result = reservationService.updateReservationStatus(1L, Reservation.Status.REJECTED);

        assertThat(result).isEqualTo("Reservation status updated to REJECTED");
        assertThat(reservation.getStatus()).isEqualTo(Reservation.Status.REJECTED);
    }

    @Test
    void updateReservationStatus_ShouldThrowWhenRejectingNonPending() {
        Reservation reservation = new Reservation();
        reservation.setStatus(Reservation.Status.CONFIRMED);

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.updateReservationStatus(1L, Reservation.Status.REJECTED))
                .isInstanceOf(ReservationStatusException.class)
                .hasMessageContaining("Reservation can only be rejected if it is pending");
    }

    @Test
    void updateReservationStatus_ShouldFinishConfirmedReservation() {
        Reservation reservation = new Reservation();
        reservation.setStatus(Reservation.Status.CONFIRMED);

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));
        given(reservationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        String result = reservationService.updateReservationStatus(1L, Reservation.Status.FINISHED);

        assertThat(result).isEqualTo("Reservation status updated to FINISHED");
        assertThat(reservation.getStatus()).isEqualTo(Reservation.Status.FINISHED);
    }

    @Test
    void updateReservationStatus_ShouldThrowWhenFinishingNonConfirmed() {
        Reservation reservation = new Reservation();
        reservation.setStatus(Reservation.Status.PENDING);

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.updateReservationStatus(1L, Reservation.Status.FINISHED))
                .isInstanceOf(ReservationStatusException.class)
                .hasMessageContaining("Reservation can only be finished if it is accepted");
    }

    @Test
    void updateReservationStatus_ShouldCancelConfirmedReservation() {
        Reservation reservation = new Reservation();
        reservation.setStatus(Reservation.Status.CONFIRMED);

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));
        given(reservationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        String result = reservationService.updateReservationStatus(1L, Reservation.Status.CANCELLED);

        assertThat(result).isEqualTo("Reservation status updated to CANCELLED");
        assertThat(reservation.getStatus()).isEqualTo(Reservation.Status.CANCELLED);
    }

    @Test
    void updateReservationStatus_ShouldThrowWhenCancellingNonConfirmed() {
        Reservation reservation = new Reservation();
        reservation.setStatus(Reservation.Status.PENDING);

        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.updateReservationStatus(1L, Reservation.Status.CANCELLED))
                .isInstanceOf(ReservationStatusException.class)
                .hasMessageContaining("Reservation can only be cancelled if it is accepted");
    }

    @Test
    void updateReservationStatus_ShouldThrowWhenReservationNotFound() {
        given(reservationRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.updateReservationStatus(1L, Reservation.Status.CONFIRMED))
                .isInstanceOf(NoReservationException.class);
    }

    @Test
    void deleteReservation_ShouldDeletePendingReservation() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(Reservation.Status.PENDING);

        given(reservationRepository.findByIdAndTenantId(1L, 100L))
                .willReturn(Optional.of(reservation));
        doNothing().when(reservationRepository).delete(reservation);

        String result = reservationService.deleteReservation(1L, 100L);

        assertThat(result).isEqualTo("Reservation deleted successfully");
        then(reservationRepository).should().delete(reservation);
    }

    @Test
    void deleteReservation_ShouldThrowIfNotPending() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(Reservation.Status.CONFIRMED);

        given(reservationRepository.findByIdAndTenantId(1L, 100L))
                .willReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.deleteReservation(1L, 100L))
                .isInstanceOf(ReservationStatusException.class)
                .hasMessageContaining("Cannot delete a processed reservation");

        then(reservationRepository).should(never()).delete(any());
    }

    @Test
    void deleteReservation_ShouldThrowIfNotFound() {
        given(reservationRepository.findByIdAndTenantId(1L, 100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.deleteReservation(1L, 100L))
                .isInstanceOf(NoReservationException.class);
    }

    @Test
    void updateReservation_ShouldUpdateWhenValidPendingAndAvailable() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);

        Property property = new Property();
        property.setId(10L);
        property.setPrice(BigDecimal.valueOf(1660));

        reservation.setProperty(property);
        reservation.setStatus(Reservation.Status.PENDING);
        reservation.setStartDate(LocalDate.now().plusDays(1));
        reservation.setEndDate(LocalDate.now().plusDays(2));
        reservation.getProperty().setId(10L);
        reservation.setPayment(Reservation.Payment.ONE_TIME);
        reservation.setPrice(BigDecimal.valueOf(1660));

        UpdateReservationRequest request = new UpdateReservationRequest();
        request.setStartDate(LocalDate.now().plusDays(3));
        request.setEndDate(LocalDate.now().plusDays(4));

        given(reservationRepository.findByIdAndTenantId(1L, 100L)).willReturn(Optional.of(reservation));
        given(reservationRepository.findByPropertyIdAndDateRangeOverlap(
                10L, request.getStartDate(), request.getEndDate())).willReturn(Collections.emptyList());

        String result = reservationService.updateReservation(1L, 100L, request);

        assertThat(result).isEqualTo("Reservation updated successfully");
        then(reservationRepository).should().save(reservation);
        assertThat(reservation.getStartDate()).isEqualTo(request.getStartDate());
        assertThat(reservation.getEndDate()).isEqualTo(request.getEndDate());
    }

    @Test
    void updateReservation_ShouldThrowWhenStatusNotPending() {
        Reservation reservation = new Reservation();
        reservation.setStatus(Reservation.Status.CONFIRMED);

        given(reservationRepository.findByIdAndTenantId(1L, 100L)).willReturn(Optional.of(reservation));
        UpdateReservationRequest request = new UpdateReservationRequest();

        assertThatThrownBy(() -> reservationService.updateReservation(1L, 100L, request))
                .isInstanceOf(ReservationStatusException.class)
                .hasMessageContaining("Cannot update a processed reservation");
    }

    @Test
    void updateReservation_ShouldThrowWhenNoReservationFound() {
        given(reservationRepository.findByIdAndTenantId(1L, 100L)).willReturn(Optional.empty());

        UpdateReservationRequest request = new UpdateReservationRequest();

        assertThatThrownBy(() -> reservationService.updateReservation(1L, 100L, request))
                .isInstanceOf(NoReservationException.class);
    }

    @Test
    void updateReservation_ShouldThrowWhenDateRangeOverlap() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setProperty(new Property());
        reservation.getProperty().setId(10L);
        reservation.setStatus(Reservation.Status.PENDING);

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(2);

        UpdateReservationRequest request = new UpdateReservationRequest();
        request.setStartDate(start);
        request.setEndDate(end);

        given(reservationRepository.findByIdAndTenantId(1L, 100L)).willReturn(Optional.of(reservation));
        given(reservationRepository.findByPropertyIdAndDateRangeOverlap(
                10L, start, end)).willReturn(List.of(new Reservation()));

        assertThatThrownBy(() -> reservationService.updateReservation(1L, 100L, request))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("Property is not available for the selected dates");
    }

    @Test
    void updateReservation_ShouldThrowWhenStartAfterEnd() {
        Reservation reservation = new Reservation();
        reservation.setProperty(new Property());
        reservation.getProperty().setId(10L);
        reservation.setStatus(Reservation.Status.PENDING);

        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now().plusDays(4); // End before start

        UpdateReservationRequest request = new UpdateReservationRequest();
        request.setStartDate(start);
        request.setEndDate(end);

        given(reservationRepository.findByIdAndTenantId(1L, 100L)).willReturn(Optional.of(reservation));
        given(reservationRepository.findByPropertyIdAndDateRangeOverlap(
                10L, start, end)).willReturn(Collections.emptyList());

        assertThatThrownBy(() -> reservationService.updateReservation(1L, 100L, request))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }

    @Test
    void updateReservation_ShouldThrowWhenDatesInPast() {
        Reservation reservation = new Reservation();
        reservation.setProperty(new Property());
        reservation.getProperty().setId(10L);
        reservation.setStatus(Reservation.Status.PENDING);

        LocalDate pastDate = LocalDate.now().minusDays(1);

        UpdateReservationRequest request = new UpdateReservationRequest();
        request.setStartDate(pastDate);
        request.setEndDate(pastDate.plusDays(2));

        given(reservationRepository.findByIdAndTenantId(1L, 100L)).willReturn(Optional.of(reservation));
        given(reservationRepository.findByPropertyIdAndDateRangeOverlap(
                10L, request.getStartDate(), request.getEndDate())).willReturn(Collections.emptyList());

        assertThatThrownBy(() -> reservationService.updateReservation(1L, 100L, request))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("Start date or end date cannot be in the past");
    }

    @Test
    void updateReservation_ShouldThrowWhenStartEqualsEnd() {
        Reservation reservation = new Reservation();
        reservation.setProperty(new Property());
        reservation.getProperty().setId(10L);
        reservation.setStatus(Reservation.Status.PENDING);

        LocalDate sameDay = LocalDate.now().plusDays(1);

        UpdateReservationRequest request = new UpdateReservationRequest();
        request.setStartDate(sameDay);
        request.setEndDate(sameDay);

        given(reservationRepository.findByIdAndTenantId(1L, 100L)).willReturn(Optional.of(reservation));
        given(reservationRepository.findByPropertyIdAndDateRangeOverlap(
                10L, sameDay, sameDay)).willReturn(Collections.emptyList());

        assertThatThrownBy(() -> reservationService.updateReservation(1L, 100L, request))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("cannot be the same");
    }

    @Test
    void shouldReturnEmptyListWhenNoReservationsFound() {
        // given
        Long tenantId = 123L;
        when(reservationRepository.findByTenantId(tenantId)).thenReturn(List.of());

        // when
        List<ReservationResponse> result = reservationService.getAllReservationsForTenant(tenantId);

        // then
        assertThat(result).isEmpty();
        verify(reservationRepository).findByTenantId(tenantId);
    }

    @Test
    void shouldMapReservationsToReservationResponses() {
        // given
        Long tenantId = 123L;

        User tenant = new User();
        tenant.setId(123L);
        tenant.setFirstName("Jan");
        tenant.setLastName("Kowalski");

        Property property = new Property();
        property.setId(456L);

        Reservation reservation = new Reservation();
        reservation.setId(789L);
        reservation.setTenant(tenant);
        reservation.setProperty(property);
        reservation.setStatus(Reservation.Status.CONFIRMED);
        reservation.setStartDate(LocalDate.of(2025, 1, 10));
        reservation.setEndDate(LocalDate.of(2025, 1, 15));
        reservation.setPayment(Reservation.Payment.ONE_TIME);
        reservation.setPrice(BigDecimal.valueOf(1000.0));

        when(reservationRepository.findByTenantId(tenantId)).thenReturn(List.of(reservation));

        // when
        List<ReservationResponse> result = reservationService.getAllReservationsForTenant(tenantId);

        // then
        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(res -> {
                    assertThat(res.getId()).isEqualTo(789L);
                    assertThat(res.getTenantId()).isEqualTo(123L);
                    assertThat(res.getTenantName()).isEqualTo("Jan");
                    assertThat(res.getTenantSurname()).isEqualTo("Kowalski");
                    assertThat(res.getPropertyId()).isEqualTo(456L);
                    assertThat(res.getStatus()).isEqualTo("CONFIRMED");
                    assertThat(res.getStartDate()).isEqualTo(LocalDate.of(2025, 1, 10));
                    assertThat(res.getEndDate()).isEqualTo(LocalDate.of(2025, 1, 15));
                });

        verify(reservationRepository).findByTenantId(tenantId);
    }

    @Test
    void shouldHandleMultipleReservations() {
        // given
        Long tenantId = 123L;

        User tenant = new User();
        tenant.setId(123L);
        tenant.setFirstName("Anna");
        tenant.setLastName("Nowak");

        Property prop1 = new Property();
        prop1.setId(1L);
        Property prop2 = new Property();
        prop2.setId(2L);

        Reservation res1 = new Reservation();
        res1.setId(1L);
        res1.setTenant(tenant);
        res1.setProperty(prop1);
        res1.setStatus(Reservation.Status.PENDING);
        res1.setStartDate(LocalDate.of(2025, 2, 1));
        res1.setEndDate(LocalDate.of(2025, 2, 5));
        res1.setPayment(Reservation.Payment.ONE_TIME);
        res1.setPrice(BigDecimal.valueOf(1000.0));

        Reservation res2 = new Reservation();
        res2.setId(2L);
        res2.setTenant(tenant);
        res2.setProperty(prop2);
        res2.setStatus(Reservation.Status.CONFIRMED);
        res2.setStartDate(LocalDate.of(2025, 3, 10));
        res2.setEndDate(LocalDate.of(2025, 3, 12));
        res2.setPayment(Reservation.Payment.ONE_TIME);
        res2.setPrice(BigDecimal.valueOf(1000.0));

        when(reservationRepository.findByTenantId(tenantId)).thenReturn(List.of(res1, res2));

        // when
        List<ReservationResponse> result = reservationService.getAllReservationsForTenant(tenantId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ReservationResponse::getPropertyId)
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(result).extracting(ReservationResponse::getStatus)
                .containsExactlyInAnyOrder("PENDING", "CONFIRMED");

        verify(reservationRepository).findByTenantId(tenantId);
    }

    @Test
    void testCreateOpinionWithValidReservationAndTenant() {
        // given
        Long reservationId = 1L;
        Long tenantId = 2L;
        Long ownerId = 99L;

        // przygotuj obiekt reservation
        User tenantUser = new User();
        tenantUser.setId(tenantId);

        Property property = new Property(
                1L,
                BigDecimal.valueOf(1000.0),
                "RENTAL_TYPE",
                ownerId
        );

        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setTenant(tenantUser);
        reservation.setProperty(property);
        reservation.setStatus(Reservation.Status.FINISHED);

        // przygotuj UserDTO dla ownera (do userClient.getUserById(99L))
        UserDTO ownerDTO = new UserDTO();
        ownerDTO.setId(ownerId);
        ownerDTO.setUsername("owner_user");
        ownerDTO.setEmail("owner@example.com");
        ownerDTO.setFirstName("OwnerFirst");
        ownerDTO.setLastName("OwnerLast");

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(userClient.getUserById(ownerId)).thenReturn(ownerDTO); // <- ważne!
        when(opinionRepository.save(any(Opinion.class)))
                .thenAnswer(inv -> inv.getArgument(0)); // zwracaj opinię

        // przygotuj request
        OpinionRequest request = new OpinionRequest();
        request.setContent("Świetny pobyt!");
        request.setRating(5);

        // when
        Opinion opinion = reservationService.createOpinion(reservationId, tenantId, request);

        // then
        assertThat(opinion).isNotNull();
        assertThat(opinion.getContent()).isEqualTo("Świetny pobyt!");
        assertThat(opinion.getRating()).isEqualTo(5);
        assertThat(opinion.getUser().getId()).isEqualTo(ownerId); // user to owner
        assertThat(opinion.getCreator().getId()).isEqualTo(tenantId);
    }

    @Test
    void shouldThrowReservationStatusExceptionWhenReservationNotFinished() {
    Reservation reservation = new Reservation();
    reservation.setStatus(Reservation.Status.PENDING);
    when(reservationRepository.findById(anyLong())).thenReturn(Optional.of(reservation));

    OpinionRequest request = new OpinionRequest();
    request.setRating(4);

    assertThatThrownBy(() -> reservationService.createOpinion(1L, 1L, request))
            .isInstanceOf(ReservationStatusException.class)
            .hasMessageContaining("Opinion can only be created for finished reservations");
}

    @Test
    void shouldThrowInvalidRatingExceptionWhenRatingOutOfBounds() {
        Reservation reservation = new Reservation();
        reservation.setStatus(Reservation.Status.FINISHED);
        when(reservationRepository.findById(anyLong())).thenReturn(Optional.of(reservation));
        OpinionRequest request = new OpinionRequest();
        request.setRating(6); // poza zakresem

        assertThatThrownBy(() -> reservationService.createOpinion(1L, 1L, request))
                .isInstanceOf(InvalidRatingException.class);
    }

    @Test
    void shouldThrowNoReservationExceptionWhenReservationNotFound() {
        when(reservationRepository.findById(anyLong())).thenReturn(Optional.empty());
        OpinionRequest request = new OpinionRequest();

        assertThatThrownBy(() -> reservationService.createOpinion(99L, 1L, request))
                .isInstanceOf(NoReservationException.class);
    }

    @Test
    void testGetProperty_ReturnsValidProperty() {
        // given
        Long id = 10L;
        ApartmentDTO apartmentDTO = new ApartmentDTO();
        apartmentDTO.setId(id);
        apartmentDTO.setPrice(BigDecimal.valueOf(1234.56));
        apartmentDTO.setRentalType("RENTAL_TYPE");
        apartmentDTO.setOwnerId(99L);

        when(apartmentClient.getApartmentById(id)).thenReturn(apartmentDTO);

        // when
        Property property = reservationService.getProperty(id);

        // then
        assertThat(property).isNotNull();
        assertThat(property.getId()).isEqualTo(id);
        assertThat(property.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(1234.56));
        assertThat(property.getRentalType()).isEqualTo("RENTAL_TYPE");
        assertThat(property.getOwnerId()).isEqualTo(99L);
    }

    @Test
    void testGetProperty_ThrowsNoPropertyException() {
        // given
        Long id = 123L;
        when(apartmentClient.getApartmentById(id)).thenThrow(FeignException.NotFound.class);

        // when + then
        assertThatThrownBy(() -> reservationService.getProperty(id))
                .isInstanceOf(NoPropertyException.class);
    }

    @Test
    void testGetTenant_ReturnsValidUser() {
        // given
        Long userId = 99L;
        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setUsername("user99");
        userDTO.setEmail("user99@example.com");
        userDTO.setFirstName("User");
        userDTO.setLastName("NinetyNine");

        when(userClient.getUserById(userId)).thenReturn(userDTO);

        // when
        User user = reservationService.getTenant(userId);

        // then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(userId);
        assertThat(user.getUsername()).isEqualTo("user99");
        assertThat(user.getEmail()).isEqualTo("user99@example.com");
        assertThat(user.getFirstName()).isEqualTo("User");
        assertThat(user.getLastName()).isEqualTo("NinetyNine");
    }

    @Test
    void testGetTenant_ThrowsNoTenantException() {
        // given
        Long userId = 123L;
        when(userClient.getUserById(userId)).thenThrow(FeignException.NotFound.class);

        // when + then
        assertThatThrownBy(() -> reservationService.getTenant(userId))
                .isInstanceOf(NoTenantException.class);
    }

    @Test
    void getAllOpinionsByUser_ShouldReturnMappedOpinionResponses() {
        // given
        User user = new User(1L, "john_doe", "john@example.com",
                "John", "Doe");
        Opinion opinion1 = Opinion.builder()
                .id(100L)
                .content("Super!")
                .rating(5)
                .user(user)
                .build();
        Opinion opinion2 = Opinion.builder()
                .id(200L)
                .content("Średnio...")
                .rating(3)
                .user(user)
                .build();

        when(opinionRepository.findAllByUser_Id(1L)).thenReturn(List.of(opinion1, opinion2));

        // when
        List<OpinionResponse> responses = reservationService.getAllOpinionsByUser(1L);

        // then
        assertThat(responses)
                .hasSize(2)
                .extracting(OpinionResponse::getId, OpinionResponse::getContent,
                        OpinionResponse::getRating, OpinionResponse::getFirstName, OpinionResponse::getLastName)
                .containsExactlyInAnyOrder(
                        tuple(100L, "Super!", 5, "John", "Doe"),
                        tuple(200L, "Średnio...", 3, "John", "Doe")
                );

        verify(opinionRepository).findAllByUser_Id(1L);
    }

    @Test
    void deleteOpinion_ShouldDeleteWhenCreatorMatchesUserId() {
        // given
        User creator = new User(1L, "john_doe", "john@example.com",
                "John", "Doe");
        Opinion opinion = Opinion.builder()
                .id(100L)
                .creator(creator)
                .build();

        when(opinionRepository.findById(100L)).thenReturn(Optional.of(opinion));
        doNothing().when(opinionRepository).delete(opinion);

        // when
        String result = reservationService.deleteOpinion(1L, 100L);

        // then
        assertThat(result).isEqualTo("Pomyślnie usunięto opinię");
        verify(opinionRepository).delete(opinion);
    }

    @Test
    void deleteOpinion_ShouldThrowOwnerExceptionWhenUserIsNotCreator() {
        // given
        User creator = new User(1L, "john_doe", "john@example.com",
                "John", "Doe");
        Opinion opinion = Opinion.builder()
                .id(100L)
                .creator(creator)
                .build();

        when(opinionRepository.findById(100L)).thenReturn(Optional.of(opinion));

        // when + then
        assertThatThrownBy(() -> reservationService.deleteOpinion(2L, 100L))
                .isInstanceOf(OwnerException.class)
                .hasMessageContaining("To nie jest twoja opinia!");

        verify(opinionRepository, never()).delete(any());
    }

    @Test
    void deleteOpinion_ShouldThrowNoOpinionExceptionWhenOpinionNotFound() {
        // given
        when(opinionRepository.findById(999L)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> reservationService.deleteOpinion(1L, 999L))
                .isInstanceOf(NoOpinionException.class);

        verify(opinionRepository, never()).delete(any());
    }
}



package zzpj_rent.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import zzpj_rent.reservation.dtos.request.ReservationRequest;
import zzpj_rent.reservation.dtos.request.UpdateReservationRequest;
import zzpj_rent.reservation.dtos.response.ReservationResponse;
import zzpj_rent.reservation.exceptions.*;
import zzpj_rent.reservation.microservices.ApartmentClient;
import zzpj_rent.reservation.microservices.UserClient;
import zzpj_rent.reservation.model.Property;
import zzpj_rent.reservation.model.Reservation;
import zzpj_rent.reservation.model.User;
import zzpj_rent.reservation.repository.PropertyRepository;
import zzpj_rent.reservation.repository.ReservationRepository;
import zzpj_rent.reservation.repository.UserRepository;
import zzpj_rent.reservation.services.ReservationService;

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
    private PropertyRepository propertyRepository;
    private UserRepository userRepository;
    private ReservationService reservationService;
    private ApartmentClient apartmentClient;
    private UserClient userClient;

    @BeforeEach
    void setup() {
        reservationRepository = mock(ReservationRepository.class);
        propertyRepository = mock(PropertyRepository.class);
        userRepository = mock(UserRepository.class);
        apartmentClient = mock(ApartmentClient.class);
        userClient = mock(UserClient.class);
        reservationService = new ReservationService(reservationRepository, propertyRepository, userRepository, apartmentClient, userClient);
    }

    @Test
    void shouldCreateReservationWhenDataIsValid() {
        // given
        ReservationRequest request = new ReservationRequest();
        request.setPropertyId(1L);
        request.setTenantId(2L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(2));
        Property property = new Property();
        property.setId(1L);
        User owner = new User();
        owner.setId(3L);
        property.setOwner(owner);
        User tenant = new User();
        tenant.setId(2L);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(userRepository.findById(2L)).thenReturn(Optional.of(tenant));
        when(reservationRepository.findByPropertyIdAndDateRangeOverlap(anyLong(), any(), any()))
                .thenReturn(List.of()); // empty list -> available
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0)); // return the same entity

        // when
        Reservation result = reservationService.createReservation(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getProperty()).isEqualTo(property);
        assertThat(result.getTenant()).isEqualTo(tenant);
        assertThat(result.getStatus()).isEqualTo(Reservation.Status.PENDING);
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
                .hasMessageContaining("Sart date cannot be after end date");
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
        when(propertyRepository.findById(anyLong())).thenReturn(Optional.empty());
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
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(new Property()));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
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

        Property property = new Property();
        property.setId(1L); // <--- ustaw ID
        property.setOwner(new User());

        User tenant = new User();
        tenant.setId(2L); // opcjonalnie też ustaw ID tenantowi, jeśli gdzieś jest używane

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(userRepository.findById(2L)).thenReturn(Optional.of(tenant));
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
        User owner = new User();
        owner.setId(2L); // same as tenant id
        Property property = new Property();
        property.setId(2L);
        property.setOwner(owner);
        User tenant = owner;

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(userRepository.findById(2L)).thenReturn(Optional.of(tenant));
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
        when(propertyRepository.findById(anyLong())).thenThrow(new org.springframework.dao.DataAccessException("db error") {});
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

        Property property = new Property();
        property.setId(propertyId);
        property.setOwner(owner);

        User tenant = new User();
        tenant.setId(10L);
        tenant.setFullName("Jan Kowalski");

        Reservation reservation = new Reservation();
        reservation.setId(123L);
        reservation.setProperty(property);
        reservation.setTenant(tenant);
        reservation.setStatus(Reservation.Status.CONFIRMED); // załóżmy, że takie istnieje
        reservation.setStartDate(LocalDate.now());
        reservation.setEndDate(LocalDate.now().plusDays(7));

        given(propertyRepository.findById(propertyId)).willReturn(Optional.of(property));
        given(reservationRepository.findByPropertyId(propertyId)).willReturn(List.of(reservation));

        // when
        List<ReservationResponse> result = reservationService.getAllReservationsForOwner(propertyId, ownerId);

        // then
        assertThat(result).hasSize(1);
        ReservationResponse response = result.get(0);
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
        Property property = new Property();
        property.setId(propertyId);
        property.setOwner(actualOwner);

        given(propertyRepository.findById(propertyId)).willReturn(Optional.of(property));

        assertThatThrownBy(() -> reservationService.getAllReservationsForOwner(propertyId, ownerId))
                .isInstanceOf(OwnerException.class)
                .hasMessageContaining("You are not the owner of this property");
    }

    @Test
    void getAllReservationsForOwner_ShouldThrowNoPropertyException_WhenPropertyNotFound() {
        Long propertyId = 1L;
        Long ownerId = 99L;

        given(propertyRepository.findById(propertyId)).willReturn(Optional.empty());

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
        tenant.setFullName("John Doe");

        Property property = new Property();
        property.setId(200L);
        property.setOwner(owner);

        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setProperty(property);
        reservation.setTenant(tenant);
        reservation.setStatus(Reservation.Status.CONFIRMED);
        reservation.setStartDate(LocalDate.of(2025, 8, 1));
        reservation.setEndDate(LocalDate.of(2025, 8, 5));

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(propertyRepository.findById(property.getId())).willReturn(Optional.of(property));

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
        given(propertyRepository.findById(property.getId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.getReservationByIdForOwner(reservationId, ownerId))
                .isInstanceOf(NoPropertyException.class);
    }

    @Test
    void getReservationByIdForOwner_ShouldThrowOwnerException_WhenUserIsNotOwner() {
        Long reservationId = 1L;
        Long ownerId = 99L;

        User propertyOwner = new User();
        propertyOwner.setId(50L); // different owner

        Property property = new Property();
        property.setId(200L);
        property.setOwner(propertyOwner);

        Reservation reservation = new Reservation();
        reservation.setId(reservationId);
        reservation.setProperty(property);

        given(reservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
        given(propertyRepository.findById(property.getId())).willReturn(Optional.of(property));

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
        tenant.setFullName("Alice Smith");

        Property property = new Property();
        property.setId(300L);

        Reservation reservation1 = new Reservation();
        reservation1.setId(1L);
        reservation1.setTenant(tenant);
        reservation1.setProperty(property);
        reservation1.setStatus(Reservation.Status.PENDING);
        reservation1.setStartDate(LocalDate.now());
        reservation1.setEndDate(LocalDate.now().plusDays(1));

        Reservation reservation2 = new Reservation();
        reservation2.setId(2L);
        reservation2.setTenant(tenant);
        reservation2.setProperty(property);
        reservation2.setStatus(Reservation.Status.PENDING);
        reservation2.setStartDate(LocalDate.now().plusDays(10));
        reservation2.setEndDate(LocalDate.now().plusDays(12));

        given(reservationRepository.findByStatusAndTenantId(Reservation.Status.PENDING, tenantId))
                .willReturn(List.of(reservation1, reservation2));

        // when
        List<ReservationResponse> responses = reservationService.getReservationsForTenantByStatus(tenantId, Reservation.Status.PENDING);

        // then
        assertThat(responses).hasSize(2);

        ReservationResponse response1 = responses.get(0);
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
        reservation.setProperty(new Property());
        reservation.setStatus(Reservation.Status.PENDING);
        reservation.setStartDate(LocalDate.now().plusDays(1));
        reservation.setEndDate(LocalDate.now().plusDays(2));
        reservation.getProperty().setId(10L);

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

}


//package zzpj_rent.reservation;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import zzpj_rent.reservation.dtos.request.ReservationRequest;
//import zzpj_rent.reservation.exceptions.*;
//import zzpj_rent.reservation.model.Property;
//import zzpj_rent.reservation.model.Reservation;
//import zzpj_rent.reservation.model.User;
//import zzpj_rent.reservation.repository.PropertyRepository;
//import zzpj_rent.reservation.repository.ReservationRepository;
//import zzpj_rent.reservation.repository.UserRepository;
//import zzpj_rent.reservation.services.ReservationService;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//@SpringBootTest
//class ReservationApplicationTests {
//
//    private ReservationRepository reservationRepository;
//    private PropertyRepository propertyRepository;
//    private UserRepository userRepository;
//    private ReservationService reservationService;
//
//    @BeforeEach
//    void setup() {
//        reservationRepository = mock(ReservationRepository.class);
//        propertyRepository = mock(PropertyRepository.class);
//        userRepository = mock(UserRepository.class);
//        reservationService = new ReservationService(reservationRepository, propertyRepository, userRepository);
//    }
//
//    @Test
//    void shouldCreateReservationWhenDataIsValid() {
//        // given
//        ReservationRequest request = new ReservationRequest();
//        request.setPropertyId(1L);
//        request.setTenantId(2L);
//        request.setStartDate(LocalDate.now().plusDays(1));
//        request.setEndDate(LocalDate.now().plusDays(2));
//        Property property = new Property();
//        property.setId(1L);
//        User owner = new User();
//        owner.setId(3L);
//        property.setOwner(owner);
//        User tenant = new User();
//        tenant.setId(2L);
//        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
//        when(userRepository.findById(2L)).thenReturn(Optional.of(tenant));
//        when(reservationRepository.findByPropertyIdAndDateRangeOverlap(anyLong(), any(), any()))
//                .thenReturn(List.of()); // empty list -> available
//        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0)); // return the same entity
//
//        // when
//        Reservation result = reservationService.createReservation(request);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.getProperty()).isEqualTo(property);
//        assertThat(result.getTenant()).isEqualTo(tenant);
//        assertThat(result.getStatus()).isEqualTo(Reservation.Status.PENDING);
//    }
//
//    @Test
//    void shouldThrowWhenStartDateIsNull() {
//        ReservationRequest request = new ReservationRequest();
//        request.setEndDate(LocalDate.now().plusDays(1));
//        assertThatThrownBy(() -> reservationService.createReservation(request))
//                .isInstanceOf(InvalidDateRangeException.class)
//                .hasMessageContaining("Sart date and end date are required");
//    }
//
//    @Test
//    void shouldThrowWhenStartDateIsAfterEndDate() {
//        ReservationRequest request = new ReservationRequest();
//        request.setStartDate(LocalDate.now().plusDays(2));
//        request.setEndDate(LocalDate.now().plusDays(1));
//        assertThatThrownBy(() -> reservationService.createReservation(request))
//                .isInstanceOf(InvalidDateRangeException.class)
//                .hasMessageContaining("Sart date cannot be after end date");
//    }
//
//    @Test
//    void shouldThrowWhenStartDateIsInThePast() {
//        ReservationRequest request = new ReservationRequest();
//        request.setStartDate(LocalDate.now().minusDays(1));
//        request.setEndDate(LocalDate.now().plusDays(2));
//        assertThatThrownBy(() -> reservationService.createReservation(request))
//                .isInstanceOf(InvalidDateRangeException.class)
//                .hasMessageContaining("Start date or end date cannot be in the past");
//    }
//
//    @Test
//    void shouldThrowWhenStartDateEqualsEndDate() {
//        ReservationRequest request = new ReservationRequest();
//        LocalDate date = LocalDate.now().plusDays(2);
//        request.setStartDate(date);
//        request.setEndDate(date);
//        assertThatThrownBy(() -> reservationService.createReservation(request))
//                .isInstanceOf(InvalidDateRangeException.class)
//                .hasMessageContaining("Start date and end date cannot be the same");
//    }
//
//    @Test
//    void shouldThrowWhenPropertyNotFound() {
//        ReservationRequest request = new ReservationRequest();
//        request.setPropertyId(1L);
//        request.setTenantId(2L);
//        request.setStartDate(LocalDate.now().plusDays(1));
//        request.setEndDate(LocalDate.now().plusDays(2));
//        when(propertyRepository.findById(anyLong())).thenReturn(Optional.empty());
//        assertThatThrownBy(() -> reservationService.createReservation(request))
//                .isInstanceOf(NoPropertyException.class);
//    }
//
//    @Test
//    void shouldThrowWhenTenantNotFound() {
//        ReservationRequest request = new ReservationRequest();
//        request.setPropertyId(1L);
//        request.setTenantId(2L);
//        request.setStartDate(LocalDate.now().plusDays(1));
//        request.setEndDate(LocalDate.now().plusDays(2));
//        when(propertyRepository.findById(1L)).thenReturn(Optional.of(new Property()));
//        when(userRepository.findById(2L)).thenReturn(Optional.empty());
//        assertThatThrownBy(() -> reservationService.createReservation(request))
//                .isInstanceOf(NoTenantException.class);
//    }
//
//    @Test
//    void shouldThrowWhenNotAvailable() {
//        ReservationRequest request = new ReservationRequest();
//        request.setPropertyId(1L);
//        request.setTenantId(2L);
//        request.setStartDate(LocalDate.now().plusDays(1));
//        request.setEndDate(LocalDate.now().plusDays(2));
//
//        Property property = new Property();
//        property.setId(1L); // <--- ustaw ID
//        property.setOwner(new User());
//
//        User tenant = new User();
//        tenant.setId(2L); // opcjonalnie też ustaw ID tenantowi, jeśli gdzieś jest używane
//
//        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
//        when(userRepository.findById(2L)).thenReturn(Optional.of(tenant));
//        when(reservationRepository.findByPropertyIdAndDateRangeOverlap(anyLong(), any(LocalDate.class), any(LocalDate.class)))
//                .thenReturn(List.of(new Reservation()));
//
//        assertThatThrownBy(() -> reservationService.createReservation(request))
//                .isInstanceOf(InvalidDateRangeException.class)
//                .hasMessageContaining("Property is not available for the selected dates");
//    }
//
//    @Test
//    void shouldThrowWhenTenantIsOwner() {
//        ReservationRequest request = new ReservationRequest();
//        request.setPropertyId(1L);
//        request.setTenantId(2L);
//        request.setStartDate(LocalDate.now().plusDays(1));
//        request.setEndDate(LocalDate.now().plusDays(2));
//        User owner = new User();
//        owner.setId(2L); // same as tenant id
//        Property property = new Property();
//        property.setId(2L);
//        property.setOwner(owner);
//        User tenant = owner;
//
//        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
//        when(userRepository.findById(2L)).thenReturn(Optional.of(tenant));
//        when(reservationRepository.findByPropertyIdAndDateRangeOverlap(anyLong(), any(), any())).thenReturn(List.of());
//
//        assertThatThrownBy(() -> reservationService.createReservation(request))
//                .isInstanceOf(OwnerException.class)
//                .hasMessageContaining("Owner cannot reserve their own property");
//    }
//
//    @Test
//    void shouldThrowNotSpecifiedWhenDataAccessExceptionOccurs() {
//        ReservationRequest request = new ReservationRequest();
//        request.setPropertyId(1L);
//        request.setTenantId(2L);
//        request.setStartDate(LocalDate.now().plusDays(1));
//        request.setEndDate(LocalDate.now().plusDays(2));
//        when(propertyRepository.findById(anyLong())).thenThrow(new org.springframework.dao.DataAccessException("db error") {});
//        assertThatThrownBy(() -> reservationService.createReservation(request))
//                .isInstanceOf(NotSpecifiedException.class)
//                .hasMessageContaining("An error occurred while creating the reservation");
//    }
//
//}

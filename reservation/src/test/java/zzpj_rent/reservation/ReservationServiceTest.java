//package zzpj_rent.reservation;
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import zzpj_rent.reservation.dtos.request.ReservationRequest;
//import zzpj_rent.reservation.model.Property;
//import zzpj_rent.reservation.model.Reservation;
//import zzpj_rent.reservation.model.User;
//import zzpj_rent.reservation.repository.PropertyRepository;
//import zzpj_rent.reservation.repository.ReservationRepository;
//import zzpj_rent.reservation.repository.UserRepository;
//import zzpj_rent.reservation.services.ReservationService;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.verify;
//
//@ExtendWith(MockitoExtension.class)
//class ReservationServiceTest {
//
//    @Mock
//    private ReservationRepository reservationRepository;
//
//    @Mock
//    private PropertyRepository propertyRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @InjectMocks
//    private ReservationService reservationService;
//
//    @Test
//    void shouldCreateReservationWhenPropertyAvailable() {
//        // given
//        Long propertyId = 1L;
//        Long tenantId = 2L;
//        LocalDate startDate = LocalDate.of(2025, 6, 1);
//        LocalDate endDate = LocalDate.of(2025, 6, 5);
//
//        Property property = Property.builder().id(propertyId).build();
//        User tenant = User.builder().id(tenantId).build();
//
//        ReservationRequest request = new ReservationRequest();
//        request.setPropertyId(propertyId);
//        request.setTenantId(tenantId);
//        request.setStartDate(startDate);
//        request.setEndDate(endDate);
//
//        // Mockowanie repozytoriów
//        given(propertyRepository.findById(propertyId)).willReturn(Optional.of(property));
//        given(userRepository.findById(tenantId)).willReturn(Optional.of(tenant));
//        given(reservationRepository.findByPropertyIdAndDateRangeOverlap(propertyId, startDate, endDate))
//                .willReturn(Collections.emptyList());
//
//        Reservation savedReservation = Reservation.builder()
//                .id(10L)
//                .property(property)
//                .tenant(tenant)
//                .startDate(startDate)
//                .endDate(endDate)
//                .status(Reservation.Status.PENDING)
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        given(reservationRepository.save(any(Reservation.class))).willReturn(savedReservation);
//
//        // when
//        Reservation result = reservationService.createReservation(request);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.getId()).isEqualTo(10L);
//        assertThat(result.getProperty()).isEqualTo(property);
//        assertThat(result.getTenant()).isEqualTo(tenant);
//        assertThat(result.getStartDate()).isEqualTo(startDate);
//        assertThat(result.getEndDate()).isEqualTo(endDate);
//        assertThat(result.getStatus()).isEqualTo(Reservation.Status.PENDING);
//
//        // verify, że metoda save została wywołana
//        verify(reservationRepository).save(any(Reservation.class));
//    }
//
//    @Test
//    void shouldThrowExceptionWhenPropertyNotAvailableBecauseOfDate() {
//        // given
//        Long propertyId = 1L;
//        Long tenantId = 2L;
//        LocalDate startDate = LocalDate.of(2025, 6, 1);
//        LocalDate endDate = LocalDate.of(2025, 6, 5);
//
//        Property property = Property.builder().id(propertyId).build();
//        User tenant = User.builder().id(tenantId).build();
//
//        ReservationRequest request = new ReservationRequest();
//        request.setPropertyId(propertyId);
//        request.setTenantId(tenantId);
//        request.setStartDate(startDate);
//        request.setEndDate(endDate);
//
//        Reservation conflictingReservation = Reservation.builder().id(20L)
//                .startDate(LocalDate.of(2025, 6, 2))
//                .endDate(LocalDate.of(2025, 6, 5)).build();
//
//        given(propertyRepository.findById(propertyId)).willReturn(Optional.of(property));
//        given(userRepository.findById(tenantId)).willReturn(Optional.of(tenant));
//        // zwracamy niepustą listę – znaczy, że są kolizje terminów
//        given(reservationRepository.findByPropertyIdAndDateRangeOverlap(propertyId, startDate, endDate))
//                .willReturn(List.of(conflictingReservation));
//
//        // when + then
//        assertThatThrownBy(() -> reservationService.createReservation(request))
//                .isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("Property is not available");
//    }
//
//    @Test
//    void shouldThrowExceptionWhenOwnerTriesToReserveOwnProperty() {
//        Long propertyId = 1L;
//        Long ownerId = 100L;
//
//        User owner = User.builder()
//                .id(ownerId)
//                .fullName("Owner User")
//                .build();
//
//        Property property = Property.builder()
//                .id(propertyId)
//                .address("Testowa 1")
//                .city("Warszawa")
//                .owner(owner)
//                .build();
//
//        ReservationRequest request = new ReservationRequest(
//                propertyId,
//                ownerId,  // <- próbuje jako najemca
//                LocalDate.of(2025, 6, 1),
//                LocalDate.of(2025, 6, 5)
//        );
//
//        given(propertyRepository.findById(propertyId)).willReturn(Optional.of(property));
//        given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
//
//        // when + then
//        assertThatThrownBy(() -> reservationService.createReservation(request))
//                .isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("Owner cannot reserve their own property");
//    }
//}
//

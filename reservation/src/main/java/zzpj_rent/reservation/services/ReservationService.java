package zzpj_rent.reservation.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import zzpj_rent.reservation.dtos.request.ReservationRequest;
import zzpj_rent.reservation.model.Property;
import zzpj_rent.reservation.model.Reservation;
import zzpj_rent.reservation.model.User;
import zzpj_rent.reservation.repository.PropertyRepository;
import zzpj_rent.reservation.repository.ReservationRepository;
import zzpj_rent.reservation.repository.UserRepository;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public Reservation createReservation(ReservationRequest request) {
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        User tenant = userRepository.findById(request.getTenantId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        boolean isAvailable = reservationRepository
                .findByPropertyIdAndDateRangeOverlap(property.getId(), request.getStartDate(), request.getEndDate())
                .isEmpty();

        if (!isAvailable) {
            throw new RuntimeException("Property is not available for the selected dates");
        }

        if (tenant.equals(property.getOwner())) {
            throw new RuntimeException("Owner cannot reserve their own property");
        }

        Reservation reservation = Reservation.builder()
                .property(property)
                .tenant(tenant)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Reservation.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return reservationRepository.save(reservation);
    }
}

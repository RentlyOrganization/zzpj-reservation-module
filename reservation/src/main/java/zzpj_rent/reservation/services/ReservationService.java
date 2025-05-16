package zzpj_rent.reservation.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import zzpj_rent.reservation.dtos.request.ReservationRequest;
import zzpj_rent.reservation.dtos.response.ReservationResponse;
import zzpj_rent.reservation.exceptions.BadRequestException;
import zzpj_rent.reservation.exceptions.ForbiddenException;
import zzpj_rent.reservation.exceptions.NotFoundException;
import zzpj_rent.reservation.model.Property;
import zzpj_rent.reservation.model.Reservation;
import zzpj_rent.reservation.model.User;
import zzpj_rent.reservation.repository.PropertyRepository;
import zzpj_rent.reservation.repository.ReservationRepository;
import zzpj_rent.reservation.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public Reservation createReservation(ReservationRequest request) {
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new NotFoundException("Property not found"));

        User tenant = userRepository.findById(request.getTenantId())
                .orElseThrow(() -> new NotFoundException("Tenant not found"));

        boolean isAvailable = reservationRepository
                .findByPropertyIdAndDateRangeOverlap(property.getId(), request.getStartDate(), request.getEndDate())
                .isEmpty();

        if (!isAvailable) {
            throw new BadRequestException("Property is not available for the selected dates");
        }

        if (tenant.equals(property.getOwner())) {
            throw new ForbiddenException("Owner cannot reserve their own property");
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

    public List<ReservationResponse> getAllReservationsForTenant(Long id) {
        return reservationRepository.findByTenantId(id).stream().map(res ->
                ReservationResponse.builder()
                        .id(res.getId())
                        .tenantId(res.getTenant().getId())
                        .tenantName(res.getTenant().getFullName())
                        .propertyId(res.getProperty().getId())
                        .status(res.getStatus().name())
                        .startDate(res.getStartDate())
                        .endDate(res.getEndDate())
                        .build()).collect(Collectors.toList());
    }

    public List<ReservationResponse> getReservationsForTenantByStatus(Long id, Reservation.Status status) {
        return reservationRepository.findByStatusAndTenantId(status, id).stream().map(res ->
                ReservationResponse.builder()
                        .id(res.getId())
                        .tenantId(res.getTenant().getId())
                        .tenantName(res.getTenant().getFullName())
                        .propertyId(res.getProperty().getId())
                        .status(res.getStatus().name())
                        .startDate(res.getStartDate())
                        .endDate(res.getEndDate())
                        .build()).collect(Collectors.toList());
    }

}

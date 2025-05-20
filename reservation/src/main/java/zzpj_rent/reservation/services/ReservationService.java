package zzpj_rent.reservation.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import zzpj_rent.reservation.dtos.request.ReservationRequest;
import zzpj_rent.reservation.dtos.request.UpdateReservationRequest;
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

import java.time.LocalDate;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public Reservation createReservation(ReservationRequest request) {
        try {
            if (request.getStartDate() == null || request.getEndDate() == null) {
                throw new BadRequestException("Start date and end date are required");
            } else if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new BadRequestException("Start date cannot be after end date");
            } else if (request.getStartDate().isBefore(ChronoLocalDate.from(LocalDateTime.now())) ||
                    request.getEndDate().isBefore(ChronoLocalDate.from(LocalDateTime.now()))) {
                throw new BadRequestException("Start date or end date cannot be in the past");
            } else if (request.getStartDate().isEqual(request.getEndDate())) {
                throw new BadRequestException("Start date and end date cannot be the same");
            }

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
        } catch (Exception e) {
            throw new BadRequestException("An error occurred while creating the reservation");

        }
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

    public ReservationResponse getReservationByIdTenant(Long id, Long tenantId) {
        Reservation res = reservationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        return ReservationResponse.builder()
                .id(res.getId())
                .tenantId(res.getTenant().getId())
                .tenantName(res.getTenant().getFullName())
                .propertyId(res.getProperty().getId())
                .status(res.getStatus().name())
                .startDate(res.getStartDate())
                .endDate(res.getEndDate())
                .build();
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

    public String updateReservationStatus(Long id, Reservation.Status status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        switch (status) {
            case CONFIRMED -> {
                if (reservation.getStatus() != Reservation.Status.PENDING) {
                    throw new BadRequestException("Reservation can only be accepted if it is pending");
                } else {
                    reservation.setStatus(Reservation.Status.CONFIRMED);
                }
            }
            case REJECTED -> {
                if (reservation.getStatus() != Reservation.Status.PENDING) {
                    throw new BadRequestException("Reservation can only be rejected if it is pending");
                } else {
                    reservation.setStatus(Reservation.Status.REJECTED);
                }
            }
            case FINISHED -> {
                if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
                    throw new BadRequestException("Reservation can only be finished if it is accepted");
                } else {
                    reservation.setStatus(Reservation.Status.FINISHED);
                }
            }
            case CANCELLED -> {
                if (reservation.getStatus() == Reservation.Status.CONFIRMED) {
                    reservation.setStatus(Reservation.Status.CANCELLED);
                } else {
                    throw new BadRequestException("Reservation can only be cancelled if it is accepted");
                }
            }
            default ->
                throw new BadRequestException("Invalid reservation status");
        }

        reservationRepository.save(reservation);
        return "Reservation status updated to " + reservation.getStatus();
    }

    public String deleteReservation(Long id, Long tenantId) {
        Reservation reservation = reservationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new BadRequestException("Cannot delete a processed reservation");
        }

        reservationRepository.delete(reservation);
        return "Reservation deleted successfully";
    }

    public String updateReservation(Long id, Long tenantId, UpdateReservationRequest request) {
        Reservation reservation = reservationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        LocalDate startDate;
        LocalDate endDate;

        if (request.getStartDate() == null) {
            startDate = reservation.getStartDate();
        } else {
            startDate = request.getStartDate();
        }

        if (request.getEndDate() == null) {
            endDate = reservation.getEndDate();
        } else {
            endDate = request.getEndDate();
        }

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new BadRequestException("Cannot update a processed reservation");
        }

        boolean isAvailable = reservationRepository
                .findByPropertyIdAndDateRangeOverlap(reservation.getProperty().getId(), startDate, endDate)
                .isEmpty();

        if (!isAvailable) {
            throw new BadRequestException("Property is not available for the selected dates");
        }

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        } else if (startDate.isBefore(ChronoLocalDate.from(LocalDateTime.now())) ||
                endDate.isBefore(ChronoLocalDate.from(LocalDateTime.now()))) {
            throw new BadRequestException("Start date or end date cannot be in the past");
        } else if (startDate.isEqual(endDate)) {
            throw new BadRequestException("Start date and end date cannot be the same");
        }

        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);

        reservationRepository.save(reservation);
        return "Reservation updated successfully";

    }

}

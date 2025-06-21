package zzpj_rent.reservation.services;

import lombok.AllArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
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
    private final ApartmentClient apartmentClient;
    private final UserClient userClient;

    public Reservation createReservation(ReservationRequest request) {
        try {
            if (request.getStartDate() == null || request.getEndDate() == null) {
                throw new InvalidDateRangeException("Sart date and end date are required");
            } else if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new InvalidDateRangeException("Sart date cannot be after end date");
            } else if (request.getStartDate().isBefore(ChronoLocalDate.from(LocalDateTime.now())) ||
                    request.getEndDate().isBefore(ChronoLocalDate.from(LocalDateTime.now()))) {
                throw new InvalidDateRangeException("Start date or end date cannot be in the past");
            } else if (request.getStartDate().isEqual(request.getEndDate())) {
                throw new InvalidDateRangeException("Start date and end date cannot be the same");
            }

            Property property = propertyRepository.findById(request.getPropertyId())
                    .orElseThrow(NoPropertyException::new);

            User tenant = userRepository.findById(request.getTenantId())
                    .orElseThrow(NoTenantException::new);

            boolean isAvailable = reservationRepository
                    .findByPropertyIdAndDateRangeOverlap(property.getId(), request.getStartDate(), request.getEndDate())
                    .isEmpty();

            if (!isAvailable) {
                throw new InvalidDateRangeException("Property is not available for the selected dates");
            }

            if (tenant.equals(property.getOwner())) {
                throw new OwnerException("\"Owner cannot reserve their own property\"");
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
        } catch (DataAccessException | NullPointerException _) {
            throw new NotSpecifiedException("An error occurred while creating the reservation");
        }
    }

    //TODO NAPISAC TESTY DO TEGO
    public List<ReservationResponse> getAllReservationsForTenant(Long id) {
        System.out.println(apartmentClient.getApartmentById(1L));
        System.out.println(userClient.getUserById(1L));
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

    public List<ReservationResponse> getAllReservationsForOwner(Long propertyId, Long ownerId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(NoPropertyException::new);

        if (!property.getOwner().getId().equals(ownerId)) {
            throw new OwnerException("You are not the owner of this property");
        }

        return reservationRepository.findByPropertyId(propertyId).stream().map(res ->
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

    public ReservationResponse getReservationByIdForTenant(Long id, Long tenantId) {
        Reservation res = reservationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(NoReservationException::new);

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

    public ReservationResponse getReservationByIdForOwner(Long id, Long ownerId) {
        Reservation res = reservationRepository.findById(id)
                .orElseThrow(NoPropertyException::new);

        Property property = propertyRepository.findById(res.getProperty().getId())
                .orElseThrow(NoPropertyException::new);

        if (!property.getOwner().getId().equals(ownerId)) {
            throw new OwnerException("You are not the owner of this property");
        }

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
                .orElseThrow(NoReservationException::new);

        switch (status) {
            case CONFIRMED -> {
                if (reservation.getStatus() != Reservation.Status.PENDING) {
                    throw new ReservationStatusException("Reservation can only be accepted if it is pending");
                } else {
                    reservation.setStatus(Reservation.Status.CONFIRMED);
                }
            }
            case REJECTED -> {
                if (reservation.getStatus() != Reservation.Status.PENDING) {
                    throw new ReservationStatusException("Reservation can only be rejected if it is pending");
                } else {
                    reservation.setStatus(Reservation.Status.REJECTED);
                }
            }
            case FINISHED -> {
                if (reservation.getStatus() != Reservation.Status.CONFIRMED) {
                    throw new ReservationStatusException("Reservation can only be finished if it is accepted");
                } else {
                    reservation.setStatus(Reservation.Status.FINISHED);
                }
            }
            case CANCELLED -> {
                if (reservation.getStatus() == Reservation.Status.CONFIRMED) {
                    reservation.setStatus(Reservation.Status.CANCELLED);
                } else {
                    throw new ReservationStatusException("Reservation can only be cancelled if it is accepted");
                }
            }
            default ->
                    throw new ReservationStatusException("Invalid reservation status");
        }

        reservationRepository.save(reservation);
        return "Reservation status updated to " + reservation.getStatus();
    }

    public String deleteReservation(Long id, Long tenantId) {
        Reservation reservation = reservationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(NoReservationException::new);

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new ReservationStatusException("Cannot delete a processed reservation");
        }

        reservationRepository.delete(reservation);
        return "Reservation deleted successfully";
    }

    public String updateReservation(Long id, Long tenantId, UpdateReservationRequest request) {
        Reservation reservation = reservationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(NoReservationException::new);

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
            throw new ReservationStatusException("Cannot update a processed reservation");
        }

        boolean isAvailable = reservationRepository
                .findByPropertyIdAndDateRangeOverlap(reservation.getProperty().getId(), startDate, endDate)
                .isEmpty();

        if (!isAvailable) {
            throw new InvalidDateRangeException("Property is not available for the selected dates");
        }

        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException("Start date cannot be after end date");
        } else if (startDate.isBefore(ChronoLocalDate.from(LocalDateTime.now())) ||
                endDate.isBefore(ChronoLocalDate.from(LocalDateTime.now()))) {
            throw new InvalidDateRangeException("Start date or end date cannot be in the past");
        } else if (startDate.isEqual(endDate)) {
            throw new InvalidDateRangeException("Start date and end date cannot be the same");
        }

        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);

        reservationRepository.save(reservation);
        return "Reservation updated successfully";

    }

}

package zzpj_rent.reservation.services;

import feign.FeignException;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
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

import java.time.LocalDate;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final OpinionRepository opinionRepository;
    private final ApartmentClient apartmentClient;
    private final UserClient userClient;

    public Reservation createReservation(ReservationRequest request) {
        try {
            if (request.getStartDate() == null || request.getEndDate() == null) {
                throw new InvalidDateRangeException("Sart date and end date are required");
            } else if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new InvalidDateRangeException("Start date cannot be after end date");
            } else if (request.getStartDate().isBefore(ChronoLocalDate.from(LocalDateTime.now())) ||
                    request.getEndDate().isBefore(ChronoLocalDate.from(LocalDateTime.now()))) {
                throw new InvalidDateRangeException("Start date or end date cannot be in the past");
            } else if (request.getStartDate().isEqual(request.getEndDate())) {
                throw new InvalidDateRangeException("Start date and end date cannot be the same");
            }

            Property property = getProperty(request.getPropertyId());
            User tenant = getTenant(request.getTenantId());

            boolean isAvailable = reservationRepository
                    .findByPropertyIdAndDateRangeOverlap(property.getId(), request.getStartDate(), request.getEndDate())
                    .isEmpty();

            if (!isAvailable) {
                throw new InvalidDateRangeException("Property is not available for the selected dates");
            }

            if (tenant.getId().equals(property.getOwnerId())) {
                throw new OwnerException("Owner cannot reserve their own property");
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

    public List<ReservationResponse> getAllReservationsForTenant(Long id) {
        return reservationRepository.findByTenantId(id).stream().map(res ->
                ReservationResponse.builder()
                        .id(res.getId())
                        .tenantId(res.getTenant().getId())
                        .tenantName(res.getTenant().getFirstName())
                        .tenantSurname(res.getTenant().getLastName())
                        .propertyId(res.getProperty().getId())
                        .status(res.getStatus().name())
                        .startDate(res.getStartDate())
                        .endDate(res.getEndDate())
                        .build()).collect(Collectors.toList());
    }

    public List<ReservationResponse> getAllReservationsForOwner(Long propertyId, Long ownerId) {
        Property property = getProperty(propertyId);

        if (!property.getOwnerId().equals(ownerId)) {
            throw new OwnerException("You are not the owner of this property");
        }

        return reservationRepository.findByPropertyId(propertyId).stream().map(res ->
                        ReservationResponse.builder()
                        .id(res.getId())
                        .tenantId(res.getTenant().getId())
                        .tenantName(res.getTenant().getFirstName())
                        .tenantSurname(res.getTenant().getLastName())
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
                .tenantName(res.getTenant().getFirstName())
                .tenantSurname(res.getTenant().getLastName())
                .propertyId(res.getProperty().getId())
                .status(res.getStatus().name())
                .startDate(res.getStartDate())
                .endDate(res.getEndDate())
                .build();
    }

    public ReservationResponse getReservationByIdForOwner(Long id, Long ownerId) {
        Reservation res = reservationRepository.findById(id)
                .orElseThrow(NoPropertyException::new);

        Property property = getProperty(res.getProperty().getId());

        if (!property.getOwnerId().equals(ownerId)) {
            throw new OwnerException("You are not the owner of this property");
        }

        return ReservationResponse.builder()
                .id(res.getId())
                .tenantId(res.getTenant().getId())
                .tenantName(res.getTenant().getFirstName())
                .tenantSurname(res.getTenant().getLastName())
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
                        .tenantName(res.getTenant().getFirstName())
                        .tenantSurname(res.getTenant().getLastName())
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

    public Opinion createOpinion(Long reservationId, Long userId, OpinionRequest request) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(NoReservationException::new);

        if (reservation.getStatus() != Reservation.Status.FINISHED) {
            throw new ReservationStatusException("Opinion can only be created for finished reservations");
        }

        if (request.getRating() < 0 || request.getRating() > 5) {
            throw new InvalidRatingException();
        }

        User user;
        User creator;
        if (reservation.getTenant().getId().equals(userId)) {
            // Dodajemy opinię właścicielowi mieszkania
            creator = reservation.getTenant();
            Long ownerId = reservation.getProperty().getOwnerId();
            user = getTenant(ownerId);
        } else {
            // Dodajemy opinię najemcy
            Long ownerId = reservation.getProperty().getOwnerId();
            creator = getTenant(ownerId);
            user = reservation.getTenant();
        }

        Opinion opinion = Opinion.builder()
                .content(request.getContent())
                .rating(request.getRating())
                .user(user)
                .creator(creator)
                .build();

        return opinionRepository.save(opinion);
    }

    public List<OpinionResponse> getAllOpinionsByUser(Long userId) {
        return opinionRepository.findAllByUser_Id(userId).stream().map(o ->
                OpinionResponse.builder()
                        .id(o.getId())
                        .content(o.getContent())
                        .rating(o.getRating())
                        .firstName(o.getUser().getFirstName())
                        .lastName(o.getUser().getLastName())
                        .build()).collect(Collectors.toList());
    }

    public String deleteOpinion(Long userId, Long opinionId) {
        Opinion opinion = opinionRepository.findById(opinionId).orElseThrow(NoOpinionException::new);

        if (!opinion.getCreator().getId().equals(userId)) {
            throw new OwnerException("To nie jest twoja opinia!");
        }

        opinionRepository.delete(opinion);
        return "Pomyślnie usunięto opinię";
    }

    public Property getProperty(Long id) {
        ApartmentDTO apartmentDto;
        try {
            apartmentDto = apartmentClient.getApartmentById(id);
        } catch (FeignException.NotFound _) {
            throw new NoPropertyException();
        }
        return  new Property(apartmentDto.getId(), apartmentDto.getPrice(), apartmentDto.getRentalType(),
                apartmentDto.getOwnerId());
    }

    public User getTenant(Long id) {
        UserDTO userDto;
        try {
            userDto = userClient.getUserById(id);
        } catch (FeignException.NotFound _) {
            throw new NoTenantException();
        }

        return new User(userDto.getId(), userDto.getUsername(), userDto.getEmail(),
                userDto.getFirstName(), userDto.getLastName());
    }

}

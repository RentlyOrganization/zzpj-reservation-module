package zzpj_rent.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zzpj_rent.reservation.model.Reservation;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("SELECT r FROM Reservation r WHERE r.property.id = :propertyId " +
            "AND r.status = 'CONFIRMED' " +
            "AND (r.startDate <= :endDate AND r.endDate >= :startDate)")
    List<Reservation> findByPropertyIdAndDateRangeOverlap(
            @Param("propertyId") Long propertyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    List<Reservation> findByTenantId(Long tenantId);
    List<Reservation> findByStatusAndTenantId(Reservation.Status status, Long tenantId);
}

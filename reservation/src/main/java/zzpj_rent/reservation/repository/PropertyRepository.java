package zzpj_rent.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zzpj_rent.reservation.model.Property;

import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    Optional<Property> findById(Long id);
}

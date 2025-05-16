package zzpj_rent.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zzpj_rent.reservation.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByFullName(String fullName);
}

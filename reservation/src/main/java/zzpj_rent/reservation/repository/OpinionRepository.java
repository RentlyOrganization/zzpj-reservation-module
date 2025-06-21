package zzpj_rent.reservation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zzpj_rent.reservation.model.Opinion;

import java.util.List;

public interface OpinionRepository extends JpaRepository<Opinion, Long> {
    List<Opinion> findAllByUser_Id(Long userId);
}

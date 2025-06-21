package zzpj_rent.reservation.microservices;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import zzpj_rent.reservation.dtos.request.ApartmentDTO;

@FeignClient(name = "Rental-listing-service")  // używaj "application.name" z apartment-service
public interface ApartmentClient {

    @GetMapping("/api/apartments/{id}")
    ApartmentDTO getApartmentById(@PathVariable Long id);

}
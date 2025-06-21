package zzpj_rent.reservation.microservices;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import zzpj_rent.reservation.dtos.request.UserDTO;

@FeignClient(name = "RENTLYAUTH")
public interface UserClient {
    @GetMapping("/findUserByID/{userId}")
    UserDTO getUserById(@PathVariable Long userId);
}

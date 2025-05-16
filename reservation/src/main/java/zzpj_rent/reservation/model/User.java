package zzpj_rent.reservation.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;

    // Relacje
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Property> properties;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL)
    private List<Reservation> reservations;

}


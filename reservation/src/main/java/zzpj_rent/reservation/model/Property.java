package zzpj_rent.reservation.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "properties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacja do właściciela
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private User owner;

    private String address;
    private String city;

    // Rezerwacje
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Reservation> reservations;
}


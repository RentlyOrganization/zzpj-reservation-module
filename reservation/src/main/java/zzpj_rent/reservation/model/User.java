package zzpj_rent.reservation.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;



}
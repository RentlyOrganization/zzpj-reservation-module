package zzpj_rent.reservation.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpinionResponse {
    private Long id;
    private int rating;
    private String content;
    private String firstName;
    private String lastName;
}

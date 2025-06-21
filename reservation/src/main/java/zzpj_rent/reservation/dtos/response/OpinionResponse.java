package zzpj_rent.reservation.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OpinionResponse {
    private Long id;
    private int rating;
    private String content;
    private String firstName;
    private String lastName;
}

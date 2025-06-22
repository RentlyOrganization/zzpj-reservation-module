package zzpj_rent.reservation.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApartmentDTO {
    private Long id;
    private BigDecimal price;
    private String location;
    private Integer rooms;
    private String rentalType;
    private Boolean available;
    private Double latitude;
    private Double longitude;
    private Long ownerId;
    private String ownerName;
    private BigDecimal averageRating;
    private Integer ratingCount;
    private Long viewCount;
}

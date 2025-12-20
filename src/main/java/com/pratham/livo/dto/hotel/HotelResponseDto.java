package com.pratham.livo.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelResponseDto {
    private Long id;
    private String name;
    private String city;
    private List<String> photos;
    private List<String> amenities;
    private HotelContactInfoDto contactInfo;
    private Boolean active;
    private Boolean deleted;
}

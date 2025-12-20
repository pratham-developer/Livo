package com.pratham.livo.dto.room;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomResponseDto {
    private Long id;
    private String type;
    private BigDecimal basePrice;
    private Boolean active;
    private Boolean deleted;
    private List<String> photos;
    private List<String> amenities;
    private Integer capacity;
    private Boolean available;
}

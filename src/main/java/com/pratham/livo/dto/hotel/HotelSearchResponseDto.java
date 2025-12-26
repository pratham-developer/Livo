package com.pratham.livo.dto.hotel;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
public class HotelSearchResponseDto extends HotelResponseDto{
    private BigDecimal pricePerDay;
}

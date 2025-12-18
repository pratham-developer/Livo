package com.pratham.livo.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HotelSearchRequestDto {
    private String city;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer roomsCount;

}

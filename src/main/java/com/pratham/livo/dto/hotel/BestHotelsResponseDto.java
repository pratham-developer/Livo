package com.pratham.livo.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BestHotelsResponseDto {
    private Long id;
    private String name;
    private String city;
    private List<String> photos;
}

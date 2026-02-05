package com.pratham.livo.projection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManagersHotelWrapper {
    private Long id;
    private String name;
    private String city;
    private List<String> photos;
    private Boolean active;
}

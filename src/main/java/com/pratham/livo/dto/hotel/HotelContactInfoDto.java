package com.pratham.livo.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelContactInfoDto {

    private String address;
    private String phoneNumber;
    private String email;
    private String location;
}
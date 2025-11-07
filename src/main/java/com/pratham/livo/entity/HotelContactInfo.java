package com.pratham.livo.entity;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
//this won't be an entity(table will not be formed)
//this is an embedded class which will be a part of the hotel entity only
@Embeddable //can be embedded into another entity
public class HotelContactInfo {

    private String address;
    private String phoneNumber;
    private String email;
    private String location;
}

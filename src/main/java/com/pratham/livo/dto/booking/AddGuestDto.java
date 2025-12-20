package com.pratham.livo.dto.booking;

import com.pratham.livo.entity.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddGuestDto {
    private String name;
    private Integer age;
    private Gender gender;
}

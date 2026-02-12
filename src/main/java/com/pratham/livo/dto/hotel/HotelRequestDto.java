package com.pratham.livo.dto.hotel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelRequestDto {
    @NotBlank(message = "Hotel name is required")
    private String name;

    @NotBlank(message = "City is required")
    private String city;

    @NotNull(message = "Photos list cannot be null")
    @Size(min = 1, max = 5, message = "Provide between 1 and 5 photos")
    private List<String> photos;

    @NotNull(message = "Amenities list cannot be null")
    @Size(min = 1, max = 20, message = "Provide between 1 and 20 amenities")
    private List<String> amenities;

    @NotNull(message = "Contact info is required")
    @Valid
    private HotelContactInfoDto contactInfo;

    private Boolean active;
}
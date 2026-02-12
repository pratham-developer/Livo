package com.pratham.livo.dto.room;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomRequestDto {
    @NotBlank(message = "Room type is required")
    private String type;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal basePrice;

    @NotNull(message = "Photos list cannot be null")
    @Size(min = 1, max = 5, message = "Provide between 1 and 5 photos")
    private List<String> photos;

    @NotNull(message = "Amenities list cannot be null")
    @Size(min = 1, max = 20, message = "Provide between 1 and 20 amenities")
    private List<String> amenities;

    @NotNull(message = "Total count is required")
    @Min(value = 1, message = "Total count must be at least 1")
    private Integer totalCount;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;
}

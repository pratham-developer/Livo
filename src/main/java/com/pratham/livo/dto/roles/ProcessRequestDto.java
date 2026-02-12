package com.pratham.livo.dto.roles;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessRequestDto {
    @NotNull(message = "Approval status is required")
    Boolean approve;
}

package com.pratham.livo.dto.booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequestDto {
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer roomsCount;
}

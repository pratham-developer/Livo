package com.pratham.livo.dto.booking;

import com.pratham.livo.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingWrapperDto {
    private Long bookingId;
    private String hotelName;
    private String hotelCity;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus bookingStatus;
}

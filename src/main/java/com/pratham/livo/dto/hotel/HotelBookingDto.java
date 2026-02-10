package com.pratham.livo.dto.hotel;

import com.pratham.livo.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelBookingDto {
    private Long bookingId;
    private String roomType;
    private Integer roomCapacity;
    private Integer roomsCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus bookingStatus;
}

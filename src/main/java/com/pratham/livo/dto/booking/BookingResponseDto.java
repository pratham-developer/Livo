package com.pratham.livo.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pratham.livo.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponseDto {
    private Long id;
    private Long hotelId;
    private Long roomId;
    private Long userId;
    private String hotelName;
    private String roomType;
    private String hotelCity;
    private Integer roomsCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BookingStatus bookingStatus;
    private Set<GetGuestDto> guests;
    private String idempotencyKey;
}

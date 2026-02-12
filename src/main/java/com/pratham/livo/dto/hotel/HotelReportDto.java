package com.pratham.livo.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HotelReportDto {
    //confirmed
    private Long confirmedBookings;
    private BigDecimal confirmedRevenue;
    private BigDecimal avgRevenuePerConfirmedBooking;

    //cancelled
    private Long cancelledBookings;
    private BigDecimal revenueLostToCancellations; // booking amount
    private BigDecimal totalRefundsProcessed;      // actual refund amount

    //performance ratio
    private Double cancellationRate; // (cancelled / confirmed + cancelled)) * 100
}

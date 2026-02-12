package com.pratham.livo.projection;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class HotelBookingStats {
    private Long confirmedCount;
    private BigDecimal confirmedRevenue;
    private Long cancelledCount;
    private BigDecimal lostRevenue;

    public HotelBookingStats(Long confirmedCount, BigDecimal confirmedRevenue, Long cancelledCount, BigDecimal lostRevenue) {
        this.confirmedCount = (confirmedCount != null) ? confirmedCount : 0L;
        this.confirmedRevenue = (confirmedRevenue != null) ? confirmedRevenue : BigDecimal.ZERO;
        this.cancelledCount = (cancelledCount != null) ? cancelledCount : 0L;
        this.lostRevenue = (lostRevenue != null) ? lostRevenue : BigDecimal.ZERO;
    }
}

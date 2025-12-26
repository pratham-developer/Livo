package com.pratham.livo.projection;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class PriceCheckWrapper {
    private Long hotelId;
    private Long roomId;
    private BigDecimal avgPrice;

    public PriceCheckWrapper(Long hotelId, Long roomId,BigDecimal sumPrice, Long count) {
        this.hotelId = hotelId;
        this.roomId = roomId;
        if (count == 0) {
            this.avgPrice = BigDecimal.ZERO;
        } else {
            this.avgPrice = sumPrice.divide(BigDecimal.valueOf(count),2,RoundingMode.HALF_UP);
        }
    }
}

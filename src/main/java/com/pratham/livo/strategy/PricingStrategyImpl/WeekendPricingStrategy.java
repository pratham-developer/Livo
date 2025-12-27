package com.pratham.livo.strategy.PricingStrategyImpl;

import com.pratham.livo.entity.Inventory;
import com.pratham.livo.strategy.PricingStrategy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;

@Component
@Order(4)
public class WeekendPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculatePrice(BigDecimal currentPrice, Inventory inventory) {
        DayOfWeek day = inventory.getDate().getDayOfWeek();

        //hike by 15% if inventory is for weekend
        if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day==DayOfWeek.SUNDAY) {
            return currentPrice.multiply(BigDecimal.valueOf(1.15)).setScale(2, RoundingMode.HALF_UP);
        }
        return currentPrice;
    }
}

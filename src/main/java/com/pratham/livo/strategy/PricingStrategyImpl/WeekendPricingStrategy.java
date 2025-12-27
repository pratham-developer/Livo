package com.pratham.livo.strategy.PricingStrategyImpl;

import com.pratham.livo.entity.Inventory;
import com.pratham.livo.strategy.PricingStrategy;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;

@RequiredArgsConstructor
public class WeekendPricingStrategy implements PricingStrategy {

    private final PricingStrategy wrappedPricingStrategy;

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal basePrice = wrappedPricingStrategy.calculatePrice(inventory);
        DayOfWeek day = inventory.getDate().getDayOfWeek();

        //hike by 15% if inventory is for weekend
        if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day==DayOfWeek.SUNDAY) {
            return basePrice.multiply(BigDecimal.valueOf(1.15)).setScale(2, RoundingMode.HALF_UP);
        }
        return basePrice;
    }
}

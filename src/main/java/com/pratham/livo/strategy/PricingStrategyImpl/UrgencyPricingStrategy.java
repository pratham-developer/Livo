package com.pratham.livo.strategy.PricingStrategyImpl;

import com.pratham.livo.entity.Inventory;
import com.pratham.livo.service.impl.DateValidator;
import com.pratham.livo.strategy.PricingStrategy;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@RequiredArgsConstructor
public class UrgencyPricingStrategy implements PricingStrategy {

    private final PricingStrategy wrappedPricingStrategy;

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal basePrice = wrappedPricingStrategy.calculatePrice(inventory);

        LocalDate today = LocalDate.now();
        long daysUntilStay = java.time.temporal.ChronoUnit.DAYS.between(today, inventory.getDate());

        if (daysUntilStay >= 30) {
            // 10% discount for early booking
            return basePrice.multiply(BigDecimal.valueOf(0.9)).setScale(2, RoundingMode.HALF_UP);
        } else if (daysUntilStay <= 7 && daysUntilStay >= 0) {
            // 15% hike for last week
            return basePrice.multiply(BigDecimal.valueOf(1.15)).setScale(2, RoundingMode.HALF_UP);
        } else if (daysUntilStay <= 15 && daysUntilStay > 7) {
            // 5% hike for second last week
            return basePrice.multiply(BigDecimal.valueOf(1.05)).setScale(2, RoundingMode.HALF_UP);
        }
        return basePrice;
    }
}

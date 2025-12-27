package com.pratham.livo.strategy.PricingStrategyImpl;

import com.pratham.livo.entity.Inventory;
import com.pratham.livo.strategy.PricingStrategy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Component
@Order(3)
public class UrgencyPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculatePrice(BigDecimal currentPrice, Inventory inventory) {

        LocalDate today = LocalDate.now();
        long daysUntilStay = java.time.temporal.ChronoUnit.DAYS.between(today, inventory.getDate());

        if (daysUntilStay >= 30) {
            // 10% discount for early booking
            return currentPrice.multiply(BigDecimal.valueOf(0.9)).setScale(2, RoundingMode.HALF_UP);
        } else if (daysUntilStay <= 7 && daysUntilStay >= 0) {
            // 15% hike for last week
            return currentPrice.multiply(BigDecimal.valueOf(1.15)).setScale(2, RoundingMode.HALF_UP);
        } else if (daysUntilStay <= 15 && daysUntilStay > 7) {
            // 5% hike for second last week
            return currentPrice.multiply(BigDecimal.valueOf(1.05)).setScale(2, RoundingMode.HALF_UP);
        }
        return currentPrice;
    }
}

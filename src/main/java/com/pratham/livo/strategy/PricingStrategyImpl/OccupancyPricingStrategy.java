package com.pratham.livo.strategy.PricingStrategyImpl;

import com.pratham.livo.entity.Inventory;
import com.pratham.livo.strategy.PricingStrategy;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RequiredArgsConstructor
public class OccupancyPricingStrategy implements PricingStrategy {

    private final PricingStrategy wrappedPricingStrategy;

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {

        BigDecimal basePrice = wrappedPricingStrategy.calculatePrice(inventory);
        if (inventory.getTotalCount() == 0){
            return basePrice;
        }
        // get the filled ratio
        double filledRatio = (double) inventory.getBookedCount() / inventory.getTotalCount();
        // 40% hike if 80% booked
        if (filledRatio > 0.80) {
            return basePrice.multiply(BigDecimal.valueOf(1.4)).setScale(2, RoundingMode.HALF_UP);
        // 10% hike if 50% booked
        } else if (filledRatio > 0.50) {
            return basePrice.multiply(BigDecimal.valueOf(1.1)).setScale(2, RoundingMode.HALF_UP);
        }
        return basePrice;
    }
}

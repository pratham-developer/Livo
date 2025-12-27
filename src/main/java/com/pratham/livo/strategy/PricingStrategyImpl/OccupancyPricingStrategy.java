package com.pratham.livo.strategy.PricingStrategyImpl;

import com.pratham.livo.entity.Inventory;
import com.pratham.livo.strategy.PricingStrategy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Order(2)
public class OccupancyPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculatePrice(BigDecimal currentPrice, Inventory inventory) {

        if (inventory.getTotalCount() == 0){
            return currentPrice;
        }
        // get the filled ratio
        double filledRatio = (double) inventory.getBookedCount() / inventory.getTotalCount();
        // 40% hike if 80% booked
        if (filledRatio > 0.80) {
            return currentPrice.multiply(BigDecimal.valueOf(1.4)).setScale(2, RoundingMode.HALF_UP);
        // 10% hike if 50% booked
        } else if (filledRatio > 0.50) {
            return currentPrice.multiply(BigDecimal.valueOf(1.1)).setScale(2, RoundingMode.HALF_UP);
        }
        return currentPrice;
    }
}

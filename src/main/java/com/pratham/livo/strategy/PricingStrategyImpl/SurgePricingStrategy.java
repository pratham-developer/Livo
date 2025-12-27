package com.pratham.livo.strategy.PricingStrategyImpl;

import com.pratham.livo.entity.Inventory;
import com.pratham.livo.strategy.PricingStrategy;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RequiredArgsConstructor
public class SurgePricingStrategy implements PricingStrategy {

    private final PricingStrategy wrappedPricingStrategy;

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal basePrice = wrappedPricingStrategy.calculatePrice(inventory);
        return basePrice.multiply(inventory.getSurgeFactor()).setScale(2, RoundingMode.HALF_UP);
    }
}

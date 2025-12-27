package com.pratham.livo.strategy.PricingStrategyImpl;

import com.pratham.livo.entity.Inventory;
import com.pratham.livo.strategy.PricingStrategy;

import java.math.BigDecimal;

public class BasePricingStrategy implements PricingStrategy {
    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        return inventory.getRoom().getBasePrice();
    }
}

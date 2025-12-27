package com.pratham.livo.strategy.PricingStrategyImpl;

import com.pratham.livo.entity.Inventory;
import com.pratham.livo.strategy.PricingStrategy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Order(1)
public class SurgePricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculatePrice(BigDecimal currentPrice, Inventory inventory) {
        if(inventory.getSurgeFactor()!=null){
            return currentPrice.multiply(inventory.getSurgeFactor()).setScale(2, RoundingMode.HALF_UP);
        }
        return currentPrice;
    }
}

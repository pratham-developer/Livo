package com.pratham.livo.service;

import com.pratham.livo.entity.Inventory;

import java.math.BigDecimal;

public interface PricingService {
    BigDecimal calculateDynamicPrice(Inventory inventory);
    void updateInventoryPrices();
}

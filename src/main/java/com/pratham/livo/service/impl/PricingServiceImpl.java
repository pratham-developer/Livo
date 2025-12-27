package com.pratham.livo.service.impl;

import com.pratham.livo.entity.Inventory;
import com.pratham.livo.service.PricingService;
import com.pratham.livo.strategy.PricingStrategy;
import com.pratham.livo.strategy.PricingStrategyImpl.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingServiceImpl implements PricingService {

    //decorator pattern for pricing strategy
    @Override
    public BigDecimal calculateDynamicPrice(Inventory inventory) {
        PricingStrategy pricingStrategy = new BasePricingStrategy();
        pricingStrategy = new SurgePricingStrategy(pricingStrategy);
        pricingStrategy = new OccupancyPricingStrategy(pricingStrategy);
        pricingStrategy = new UrgencyPricingStrategy(pricingStrategy);
        pricingStrategy = new WeekendPricingStrategy(pricingStrategy);

        return pricingStrategy.calculatePrice(inventory);
    }

    @Override
    public void updateInventoryPrices() {
        //TODO: implement cron job
    }
}

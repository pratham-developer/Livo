package com.pratham.livo.service.impl;

import com.pratham.livo.entity.Inventory;
import com.pratham.livo.repository.InventoryRepository;
import com.pratham.livo.service.PricingService;
import com.pratham.livo.strategy.PricingStrategy;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingServiceImpl implements PricingService {

    private final TransactionTemplate transactionTemplate;
    private final InventoryRepository inventoryRepository;
    private final EntityManager entityManager;
    // spring automatically injects all beans implementing PricingStrategy, sorted by @Order
    private final List<PricingStrategy> pricingStrategies;

    //decorator pattern for pricing strategy
    @Override
    public BigDecimal calculateDynamicPrice(Inventory inventory) {
        BigDecimal currentPrice = inventory.getRoom().getBasePrice();
        for(PricingStrategy pricingStrategy : pricingStrategies){
            currentPrice = pricingStrategy.calculatePrice(currentPrice,inventory);
        }
        return currentPrice;
    }

    @Override
    @Scheduled(cron = "0 0 * * * *")
    public void updateInventoryPrices() {
        log.info("Starting Hourly Dynamic Pricing Cron Job");
        long start = System.currentTimeMillis();

        //setup
        boolean hasMore = true;
        int batchSize = 365;
        int pageNumber = 0;
        int totalProcessed = 0;
        LocalDate today = LocalDate.now();

        //while loop for processing batches
        while(hasMore){
            final int num = pageNumber; //page number
            //start transaction in batches
            Integer count = transactionTemplate.execute(status -> {

                //get page of inventories which are available and not closed for today and after
                Pageable limit = PageRequest.of(num,batchSize, Sort.by("id").ascending());
                Page<Inventory> inventoryPage = inventoryRepository.findInventoriesForDynamicPricing(today,limit);

                // if no inventory then return 0
                if(inventoryPage.isEmpty()){
                    return 0;
                }

                List<Inventory> inventoryList = inventoryPage.getContent();

                //for each inventory
                for(Inventory i : inventoryList){
                    //calculate the new price
                    BigDecimal newPrice = calculateDynamicPrice(i);
                    if(newPrice.compareTo(i.getRoom().getBasePrice()) < 0){
                        newPrice = i.getRoom().getBasePrice();
                    }
                    //set the new price if changed
                    if(newPrice.compareTo(i.getPrice()) != 0){
                        i.setPrice(newPrice);
                    }
                }

                //save the inventories
                inventoryRepository.saveAll(inventoryList);

                //clear objects from memory
                entityManager.flush();
                entityManager.clear();
                return inventoryList.size();
            });

            //if no update then stop loop
            if(count==null || count == 0){
                hasMore = false;
            }else{
                pageNumber++;
                totalProcessed += count;
            }
        }
        log.info("Dynamic Pricing Job Finished. Updated {} records in {}ms",
                totalProcessed, System.currentTimeMillis() - start);
    }
}

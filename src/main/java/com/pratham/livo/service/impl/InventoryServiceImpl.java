package com.pratham.livo.service.impl;

import com.pratham.livo.entity.Inventory;
import com.pratham.livo.entity.Room;
import com.pratham.livo.repository.InventoryRepository;
import com.pratham.livo.service.InventoryMaintainer;
import com.pratham.livo.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMaintainer inventoryMaintainer;

    @Override
    @Transactional
    public void initRoomFor1Year(Room room) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);
        List<Inventory> inventoryList = new ArrayList<>();
        for(;!today.isAfter(endDate);today=today.plusDays(1)){
            inventoryList.add(Inventory.builder()
                    .date(today)
                    .city(room.getHotel().getCity())
                    .hotel(room.getHotel())
                    .room(room)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .bookedCount(0)
                    .reservedCount(0)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build());
        }
        inventoryRepository.saveAll(inventoryList);
    }

    @Override
    public BigDecimal calculateTotalAmount(List<Inventory> inventoryList) {
        if (inventoryList == null || inventoryList.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return inventoryList.stream()
                .map(Inventory::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateAveragePrice(List<Inventory> inventoryList) {
        BigDecimal total = calculateTotalAmount(inventoryList);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(inventoryList.size()), 2, RoundingMode.HALF_UP);
    }

    @Override
    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(
            name = "dailyInventoryRollTask",
            lockAtLeastFor = "PT30M",
            lockAtMostFor = "PT1H"
    )
    public void runDailyInventoryRoll() {
        log.info("SCHEDULER: triggering daily inventory rollover");
        try{
            long start = System.currentTimeMillis();

            //start inventory maintenance
            inventoryMaintainer.performDailyMaintenance();

            long end = System.currentTimeMillis();
            log.info("SCHEDULER: inventory rollover job finished in {} ms", (end - start));
        }catch (Exception e){
            log.error("SCHEDULER: failure in inventory rollover job", e);
        }
    }
}

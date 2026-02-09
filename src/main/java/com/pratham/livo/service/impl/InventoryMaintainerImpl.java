package com.pratham.livo.service.impl;

import com.pratham.livo.repository.InventoryRepository;
import com.pratham.livo.service.InventoryMaintainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryMaintainerImpl implements InventoryMaintainer {

    private final InventoryRepository inventoryRepository;

    //used inside cron job
    @Transactional
    public void performDailyMaintenance() {
        LocalDate today = LocalDate.now(); //day for deleting inventory
        LocalDate targetDate = today.plusYears(1); //day for adding inventory
        log.info("INVENTORY MAINTENANCE: cleaning before {} and filling {}", today, targetDate);

        //delete inventory older than today
        inventoryRepository.deleteOldInventory(today);
        //add new inventory for rollover
        inventoryRepository.createInventoryForDate(targetDate);

        log.info("INVENTORY MAINTENANCE COMPLETE: inventory window rolled forward");
    }

}

package com.pratham.livo.service.impl;

import com.pratham.livo.entity.Inventory;
import com.pratham.livo.entity.Room;
import com.pratham.livo.repository.InventoryRepository;
import com.pratham.livo.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

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
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build());
        }
        inventoryRepository.saveAll(inventoryList);
    }
}

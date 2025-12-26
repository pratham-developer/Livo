package com.pratham.livo.service;

import com.pratham.livo.entity.Inventory;
import com.pratham.livo.entity.Room;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryService {
    void initRoomFor1Year(Room room);
    BigDecimal calculateTotalAmount(List<Inventory> inventoryList);
    BigDecimal calculateAveragePrice(List<Inventory> inventoryList);

    //TODO: implement cron job(daily) for removing old inventories and adding inventories for the next day
}

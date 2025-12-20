package com.pratham.livo.service;

import com.pratham.livo.entity.Room;

public interface InventoryService {
    void initRoomFor1Year(Room room);

    //TODO: implement cron job(daily) for removing old inventories and adding inventories for the next day
}

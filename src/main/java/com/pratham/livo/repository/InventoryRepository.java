package com.pratham.livo.repository;

import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Inventory;
import com.pratham.livo.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory,Long> {
    void deleteByRoom(Room room);

    void deleteByHotel(Hotel hotel);
}

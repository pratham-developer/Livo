package com.pratham.livo.repository;

import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room,Long> {
    List<Room> findByHotel_Id(Long hotelId);

    List<Room> findByHotel(Hotel hotel);

    void deleteByHotel(Hotel hotel);
}

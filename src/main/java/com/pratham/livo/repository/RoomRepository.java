package com.pratham.livo.repository;

import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room,Long> {

    List<Room> findByHotel(Hotel hotel);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Room r
    SET r.active = false,
        r.deleted = true
    WHERE r.hotel = :hotel
""")
    int softDeleteByHotel(@Param("hotel") Hotel hotel);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Room r
    SET r.active = true
    WHERE r.hotel = :hotel
      AND r.deleted = false
""")
    void activateNonDeleted(@Param("hotel") Hotel hotel);

    List<Room> findByHotelAndDeletedFalse(Hotel hotel);


    long countByHotelIdAndDeletedFalse(Long hotelId);
}

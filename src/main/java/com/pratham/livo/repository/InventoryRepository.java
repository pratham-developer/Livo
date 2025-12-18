package com.pratham.livo.repository;

import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Inventory;
import com.pratham.livo.entity.Room;
import com.pratham.livo.projection.RoomAvailabilityWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory,Long> {
    void deleteByRoom(Room room);

    void deleteByHotel(Hotel hotel);

    @Query("""
            SELECT DISTINCT i.hotel FROM Inventory i 
            WHERE 
            i.city = :city AND 
            i.date BETWEEN :start_date AND :end_date AND
            i.closed = false AND
            (i.totalCount-i.bookedCount) >= :rooms_count
            GROUP BY i.hotel, i .room HAVING COUNT(i.date) =  :date_count
    """)
    Page<Hotel> findAvailableHotels(
            @Param("city") String city,
            @Param("start_date") LocalDate startDate,
            @Param("end_date") LocalDate endDate,
            @Param("rooms_count") Integer roomsCount,
            @Param("date_count") Long dateCount,
            Pageable pageable
    );

    @Query("""
            SELECT new com.pratham.livo.projection.RoomAvailabilityWrapper(
                r,
                    CASE
                        WHEN(
                            SELECT COUNT(i) FROM Inventory i
                            WHERE i.room.id = r.id AND
                            i.date BETWEEN :start_date AND :end_date AND
                            (i.totalCount-i.bookedCount) >= :rooms_count
                        ) = :date_count
                            THEN true ELSE false
                    END
                ) FROM Room r
                  WHERE r.hotel.id = :hotel_id
    """)
    List<RoomAvailabilityWrapper> findRoomsWithAvailability(
            @Param("hotel_id") Long hotelId,
            @Param("start_date") LocalDate startDate,
            @Param("end_date") LocalDate endDate,
            @Param("rooms_count") Integer roomsCount,
            @Param("date_count") Long dateCount
    );
}

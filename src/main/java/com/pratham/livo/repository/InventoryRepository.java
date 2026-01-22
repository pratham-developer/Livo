package com.pratham.livo.repository;

import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Inventory;
import com.pratham.livo.entity.Room;
import com.pratham.livo.projection.PriceCheckWrapper;
import com.pratham.livo.projection.RoomAvailabilityWrapper;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory,Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Inventory i WHERE i.room = :room")
    void deleteByRoom(@Param("room") Room room);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Inventory i WHERE i.hotel = :hotel")
    void deleteByHotel(@Param("hotel") Hotel hotel);

    @Query("""
            SELECT new com.pratham.livo.projection.PriceCheckWrapper(i.hotel.id, i.room.id, SUM(i.price), COUNT(i.price))
            FROM Inventory i
            WHERE i.hotel.id IN :hotel_ids
            AND i.date BETWEEN :start_date AND :end_date
            AND i.closed = false
            AND (i.totalCount - i.bookedCount - i.reservedCount) >= :rooms_count
            GROUP BY i.hotel.id, i.room.id
            HAVING COUNT(i.date) = :date_count
            """)
    List<PriceCheckWrapper> findRoomAveragePrices(
            @Param("hotel_ids") List<Long> hotelIds,
            @Param("start_date") LocalDate startDate,
            @Param("end_date") LocalDate endDate,
            @Param("rooms_count") Integer roomsCount,
            @Param("date_count") Long dateCount
    );

    @Query("""
            SELECT DISTINCT i.hotel FROM Inventory i
            WHERE
            i.hotel.active = true AND i.hotel.deleted = false AND
            i.city = :city AND
            i.date BETWEEN :start_date AND :end_date AND
            i.closed = false AND
            (i.totalCount-i.bookedCount-i.reservedCount) >= :rooms_count
            GROUP BY i.hotel, i.room HAVING COUNT(i.date) = :date_count
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
                            i.closed = false AND
                            (i.totalCount-i.bookedCount-i.reservedCount) >= :rooms_count
                        ) = :date_count
                            THEN true ELSE false
                    END
                ) FROM Room r
                  WHERE r.hotel.id = :hotel_id
                  AND r.deleted = false
                  AND r.active = true
    """)
    List<RoomAvailabilityWrapper> findRoomsWithAvailability(
            @Param("hotel_id") Long hotelId,
            @Param("start_date") LocalDate startDate,
            @Param("end_date") LocalDate endDate,
            @Param("rooms_count") Integer roomsCount,
            @Param("date_count") Long dateCount
    );

    @Query("""
            SELECT i FROM Inventory i
            WHERE i.room.id = :room_id
            AND i.room.active = true
            AND i.room.deleted = false
            AND i.date BETWEEN :start_date AND :end_date
            AND i.closed = false
            AND (i.totalCount-i.bookedCount-i.reservedCount) >= :rooms_count
            ORDER BY i.date ASC
    """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findInventoriesForRoom(
            @Param("room_id") Long roomId,
            @Param("start_date") LocalDate startDate,
            @Param("end_date") LocalDate endDate,
            @Param("rooms_count") Integer roomsCount
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i FROM Inventory i
            WHERE i.room = :room AND
            i.date BETWEEN :startDate AND :endDate
            ORDER BY i.date ASC
            """)
    List<Inventory> findInventoriesForCleanup(@Param("room") Room room,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);


    @Query("""
            SELECT i FROM Inventory i JOIN FETCH i.room WHERE
            i.closed = false AND i.date >= :today
            AND (i.totalCount - i.bookedCount - i.reservedCount) > 0
            """)
    Page<Inventory> findInventoriesForDynamicPricing(
            @Param("today") LocalDate today,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i FROM Inventory i
            WHERE i.room.id = :roomId AND
            i.date BETWEEN :startDate AND :endDate
            ORDER BY i.date ASC
            """)
    List<Inventory> findInventoriesForBookingConfirmation(@Param("roomId") Long roomId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
}
package com.pratham.livo.repository;

import com.pratham.livo.entity.Booking;
import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Room;
import com.pratham.livo.enums.BookingStatus;
import com.pratham.livo.projection.BookingWrapper;
import com.pratham.livo.projection.HotelBookingStats;
import com.pratham.livo.projection.HotelBookingWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking,Long> {

    List<Booking> findByBookingStatusInAndUpdatedAtBefore(List<BookingStatus> statusList, LocalDateTime threshold, Pageable limit);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Booking b
            SET b.bookingStatus = :expired,
                        b.version = b.version + 1
            where b.hotel = :hotel
            AND b.bookingStatus IN :checkList
            """)
    void expireBookingsForHotel(
            @Param("hotel") Hotel hotel,
            @Param("expired") BookingStatus expired,
            @Param("checkList") List<BookingStatus> checkList
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Booking b
            SET b.bookingStatus = :expired,
                        b.version = b.version + 1
            where b.room = :room
            AND b.bookingStatus IN :checkList
            """)
    void expireBookingsForRoom(
            @Param("room") Room room,
            @Param("expired") BookingStatus expired,
            @Param("checkList") List<BookingStatus> checkList
    );


    @Query("""
            select new com.pratham.livo.projection.BookingWrapper(
            b.id,h.name,h.city,b.startDate,b.endDate,b.bookingStatus)
            from Booking b join b.hotel h
            where b.user.id = :userId and b.bookingStatus in :bookingStatusList
            """)
    Page<BookingWrapper> findMyBookings(
            @Param("userId") Long userId,
            @Param("bookingStatusList") List<BookingStatus> bookingStatusList,
            Pageable pageable
    );

    @Query("""
    SELECT b FROM Booking b LEFT JOIN FETCH b.guests
    JOIN FETCH b.hotel JOIN FETCH b.room
    WHERE b.id = :bookingId""")
    Optional<Booking> findByIdWithGuests(@Param("bookingId") Long bookingId);

    @Query("""
        select new com.pratham.livo.projection.HotelBookingWrapper(
            b.id, r.type, r.capacity, b.roomsCount, b.startDate, b.endDate, b.bookingStatus
        )
        from Booking b join b.room r
        where b.hotel.id = :hotelId
        and b.bookingStatus in :bookingStatusList
        AND b.startDate >= COALESCE(:fromDate, b.startDate)
        AND b.startDate <= COALESCE(:toDate, b.startDate)
        """)
    Page<HotelBookingWrapper> findBookingsForHotel(
            @Param("hotelId") Long hotelId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("bookingStatusList") List<BookingStatus> statusList,
            Pageable pageable);

    @Query("""
    SELECT new com.pratham.livo.projection.HotelBookingStats(
        SUM(CASE WHEN b.bookingStatus = :confirmed THEN 1L ELSE 0L END),
        SUM(CASE WHEN b.bookingStatus = :confirmed THEN b.amount END),
        SUM(CASE WHEN b.bookingStatus = :cancelled THEN 1L ELSE 0L END),
        SUM(CASE WHEN b.bookingStatus = :cancelled THEN b.amount END)
    )
    FROM Booking b
    WHERE b.hotel.id = :hotelId
    AND b.startDate >= COALESCE(:fromDate, b.startDate)
    AND b.startDate <= COALESCE(:toDate, b.startDate)
    """)
    HotelBookingStats findBookingStats(
            @Param("hotelId") Long hotelId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("confirmed") BookingStatus confirmed,
            @Param("cancelled") BookingStatus cancelled
    );
}

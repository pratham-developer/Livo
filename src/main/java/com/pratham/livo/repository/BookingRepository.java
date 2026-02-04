package com.pratham.livo.repository;

import com.pratham.livo.entity.Booking;
import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Room;
import com.pratham.livo.enums.BookingStatus;
import com.pratham.livo.projection.BookingWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking,Long> {

    List<Booking> findByBookingStatusInAndUpdatedAtBefore(List<BookingStatus> statusList, LocalDateTime threshold, Pageable limit);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Booking b
            SET b.bookingStatus = 'EXPIRED',
                        b.version = b.version + 1
            where b.hotel = :hotel
            AND b.bookingStatus IN ('RESERVED', 'GUESTS_ADDED', 'PAYMENT_PENDING')
            """)
    void expireBookingsForHotel(@Param("hotel") Hotel hotel);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Booking b
            SET b.bookingStatus = 'EXPIRED',
                        b.version = b.version + 1
            where b.room = :room
            AND b.bookingStatus IN ('RESERVED', 'GUESTS_ADDED', 'PAYMENT_PENDING')
            """)
    void expireBookingsForRoom(@Param("room") Room room);


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
}

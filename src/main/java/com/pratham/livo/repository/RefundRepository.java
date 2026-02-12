package com.pratham.livo.repository;

import com.pratham.livo.entity.Refund;
import com.pratham.livo.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund,Long> {

    @Query("""
    SELECT SUM(r.amount)
    FROM Refund r
    JOIN r.payment p
    JOIN p.booking b
    WHERE b.hotel.id = :hotelId
    AND b.bookingStatus = :cancelled
    AND b.startDate >= COALESCE(:fromDate, b.startDate)
    AND b.startDate <= COALESCE(:toDate, b.startDate)
    """)
    BigDecimal findTotalAmountRefunded(
            @Param("hotelId") Long hotelId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("cancelled")BookingStatus cancelled
            );

    @Query("""
        select r.refundStatus from Refund r
        join r.payment p join p.booking b
        where b.id = :bookingId and b.bookingStatus = :cancelled
    """)
    String findRefundStatus(
                             @Param("bookingId") Long bookingId,
                             @Param("cancelled") BookingStatus cancelled
    );

    @Query("""
    SELECT r FROM Refund r
    JOIN FETCH r.payment p
    JOIN FETCH p.booking b
    JOIN FETCH b.user
    JOIN FETCH b.hotel
    WHERE r.razorpayRefundId = :razorpayRefundId
""")
    Optional<Refund> findByRazorpayRefundId(@Param("razorpayRefundId") String razorpayRefundId);
}

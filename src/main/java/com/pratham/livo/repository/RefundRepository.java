package com.pratham.livo.repository;

import com.pratham.livo.entity.Refund;
import com.pratham.livo.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface RefundRepository extends JpaRepository<Refund,Long> {

    @Modifying
    @Query("update Refund set refundStatus = :status where razorpayRefundId = :razorpayRefundId")
    void updateStatus(@Param("razorpayRefundId") String razorpayRefundId,
                      @Param("status") String status);

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
}

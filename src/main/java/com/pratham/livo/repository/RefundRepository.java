package com.pratham.livo.repository;

import com.pratham.livo.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundRepository extends JpaRepository<Refund,Long> {

    @Modifying
    @Query("update Refund set refundStatus = :status where razorpayRefundId = :razorpayRefundId")
    void updateStatus(@Param("razorpayRefundId") String razorpayRefundId,
                      @Param("status") String status);
}

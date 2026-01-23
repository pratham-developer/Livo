package com.pratham.livo.repository;

import com.pratham.livo.entity.RazorpayEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RazorpayEventRepository extends JpaRepository<RazorpayEvent,String> {

    @Modifying
    @Query(value = """
        INSERT INTO razorpay_event (event_id, created_at)
        VALUES (:eventId, NOW())
        ON CONFLICT (event_id) DO NOTHING
    """, nativeQuery = true)
    int storeEvent(@Param("eventId") String eventId);
}

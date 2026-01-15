package com.pratham.livo.repository;

import com.pratham.livo.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GuestRepository extends JpaRepository<Guest,Long>{
    @Modifying
    @Query("DELETE FROM Guest g WHERE g.booking.id = :bookingId")
    void deleteByBookingId(Long bookingId);
}

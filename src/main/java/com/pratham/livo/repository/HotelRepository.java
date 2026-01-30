package com.pratham.livo.repository;

import com.pratham.livo.entity.Hotel;
import com.pratham.livo.projection.HotelWrapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel,Long> {
    long countByOwnerIdAndDeletedFalse(Long id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE hotel
            SET popularity_score = (
                SELECT COUNT(*) FROM booking b
                WHERE b.hotel_id = hotel.id
                AND b.booking_status = :booking_status
                AND b.created_at >= NOW() - INTERVAL '30 DAY'
            )
            WHERE active = true AND deleted = false
            """, nativeQuery = true)
    void updatePopularityOfActiveHotels(@Param("booking_status") String bookingStatus);


    @Query("""
            select new com.pratham.livo.projection.HotelWrapper(
            h.id, h.name, h.city, h.photos) from Hotel h
            where h.active = true and h.deleted = false
            order by h.popularityScore desc
            """)
    List<HotelWrapper> findBestHotels(Pageable pageable);
}

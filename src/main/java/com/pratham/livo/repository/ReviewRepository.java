package com.pratham.livo.repository;

import com.pratham.livo.entity.Review;
import com.pratham.livo.projection.RatingCountProjection;
import com.pratham.livo.projection.ReviewSummaryProjection;
import com.pratham.livo.projection.ReviewWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByBookingId(Long bookingId);
    Optional<Review> findByBookingId(Long bookingId);

    @Query("SELECT r FROM Review r WHERE r.processed = false")
    List<Review> findUnprocessedReviews(Pageable pageable);

    @Modifying
    @Query("UPDATE Review r SET r.processed = true WHERE r.id IN :reviewIds")
    void markAsProcessed(List<Long> reviewIds);

    @Query(value = "SELECT new com.pratham.livo.projection.ReviewWrapper(r.rating, r.text, u.name) " +
            "FROM Review r JOIN r.booking b JOIN b.user u WHERE r.hotel.id = :hotelId",
            countQuery = "SELECT COUNT(r) FROM Review r WHERE r.hotel.id = :hotelId")
    Page<ReviewWrapper> findByHotelIdWithUser(@Param("hotelId") Long hotelId, Pageable pageable);

    @Query("SELECT r.rating as rating, COUNT(r) as count FROM Review r WHERE r.hotel.id = :hotelId GROUP BY r.rating")
    List<RatingCountProjection> getRatingDistribution(@Param("hotelId") Long hotelId);

    @Query(nativeQuery = true, value = """
        (SELECT rating, text FROM review WHERE hotel_id = :hotelId AND rating = 5 ORDER BY created_at DESC LIMIT :limit5)
        UNION ALL
        (SELECT rating, text FROM review WHERE hotel_id = :hotelId AND rating = 4 ORDER BY created_at DESC LIMIT :limit4)
        UNION ALL
        (SELECT rating, text FROM review WHERE hotel_id = :hotelId AND rating = 3 ORDER BY created_at DESC LIMIT :limit3)
        UNION ALL
        (SELECT rating, text FROM review WHERE hotel_id = :hotelId AND rating = 2 ORDER BY created_at DESC LIMIT :limit2)
        UNION ALL
        (SELECT rating, text FROM review WHERE hotel_id = :hotelId AND rating = 1 ORDER BY created_at DESC LIMIT :limit1)
    """)
    List<ReviewSummaryProjection> fetchStratifiedSample(
            @Param("hotelId") Long hotelId,
            @Param("limit5") int limit5,
            @Param("limit4") int limit4,
            @Param("limit3") int limit3,
            @Param("limit2") int limit2,
            @Param("limit1") int limit1
    );
}

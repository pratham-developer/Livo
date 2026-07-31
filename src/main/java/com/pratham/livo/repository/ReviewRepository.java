package com.pratham.livo.repository;

import com.pratham.livo.entity.Review;
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
}

package com.pratham.livo.service.cron;

import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Review;
import com.pratham.livo.repository.HotelRepository;
import com.pratham.livo.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelRatingCronService {

    private final ReviewRepository reviewRepository;
    private final HotelRepository hotelRepository;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(cron = "0 0 2 * * *") // Runs at 2:00 AM every day
    @SchedulerLock(
            name = "processHotelRatingsTask",
            lockAtLeastFor = "PT2M",
            lockAtMostFor = "PT15M"
    )
    public void processHotelRatings() {
        log.info("Starting Nightly Hotel Rating Processing Job");
        long start = System.currentTimeMillis();

        boolean hasMore = true;
        int totalProcessed = 0;

        while (hasMore) {
            // Process in batches of 500 to protect RAM
            Integer batchCount = transactionTemplate.execute(status -> {
                Pageable limit = PageRequest.of(0, 500);
                List<Review> unprocessedReviews = reviewRepository.findUnprocessedReviews(limit);

                if (unprocessedReviews.isEmpty()) {
                    return 0;
                }

                // 1. Group reviews by Hotel ID in memory
                Map<Long, List<Review>> reviewsByHotel = unprocessedReviews.stream()
                        .collect(Collectors.groupingBy(review -> review.getHotel().getId()));

                List<Hotel> hotelsToUpdate = new ArrayList<>();
                List<Long> processedReviewIds = new ArrayList<>();

                // 2. O(1) Math for each hotel
                for (Map.Entry<Long, List<Review>> entry : reviewsByHotel.entrySet()) {
                    Long hotelId = entry.getKey();
                    List<Review> hotelReviews = entry.getValue();

                    Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
                    if (hotel == null) continue;

                    // Safely handle null values for brand new hotels
                    long currentReviewCount = hotel.getReviewCount() != null ? hotel.getReviewCount() : 0L;
                    long currentRatingSum = hotel.getRatingSum() != null ? hotel.getRatingSum() : 0L;

                    long newReviewCount = currentReviewCount + hotelReviews.size();

                    long sumOfNewRatings = hotelReviews.stream().mapToInt(Review::getRating).sum();
                    long newRatingSum = currentRatingSum + sumOfNewRatings;

                    // Calculate average: (Sum / Count) -> Scale to 2 decimal places
                    BigDecimal newAverage = BigDecimal.valueOf(newRatingSum)
                            .divide(BigDecimal.valueOf(newReviewCount), 2, RoundingMode.HALF_UP);

                    // Update Hotel
                    hotel.setReviewCount(newReviewCount);
                    hotel.setRatingSum(newRatingSum);
                    hotel.setAverageRating(newAverage);

                    hotelsToUpdate.add(hotel);

                    // Collect review IDs to mark them as processed
                    hotelReviews.forEach(r -> processedReviewIds.add(r.getId()));
                }

                // 3. Bulk Save
                hotelRepository.saveAll(hotelsToUpdate);
                reviewRepository.markAsProcessed(processedReviewIds);

                return unprocessedReviews.size();
            });

            if (batchCount == null || batchCount == 0) {
                hasMore = false;
            } else {
                totalProcessed += batchCount;
            }
        }

        log.info("Nightly Rating Job Finished. Processed {} reviews in {}ms",
                totalProcessed, System.currentTimeMillis() - start);
    }
}
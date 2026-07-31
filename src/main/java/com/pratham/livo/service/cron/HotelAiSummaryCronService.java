package com.pratham.livo.service.cron;

import com.pratham.livo.projection.RatingCountProjection;
import com.pratham.livo.projection.ReviewSummaryProjection;
import com.pratham.livo.repository.HotelRepository;
import com.pratham.livo.repository.ReviewRepository;
import com.pratham.livo.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelAiSummaryCronService {

    private final HotelRepository hotelRepository;
    private final ReviewRepository reviewRepository;
    private final LlmService llmService;
    private final HotelSummaryPersistHelper persistHelper;

    private static final int MAX_SAMPLE_SIZE = 500;
    private static final int DAYS_BEFORE_REFRESH = 15;
    private static final int BATCH_SIZE = 10;

    // If the LLM is unreachable (dead API key, OpenAI outage), every hotel fails
    // in a row. Abort the run rather than burn quota/time on doomed calls; the
    // untouched hotels are naturally retried next run.
    private static final int MAX_CONSECUTIVE_FAILURES = 10;

    private static final String SYSTEM_PROMPT =
            "You are writing a short, honest overview to help a traveler decide whether to book this hotel. " +
                    "You are given a representative sample of recent guest reviews, each tagged with its star rating. " +
                    "Write exactly 3 sentences, in a warm but neutral tone, addressed to a prospective guest.\n" +
                    "Base every statement strictly on the reviews provided — do not invent details, amenities, or specifics that are not mentioned. " +
                    "Do not mention review counts, ratings, star numbers, sampling, or that you were given data; write as if summarizing what past guests generally felt.\n" +
                    "Cover both what guests consistently liked and what they consistently complained about, so the reader gets a balanced picture. " +
                    "Prioritize recurring themes over one-off remarks, and name concrete specifics (e.g. location, cleanliness, staff, breakfast, noise) rather than vague praise. " +
                    "Never mention specific guest names.";

    @Scheduled(cron = "0 0 4 * * *") // 4:00 AM daily
    @SchedulerLock(name = "aiSummarizerTask", lockAtLeastFor = "PT2M", lockAtMostFor = "PT30M")
    public void generateAiSummaries() {
        log.info("Starting AI Summarizer Cron Job");
        LocalDateTime threshold = LocalDateTime.now().minusDays(DAYS_BEFORE_REFRESH);

        int processed = 0, skipped = 0, failed = 0;
        int consecutiveFailures = 0;
        Long lastId = 0L; // keyset cursor — a failing hotel never blocks the batch

        outer:
        while (true) {
            List<Long> hotelIds = hotelRepository.findHotelIdsNeedingSummaryUpdate(
                    threshold, lastId, PageRequest.of(0, BATCH_SIZE));
            if (hotelIds.isEmpty()) break;

            for (Long hotelId : hotelIds) {
                lastId = hotelId; // advance regardless of outcome
                try {
                    String summary = computeSummary(hotelId); // slow, no transaction
                    if (summary == null) {
                        persistHelper.touchTimestamp(hotelId); // short transaction
                        skipped++;
                    } else {
                        persistHelper.applySummary(hotelId, summary); // short transaction
                        processed++;
                    }
                    consecutiveFailures = 0; // any success clears the streak
                } catch (Exception e) {
                    failed++;
                    log.error("Failed to generate/persist AI summary for Hotel ID: {}", hotelId, e);
                    // No stamp on failure — naturally retried next run.
                    if (++consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        log.error("Aborting run after {} consecutive failures — LLM likely unreachable",
                                consecutiveFailures);
                        break outer;
                    }
                }
            }
        }

        log.info("Finished AI Summarizer Cron Job. processed={}, skipped={}, failed={}",
                processed, skipped, failed);
    }

    // -- pure compute: reads projections, calls the LLM, returns a string --
    private String computeSummary(Long hotelId) {
        List<RatingCountProjection> distribution = reviewRepository.getRatingDistribution(hotelId);
        if (distribution.isEmpty()) return null;

        Map<Integer, Integer> limits = calculateStratifiedLimits(distribution);

        List<ReviewSummaryProjection> sample = reviewRepository.fetchStratifiedSample(
                hotelId,
                limits.getOrDefault(5, 0),
                limits.getOrDefault(4, 0),
                limits.getOrDefault(3, 0),
                limits.getOrDefault(2, 0),
                limits.getOrDefault(1, 0));
        if (sample.isEmpty()) return null;

        StringBuilder payload = new StringBuilder(sample.size() * 128);
        for (ReviewSummaryProjection review : sample) {
            payload.append("[RATING: ").append(review.getRating()).append(" Stars] ")
                    .append(review.getText()).append('\n');
        }

        // LlmServiceImpl guarantees non-blank output or throws.
        return llmService.generateSummary(SYSTEM_PROMPT, payload.toString());
    }

    // -- stratified sampling: largest-remainder guarantees exact MAX_SAMPLE_SIZE --
    private Map<Integer, Integer> calculateStratifiedLimits(List<RatingCountProjection> distribution) {
        int total = distribution.stream().mapToInt(d -> d.getCount().intValue()).sum();
        Map<Integer, Integer> limits = new HashMap<>();

        if (total <= MAX_SAMPLE_SIZE) {
            distribution.forEach(d -> limits.put(d.getRating(), d.getCount().intValue()));
            return limits;
        }

        int allocated = 0;
        List<double[]> remainders = new ArrayList<>(); // [rating, fractionalPart]
        for (RatingCountProjection d : distribution) {
            double exact = (double) d.getCount().intValue() / total * MAX_SAMPLE_SIZE;
            int floor = (int) Math.floor(exact);
            limits.put(d.getRating(), floor);
            allocated += floor;
            remainders.add(new double[]{d.getRating(), exact - floor});
        }

        int remaining = MAX_SAMPLE_SIZE - allocated;
        remainders.sort((a, b) -> Double.compare(b[1], a[1]));
        for (int i = 0; i < remaining && i < remainders.size(); i++) {
            int rating = (int) remainders.get(i)[0];
            limits.merge(rating, 1, Integer::sum);
        }
        return limits;
    }
}
package com.pratham.livo.service.cron;

import com.pratham.livo.entity.Hotel;
import com.pratham.livo.repository.HotelRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HotelSummaryPersistHelper {

    private final HotelRepository hotelRepository;

    // Short transaction: load → mutate → flush at commit.
    // Managed entity, no detached merge, real @Version check.
    @Transactional
    public void applySummary(Long hotelId, String summary) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new EntityNotFoundException("Hotel not found: " + hotelId));
        hotel.setAiSummary(summary);
        hotel.setLastSummaryUpdatedAt(LocalDateTime.now());
        // no save() — dirty checking handles the UPDATE on commit
    }

    // Stamp hotels with no reviews/sample so they aren't rescanned nightly.
    @Transactional
    public void touchTimestamp(Long hotelId) {
        hotelRepository.findById(hotelId)
                .ifPresent(h -> h.setLastSummaryUpdatedAt(LocalDateTime.now()));
    }
}
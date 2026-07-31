package com.pratham.livo.controller;

import com.pratham.livo.dto.booking.*;
import com.pratham.livo.dto.review.ReviewRequestDto;
import com.pratham.livo.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
@Validated //required to validate List<AddGuestDto> in request body
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/init")
    public ResponseEntity<BookingResponseDto> initBooking(@Valid @RequestBody BookingRequestDto bookingRequestDto){
        log.info("Attempting to create booking with: {}", bookingRequestDto);
        return ResponseEntity.ok(bookingService.initBooking(bookingRequestDto));
    }

    @PostMapping("/{bookingId}/addGuests")
    public ResponseEntity<BookingResponseDto> addGuests(
            @PathVariable Long bookingId,
            @RequestBody List<@Valid AddGuestDto> guestDtoList //validates each item in list
    ){
        log.info("Attempting to add guests to booking with id: {}",bookingId);
        return ResponseEntity.ok(bookingService.addGuests(bookingId,guestDtoList));
    }
    
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponseDto> cancelBooking(@PathVariable Long bookingId){
        log.info("Attempting to cancel booking with id: {}",bookingId);
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }

    @GetMapping
    public ResponseEntity<PagedModel<BookingWrapperDto>> getMyBookings(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ){
        log.info("Attempting to get bookings for a user");
        return ResponseEntity.ok(bookingService.getMyBookings(page,Math.min(size,100)));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponseDto> getBookingById(
            @PathVariable Long bookingId){
        log.info("Attempting to get booking with id: {}",bookingId);
        return ResponseEntity.ok(bookingService.getBookingById(bookingId));
    }

    @PostMapping("/review")
    public ResponseEntity<Map<String, String>> addReview(
            @Valid @RequestBody ReviewRequestDto reviewRequestDto) {
        log.info("Attempting to add review to booking with id: {}",
                reviewRequestDto.getBookingId());
        bookingService.addReview(reviewRequestDto);
        return ResponseEntity.ok(
                Map.of("message", "Review added successfully")
        );
    }
}
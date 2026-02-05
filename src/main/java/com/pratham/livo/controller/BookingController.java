package com.pratham.livo.controller;

import com.pratham.livo.dto.booking.AddGuestDto;
import com.pratham.livo.dto.booking.BookingRequestDto;
import com.pratham.livo.dto.booking.BookingResponseDto;
import com.pratham.livo.dto.booking.BookingWrapperDto;
import com.pratham.livo.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/init")
    public ResponseEntity<BookingResponseDto> initBooking(@RequestBody BookingRequestDto bookingRequestDto){
        log.info("Attempting to create booking with: {}", bookingRequestDto);
        return ResponseEntity.ok(bookingService.initBooking(bookingRequestDto));
    }

    @PostMapping("/{bookingId}/addGuests")
    public ResponseEntity<BookingResponseDto> addGuests(@PathVariable Long bookingId, @RequestBody List<AddGuestDto> guestDtoList){
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

}

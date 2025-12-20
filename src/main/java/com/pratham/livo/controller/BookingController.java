package com.pratham.livo.controller;

import com.pratham.livo.dto.booking.AddGuestDto;
import com.pratham.livo.dto.booking.BookingRequestDto;
import com.pratham.livo.dto.booking.BookingResponseDto;
import com.pratham.livo.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}

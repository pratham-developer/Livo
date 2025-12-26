package com.pratham.livo.service;

import com.pratham.livo.dto.booking.AddGuestDto;
import com.pratham.livo.dto.booking.BookingResponseDto;
import com.pratham.livo.dto.booking.BookingRequestDto;

import java.util.List;

public interface BookingService {
    BookingResponseDto initBooking(BookingRequestDto bookingRequestDto);
    BookingResponseDto addGuests(Long bookingId, List<AddGuestDto> guestDtoList);
    void cleanExpiredBookings();

    //TODO: implement dynamic pricing strategy using decorator pattern

    /*
    TODO:
    1. whenever we open the homepage, an endpoint hits to fetch the last pending booking with option to continue it
    2. when we click on book button, it should first try to continue an already existing book for the same user,
    for the same room and between the same dates instead of creating a new booking.
    3. If a user creates a new Booking B while Booking A is still pending, cancel Booking A immediately (release inventory).
    Then Create Booking B.

    The Hybrid Logic Flow:

       - User clicks Book (Room B).
       - Check: Does this user have any pending booking?
           (i) No: Create Booking for Room B.
           (ii) Yes (Room B, Same Dates): Return existing Booking (Resume).
           (iii) Yes (Room A OR Different Dates): Cancel old booking (Release Inventory)
                 Create Booking for Room B (Replace).
     */

}

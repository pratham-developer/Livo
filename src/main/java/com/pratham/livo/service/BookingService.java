package com.pratham.livo.service;

import com.pratham.livo.dto.booking.AddGuestDto;
import com.pratham.livo.dto.booking.BookingResponseDto;
import com.pratham.livo.dto.booking.BookingRequestDto;
import com.pratham.livo.dto.booking.BookingWrapperDto;
import com.pratham.livo.dto.review.ReviewRequestDto;
import org.springframework.data.web.PagedModel;

import java.util.List;

public interface BookingService {
    BookingResponseDto initBooking(BookingRequestDto bookingRequestDto);
    BookingResponseDto addGuests(Long bookingId, List<AddGuestDto> guestDtoList);
    void cleanExpiredBookings();
    BookingResponseDto cancelBooking(Long bookingId);
    PagedModel<BookingWrapperDto> getMyBookings(Integer page, Integer size);
    BookingResponseDto getBookingById(Long bookingId);
    void addReview(ReviewRequestDto reviewRequestDto);
}

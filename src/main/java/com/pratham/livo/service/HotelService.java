package com.pratham.livo.service;

import com.pratham.livo.dto.hotel.HotelRequestDto;
import com.pratham.livo.dto.hotel.HotelResponseDto;

public interface HotelService {
    HotelResponseDto createHotel(HotelRequestDto hotelRequestDto);
    HotelResponseDto getHotelById(Long id);
    HotelResponseDto updateHotelById(Long id, HotelRequestDto hotelRequestDto);
    void deleteHotelById(Long id);
    void activateHotelById(Long id);
}

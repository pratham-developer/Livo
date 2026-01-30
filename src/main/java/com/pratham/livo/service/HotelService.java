package com.pratham.livo.service;

import com.pratham.livo.dto.hotel.*;
import org.springframework.data.web.PagedModel;

import java.time.LocalDate;
import java.util.List;

public interface HotelService {
    HotelResponseDto createHotel(HotelRequestDto hotelRequestDto);
    HotelResponseDto getHotelById(Long id);
    HotelResponseDto updateHotelById(Long id, HotelRequestDto hotelRequestDto);
    void deleteHotelById(Long id);
    void activateHotelById(Long id);
    PagedModel<HotelSearchResponseDto> searchHotels(HotelSearchRequestDto hotelSearchRequestDto, Integer page, Integer size);
    HotelInfoDto getHotelInfo(Long id, LocalDate startDate, LocalDate endDate, Integer roomsCount);
    void updatePopularityOfActiveHotels();
    List<BestHotelsResponseDto> getBestHotels();
}

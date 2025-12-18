package com.pratham.livo.controller;

import com.pratham.livo.dto.hotel.HotelInfoDto;
import com.pratham.livo.dto.hotel.HotelResponseDto;
import com.pratham.livo.dto.hotel.HotelSearchRequestDto;
import com.pratham.livo.entity.Hotel;
import com.pratham.livo.service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@RestController
@RequestMapping("/hotels")
@RequiredArgsConstructor
@Slf4j
public class HotelBrowseController {

    private final HotelService hotelService;

    //logically should be a get request
    //but frontend clients strip body from get requests sometimes
    //so we make it a post request
    @PostMapping("/search")
    public ResponseEntity<PagedModel<HotelResponseDto>> findHotels(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestBody HotelSearchRequestDto hotelSearchRequestDto){

        log.info("Attempting to fetch hotels with request: {}",hotelSearchRequestDto);
        return ResponseEntity.ok(hotelService.searchHotels(
                hotelSearchRequestDto,page,size
        ));
    }

    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelInfoDto> getHotelInfo(
            @PathVariable Long hotelId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "1") Integer roomsCount
            ){
        log.info("Attempting to fetch hotel info with id: {}",hotelId);
        return ResponseEntity.ok(hotelService.getHotelInfo(hotelId,startDate,endDate,roomsCount));
    }
}

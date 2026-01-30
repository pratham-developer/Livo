package com.pratham.livo.controller;

import com.pratham.livo.dto.hotel.BestHotelsResponseDto;
import com.pratham.livo.dto.hotel.HotelInfoDto;
import com.pratham.livo.dto.hotel.HotelSearchRequestDto;
import com.pratham.livo.dto.hotel.HotelSearchResponseDto;
import com.pratham.livo.service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


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
    public ResponseEntity<PagedModel<HotelSearchResponseDto>> findHotels(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestBody HotelSearchRequestDto hotelSearchRequestDto){


        log.info("Attempting to fetch hotels with request: {}",hotelSearchRequestDto);
        //limit the page size to 100 to prevent attacks
        return ResponseEntity.ok(hotelService.searchHotels(
                hotelSearchRequestDto,page,Math.min(size,100)
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

    @GetMapping("/best")
    public ResponseEntity<List<BestHotelsResponseDto>> getBestHotels(
    ){
        log.info("Attempting to fetch best hotels");
        return ResponseEntity.ok(hotelService.getBestHotels());
    }
}

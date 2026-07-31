package com.pratham.livo.controller;

import com.pratham.livo.dto.hotel.*;
import com.pratham.livo.dto.review.ReviewDto;
import com.pratham.livo.service.HotelService;
import jakarta.validation.Valid;
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

    @PostMapping("/search")
    public ResponseEntity<PagedModel<HotelSearchResponseDto>> findHotels(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @Valid @RequestBody HotelSearchRequestDto hotelSearchRequestDto){

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


    //most popular hotels
    @GetMapping("/best")
    public ResponseEntity<List<HotelResponseDto>> getBestHotels(
    ){
        log.info("Attempting to fetch best hotels");
        return ResponseEntity.ok(hotelService.getBestHotels());
    }

    @GetMapping("/{hotelId}/reviews")
    public ResponseEntity<PagedModel<ReviewDto>> getHotelReviews(
            @PathVariable Long hotelId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("Attempting to get reviews for hotel id: {}", hotelId);
        // Cap the max size at 50 to prevent frontend from requesting massive payloads
        return ResponseEntity.ok(hotelService.getHotelReviews(hotelId, page, Math.min(size, 50)));
    }
}
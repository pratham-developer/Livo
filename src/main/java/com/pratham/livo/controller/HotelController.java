package com.pratham.livo.controller;

import com.pratham.livo.dto.hotel.HotelRequestDto;
import com.pratham.livo.dto.hotel.HotelResponseDto;
import com.pratham.livo.service.HotelService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/hotels")
@AllArgsConstructor
@Slf4j
public class HotelController  {

    private final HotelService hotelService;

    @PostMapping
    ResponseEntity<HotelResponseDto> createNewHotel(@RequestBody HotelRequestDto hotelRequestDto){
        log.info("Attempting to create hotel with name: {}",hotelRequestDto.getName());
        HotelResponseDto hotelResponseDto = hotelService.createHotel(hotelRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(hotelResponseDto);
    }

    @GetMapping("/{hotelId}")
    ResponseEntity<HotelResponseDto> getHotelById(@PathVariable Long hotelId){
        HotelResponseDto hotelResponseDto = hotelService.getHotelById(hotelId);
        return ResponseEntity.ok(hotelResponseDto);
    }

    @PutMapping("/{hotelId}")
    public ResponseEntity<HotelResponseDto> updateHotel(@PathVariable Long hotelId, @RequestBody HotelRequestDto hotelRequestDto){
        log.info("Attempting to update hotel with id: {}",hotelId);
        HotelResponseDto hotelResponseDto = hotelService.updateHotelById(hotelId, hotelRequestDto);
        return ResponseEntity.ok(hotelResponseDto);
    }

    @DeleteMapping("/{hotelId}")
    public ResponseEntity<Void> deleteHotelById(@PathVariable Long hotelId){
        log.info("Attempting to delete hotel with id: {}",hotelId);
        hotelService.deleteHotelById(hotelId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{hotelId}")
    public ResponseEntity<Void> activateHotelById(@PathVariable Long hotelId){
        log.info("Attempting to activate hotel with id: {}",hotelId);
        hotelService.activateHotelById(hotelId);
        return ResponseEntity.noContent().build();
    }
}

package com.pratham.livo.controller;

import com.pratham.livo.dto.room.RoomRequestDto;
import com.pratham.livo.dto.room.RoomResponseDto;
import com.pratham.livo.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/rooms")
@RequiredArgsConstructor
@Slf4j
public class AdminRoomController {

    private final RoomService roomService;

    @PostMapping("/hotel/{hotelId}")
    public ResponseEntity<RoomResponseDto> createNewRoom(@PathVariable Long hotelId, @Valid @RequestBody RoomRequestDto roomRequestDto){
        log.info("Attempting to create room in hotel(id={}) with type: {}",hotelId, roomRequestDto.getType());
        RoomResponseDto roomResponseDto = roomService.createNewRoomInHotel(hotelId, roomRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(roomResponseDto);
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<RoomResponseDto>> getAllRoomsInHotel(@PathVariable Long hotelId){
        log.info("Attempting to fetch all rooms for hotelId={}", hotelId);
        return ResponseEntity.ok(roomService.getAllRoomsInHotel(hotelId));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponseDto> getRoomById(@PathVariable Long roomId){
        log.info("Attempting to fetch room with id: {}", roomId);
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoomById(@PathVariable Long roomId){
        log.info("Attempting to delete room with id: {}", roomId);
        roomService.deleteRoomById(roomId);
        return ResponseEntity.noContent().build();
    }
}
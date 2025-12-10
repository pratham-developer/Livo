package com.pratham.livo.service;

import com.pratham.livo.dto.room.RoomRequestDto;
import com.pratham.livo.dto.room.RoomResponseDto;

import java.util.List;

public interface RoomService {
    RoomResponseDto createNewRoomInHotel(Long hotelId, RoomRequestDto roomRequestDto);
    List<RoomResponseDto> getAllRoomsInHotel(Long hotelId);
    RoomResponseDto getRoomById(Long roomId);
    void deleteRoomById(Long roomId);
}

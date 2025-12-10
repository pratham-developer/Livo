package com.pratham.livo.service.impl;

import com.pratham.livo.dto.room.RoomRequestDto;
import com.pratham.livo.dto.room.RoomResponseDto;
import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Room;
import com.pratham.livo.exception.ResourceNotFoundException;
import com.pratham.livo.repository.HotelRepository;
import com.pratham.livo.repository.InventoryRepository;
import com.pratham.livo.repository.RoomRepository;
import com.pratham.livo.service.InventoryService;
import com.pratham.livo.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final ModelMapper modelMapper;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public RoomResponseDto createNewRoomInHotel(Long hotelId, RoomRequestDto roomRequestDto) {
        log.info("Creating room in hotel(id={}) with type: {}",hotelId, roomRequestDto.getType());
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(
                ()->new ResourceNotFoundException("Hotel Not Found with id: "+hotelId)
        );
        Room room = modelMapper.map(roomRequestDto, Room.class);
        room.setHotel(hotel);
        Room savedRoom = roomRepository.save(room);
        log.info("Room created in hotel(id={}) with type: {}",hotelId, roomRequestDto.getType());

        //create inventory for this room after creation if hotel is active
        if(hotel.getActive()){
            inventoryService.initRoomFor1Year(savedRoom);
        }
        return modelMapper.map(savedRoom, RoomResponseDto.class);
    }

    @Override
    public List<RoomResponseDto> getAllRoomsInHotel(Long hotelId) {
        log.info("Fetching all rooms for hotelId={}", hotelId);
        if(!hotelRepository.existsById(hotelId)){
            throw new ResourceNotFoundException("Hotel not found with id: " + hotelId);
        }
        List<Room> rooms = roomRepository.findByHotel_Id(hotelId);
        log.info("Found {} rooms for hotelId={}", rooms.size(), hotelId);
        return rooms.stream().map(room -> modelMapper.map(room, RoomResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public RoomResponseDto getRoomById(Long roomId) {
        log.info("Getting room with id: {}", roomId);
        Room room = roomRepository.findById(roomId).orElseThrow(
                ()->new ResourceNotFoundException("Room Not Found with id: "+roomId)
        );
        log.info("Retrieved room with id: {}", roomId);
        return modelMapper.map(room, RoomResponseDto.class);
    }

    @Override
    @Transactional
    public void deleteRoomById(Long roomId) {
        log.info("Deleting room with id: {}", roomId);
        Room room = roomRepository.findById(roomId).orElseThrow(
                ()->new ResourceNotFoundException("Room Not Found with id: "+roomId)
        );

        //delete inventories of this room
        log.info("Deleting inventories of room with id: {}", roomId);
        inventoryRepository.deleteByRoom(room);
        log.info("Deleted inventories room with id: {}", roomId);

        //delete room
        roomRepository.delete(room);
        log.info("Deleted room with id: {}", roomId);
    }
}

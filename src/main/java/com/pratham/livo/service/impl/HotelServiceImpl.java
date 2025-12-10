package com.pratham.livo.service.impl;

import com.pratham.livo.dto.hotel.HotelRequestDto;
import com.pratham.livo.dto.hotel.HotelResponseDto;
import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Room;
import com.pratham.livo.exception.ResourceNotFoundException;
import com.pratham.livo.repository.HotelRepository;
import com.pratham.livo.repository.InventoryRepository;
import com.pratham.livo.repository.RoomRepository;
import com.pratham.livo.service.HotelService;
import com.pratham.livo.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelServiceImpl implements HotelService {

    private final ModelMapper modelMapper;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public HotelResponseDto createHotel(HotelRequestDto hotelRequestDto) {
        log.info("Creating hotel with name: {}",hotelRequestDto.getName());
        Hotel hotel = modelMapper.map(hotelRequestDto,Hotel.class);
        //set active = false on creation
        hotel.setActive(false);
        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("Hotel created with id: {}",savedHotel.getId());
        return modelMapper.map(savedHotel,HotelResponseDto.class);
    }

    @Override
    public HotelResponseDto getHotelById(Long id) {
        log.info("Getting hotel with id: {}",id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(()->
                new ResourceNotFoundException("Hotel Not Found with id: "+id));
        log.info("Hotel retrieved with id: {}",hotel.getId());
        return modelMapper.map(hotel, HotelResponseDto.class);
    }

    @Override
    @Transactional
    public HotelResponseDto updateHotelById(Long id, HotelRequestDto hotelRequestDto) {
        log.info("Updating hotel with id: {}",id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(()->
                new ResourceNotFoundException("Hotel Not Found with id: "+id));
        hotelRequestDto.setActive(hotel.getActive());
        modelMapper.map(hotelRequestDto,hotel);
        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("Hotel updated with id: {}",id);
        return modelMapper.map(savedHotel,HotelResponseDto.class);
    }

    @Override
    @Transactional
    public void deleteHotelById(Long id){
        log.info("Deleting hotel with id: {}",id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found with id: "+id));

        //delete inventories of this hotel
        log.info("Deleting inventories of hotel with id: {}",id);
        inventoryRepository.deleteByHotel(hotel);
        log.info("Inventories deleted of hotel with id: {}",id);

        log.info("Deleting rooms of hotel with id: {}",id);
        roomRepository.deleteByHotel(hotel);
        log.info("Deleted rooms of hotel with id: {}",id);

        //delete hotel
        hotelRepository.delete(hotel);
        log.info("Hotel deleted with id: {}",id);


    }

    @Override
    @Transactional
    public void activateHotelById(Long id) {
        log.info("Activate hotel with id: {}",id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(
                ()->new ResourceNotFoundException("Hotel Not Found with id: "+id)
        );

        //if already active then return
        if(hotel.getActive()) return;

        hotel.setActive(true);
        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("Hotel activated with id: {}",id);

        //Create inventory for all the rooms of this hotel

        List<Room> rooms = roomRepository.findByHotel(savedHotel);
        for(Room room: rooms){
            inventoryService.initRoomFor1Year(room);
        }
    }
}

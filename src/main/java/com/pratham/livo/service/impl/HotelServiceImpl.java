package com.pratham.livo.service.impl;

import com.pratham.livo.dto.hotel.HotelInfoDto;
import com.pratham.livo.dto.hotel.HotelRequestDto;
import com.pratham.livo.dto.hotel.HotelResponseDto;
import com.pratham.livo.dto.hotel.HotelSearchRequestDto;
import com.pratham.livo.dto.room.RoomResponseDto;
import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Room;
import com.pratham.livo.exception.ResourceNotFoundException;
import com.pratham.livo.projection.RoomAvailabilityWrapper;
import com.pratham.livo.repository.HotelRepository;
import com.pratham.livo.repository.InventoryRepository;
import com.pratham.livo.repository.RoomRepository;
import com.pratham.livo.service.HotelService;
import com.pratham.livo.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    @Transactional(readOnly = true)
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
        log.info("Activating hotel with id: {}",id);
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

    @Override
    @Transactional(readOnly = true)
    public PagedModel<HotelResponseDto> searchHotels(
            HotelSearchRequestDto hotelSearchRequestDto,
            Integer page,
            Integer size) {

        //searching for hotels
        //in the city
        //for the given dates
        //not closed
        //available rooms
        log.info("Searching for hotels with request: {}",hotelSearchRequestDto);
        Pageable pageable = PageRequest.of(page,size);
        Page<Hotel> hotels = inventoryRepository.findAvailableHotels(
                hotelSearchRequestDto.getCity(),
                hotelSearchRequestDto.getStartDate(),
                hotelSearchRequestDto.getEndDate(),
                hotelSearchRequestDto.getRoomsCount(),
                ChronoUnit.DAYS.between(
                        hotelSearchRequestDto.getStartDate(),hotelSearchRequestDto.getEndDate()
                )+1,
                pageable
        );

        Page<HotelResponseDto> hotelResponseDtos = hotels.map(hotel -> modelMapper.map(hotel,HotelResponseDto.class));
        log.info("Hotels retrieved successfully");
        return new PagedModel<>(hotelResponseDtos);
    }

    @Override
    @Transactional(readOnly = true)
    public HotelInfoDto getHotelInfo(Long id, LocalDate startDate, LocalDate endDate, Integer roomsCount) {

        //find hotel first
        log.info("Fetching info for hotel with id: {}",id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(()->
                        new ResourceNotFoundException("Hotel Not Found with id: "+id));
        List<RoomResponseDto> roomResponseDtos;


        // if start date and end date are not null, then
        // fetch rooms of that hotel with each marked as true/false for available
        // on the basis of inventory
        if(startDate!=null && endDate!=null){
            int targetRoomsCount = (roomsCount==null) ? roomsCount:1;
            List<RoomAvailabilityWrapper> availabilityWrappers = inventoryRepository.findRoomsWithAvailability(
                    id,
                    startDate,
                    endDate,
                    roomsCount,
                    ChronoUnit.DAYS.between(startDate,endDate)+1
            );

            roomResponseDtos = availabilityWrappers.stream()
                    .map(wrapper -> {
                        RoomResponseDto dto = modelMapper.map(wrapper.getRoom(),RoomResponseDto.class);
                        dto.setAvailable(wrapper.getIsAvailable());
                        return dto;
                    }).toList();
        }
        //else fetch all rooms in that hotel
        //general search by hotel
        else{
            List<Room> rooms = roomRepository.findByHotel(hotel);
            roomResponseDtos = rooms.stream()
                    .map(room -> {
                        RoomResponseDto dto = modelMapper.map(room,RoomResponseDto.class);
                        dto.setAvailable(true);
                        return dto;
                    })
                    .toList();
        }

        log.info("Hotel info retrieved successfully");
        return HotelInfoDto.builder()
                .hotel(modelMapper.map(hotel,HotelResponseDto.class))
                .rooms(roomResponseDtos)
                .build();
    }
}

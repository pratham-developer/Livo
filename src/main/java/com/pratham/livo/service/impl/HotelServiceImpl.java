package com.pratham.livo.service.impl;

import com.pratham.livo.dto.hotel.*;
import com.pratham.livo.dto.room.RoomResponseDto;
import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Room;
import com.pratham.livo.exception.BadRequestException;
import com.pratham.livo.exception.ResourceNotFoundException;
import com.pratham.livo.projection.PriceCheckWrapper;
import com.pratham.livo.projection.RoomAvailabilityWrapper;
import com.pratham.livo.repository.BookingRepository;
import com.pratham.livo.repository.HotelRepository;
import com.pratham.livo.repository.InventoryRepository;
import com.pratham.livo.repository.RoomRepository;
import com.pratham.livo.service.HotelService;
import com.pratham.livo.service.InventoryService;
import com.pratham.livo.utils.DateValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelServiceImpl implements HotelService {

    private final ModelMapper modelMapper;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final BookingRepository bookingRepository;
    private final DateValidator dateValidator;

    @Override
    @Transactional
    public HotelResponseDto createHotel(HotelRequestDto hotelRequestDto) {
        log.info("Creating hotel with name: {}",hotelRequestDto.getName());
        Hotel hotel = modelMapper.map(hotelRequestDto,Hotel.class);
        //set active, deleted = false on creation
        hotel.setActive(false);
        hotel.setDeleted(false);
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

        // Prevent updates to deleted hotels
        if(hotel.getDeleted()) {
            throw new BadRequestException("Cannot update a deleted hotel");
        }

        hotelRequestDto.setActive(hotel.getActive());
        modelMapper.map(hotelRequestDto,hotel);
        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("Hotel updated with id: {}",id);
        return modelMapper.map(savedHotel,HotelResponseDto.class);
    }

    @Override
    @Transactional
    public void deleteHotelById(Long id){
        log.info("Soft Deleting hotel with id: {}",id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found with id: "+id));

        //kill pending bookings
        bookingRepository.expireBookingsForHotel(hotel);
        log.info("Expired Pending Bookings for hotel with id: {}",id);

        //soft delete rooms
        int roomsDeleted = roomRepository.softDeleteByHotel(hotel);
        log.info("Soft Deleted {} rooms for hotel with id: {}",roomsDeleted, id);

        //hard delete inventory (remove from search)
        inventoryRepository.deleteByHotel(hotel);
        log.info("Hard Deleted inventory for hotel with id: {}",id);

        //soft delete hotel
        hotel.setActive(false);
        hotel.setDeleted(true);
        hotelRepository.save(hotel);
        log.info("Soft Deleted hotel with id: {}",id);
    }

    @Override
    @Transactional
    public void activateHotelById(Long id) {
        log.info("Activating hotel with id: {}",id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(
                ()->new ResourceNotFoundException("Hotel Not Found with id: "+id)
        );

        //prevent revival for a dead hotel
        if (hotel.getDeleted()) {
            throw new BadRequestException("Cannot activate a permanently deleted hotel.");
        }

        //if already active then return
        if(hotel.getActive()) return;

        hotel.setActive(true);
        Hotel savedHotel = hotelRepository.save(hotel);

        //activate non deleted rooms for this hotel
        roomRepository.activateNonDeleted(savedHotel);

        List<Room> rooms = roomRepository.findByHotelAndDeletedFalse(savedHotel);
        //Create inventory for all the non deleted rooms of this hotel
        for(Room room: rooms){
            inventoryService.initRoomFor1Year(room);
        }

        log.info("Hotel activated with id: {}",id);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedModel<HotelSearchResponseDto> searchHotels(
            HotelSearchRequestDto hotelSearchRequestDto,
            Integer page,
            Integer size) {

        //searching for hotels
        //in the city
        //for the given dates
        //not closed
        //available rooms
        log.info("Searching for hotels with request: {}",hotelSearchRequestDto);

        //date validation
        long days = dateValidator.countDays(hotelSearchRequestDto.getStartDate(),hotelSearchRequestDto.getEndDate());

        //get the page for available hotels
        Pageable pageable = PageRequest.of(page,size);
        Page<Hotel> hotelPage = inventoryRepository.findAvailableHotels(
                hotelSearchRequestDto.getCity(),
                hotelSearchRequestDto.getStartDate(),
                hotelSearchRequestDto.getEndDate(),
                hotelSearchRequestDto.getRoomsCount(),
                days,
                pageable
        );

        //if no hotel, then return an empty page
        if(hotelPage.isEmpty()){
            return new PagedModel<>(Page.empty());
        }

        //else find the list of hotel ids
        List<Long> hotelIds = hotelPage.stream()
                .map(Hotel::getId)
                .toList();

        //find the list of room prices for these hotels
        List<PriceCheckWrapper> priceCheckWrappers = inventoryRepository.findRoomAveragePrices(
                hotelIds,
                hotelSearchRequestDto.getStartDate(),
                hotelSearchRequestDto.getEndDate(),
                hotelSearchRequestDto.getRoomsCount(),
                days
        );

        //map the hotel ids with their least prices
        Map<Long, BigDecimal> priceMap = new HashMap<>();
        for (PriceCheckWrapper wrapper : priceCheckWrappers) {
            // map.merge is perfect here
            // if Key doesn't exist -> insert New Value
            // if Key exists -> run the function (old, new) -> old.min(new)
            priceMap.merge(
                    wrapper.getHotelId(),       // Key
                    wrapper.getAvgPrice(),      // Value
                    BigDecimal::min             // Function if collision (Pick smaller)
            );
        }

        //return the paginated response
        Page<HotelSearchResponseDto> responseDtoPage =
                hotelPage.map(
                        hotel -> {
                            HotelSearchResponseDto dto = modelMapper.map(hotel, HotelSearchResponseDto.class);
                            dto.setPricePerDay(priceMap.getOrDefault(hotel.getId(),BigDecimal.ZERO));
                            return dto;
                        }
                );
        log.info("Hotels retrieved successfully");
        return new PagedModel<>(responseDtoPage);
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
            //date validation
            long days = dateValidator.countDays(startDate,endDate);
            Integer targetRoomsCount = (roomsCount == null) ? 1 : roomsCount;

            //find the list of available rooms
            List<RoomAvailabilityWrapper> availabilityWrappers = inventoryRepository.findRoomsWithAvailability(
                    id, startDate, endDate, targetRoomsCount, days);

            //find the list of room prices for this hotel
            List<PriceCheckWrapper> priceCheckWrappers = inventoryRepository.findRoomAveragePrices(
                    List.of(id), startDate, endDate, targetRoomsCount, days);

            //map the room ids with their avg prices
            Map<Long, BigDecimal> priceMap = new HashMap<>();
            for (PriceCheckWrapper priceCheckWrapper : priceCheckWrappers) {
                priceMap.put(priceCheckWrapper.getRoomId(),priceCheckWrapper.getAvgPrice());
            }

            roomResponseDtos = availabilityWrappers.stream()
                    .map(wrapper -> {
                        RoomResponseDto dto = modelMapper.map(wrapper.getRoom(),RoomResponseDto.class);
                        dto.setAvailable(wrapper.getIsAvailable());
                        dto.setPricePerDay(priceMap.getOrDefault(wrapper.getRoom().getId(),wrapper.getRoom().getBasePrice()));
                        return dto;
                    }).toList();
        }
        //else fetch all rooms in that hotel
        //general search by hotel
        else{
            // Fetch all rooms, BUT filter out deleted ones
            List<Room> rooms = roomRepository.findByHotel(hotel);
            roomResponseDtos = rooms.stream()
                    .filter(room -> !(room.getDeleted()))
                    .map(room -> {
                        RoomResponseDto dto = modelMapper.map(room,RoomResponseDto.class);
                        dto.setAvailable(true);
                        dto.setPricePerDay(null);
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

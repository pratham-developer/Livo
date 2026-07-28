package com.pratham.livo.service.impl;

import com.pratham.livo.dto.auth.AuthenticatedUser;
import com.pratham.livo.dto.room.RoomRequestDto;
import com.pratham.livo.dto.room.RoomResponseDto;
import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Room;
import com.pratham.livo.enums.BookingStatus;
import com.pratham.livo.exception.BadRequestException;
import com.pratham.livo.exception.ResourceNotFoundException;
import com.pratham.livo.exception.SessionNotFoundException;
import com.pratham.livo.media.event.MediaEventPublisher;
import com.pratham.livo.media.event.MediaPromotionEvent;
import com.pratham.livo.media.util.MediaUrlProvider;
import com.pratham.livo.repository.BookingRepository;
import com.pratham.livo.repository.HotelRepository;
import com.pratham.livo.repository.InventoryRepository;
import com.pratham.livo.repository.RoomRepository;
import com.pratham.livo.security.SecurityHelper;
import com.pratham.livo.service.InventoryService;
import com.pratham.livo.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
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
    private final BookingRepository bookingRepository;
    private final SecurityHelper securityHelper;

    private final MediaEventPublisher mediaEventPublisher;
    private final MediaUrlProvider mediaUrlProvider;

    public static final int MAX_ROOMS_PER_HOTEL = 100;
    public static final int MAX_ROOM_CAPACITY = 6;

    @Override
    @Transactional
    public RoomResponseDto createNewRoomInHotel(Long hotelId, RoomRequestDto roomRequestDto) {
        log.info("Creating room in hotel(id={}) with type: {}", hotelId, roomRequestDto.getType());

        int capacity = roomRequestDto.getCapacity();
        if (capacity <= 0 || capacity > MAX_ROOM_CAPACITY) {
            throw new BadRequestException("Invalid room capacity");
        }

        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(
                ()->new ResourceNotFoundException("Hotel Not Found with id: " + hotelId)
        );

        AuthenticatedUser authenticatedUser = currentUser();
        verifyHotelOwner(authenticatedUser, hotel);

        if(hotel.getDeleted()) {
            throw new BadRequestException("Cannot add room to a deleted hotel");
        }

        long roomCount = roomRepository.countByHotelIdAndDeletedFalse(hotelId);
        if (roomCount >= MAX_ROOMS_PER_HOTEL) {
            throw new BadRequestException("Maximum room limit reached");
        }

        Room room = modelMapper.map(roomRequestDto, Room.class);
        room.setHotel(hotel);
        room.setDeleted(false);
        room.setActive(hotel.getActive());

        // Save temporary paths immediately
        room.setPhotos(roomRequestDto.getPhotos());

        // 1. Save to generate the Room ID
        Room savedRoom = roomRepository.save(room);

        // 2. Publish async event
        List<String> incomingTempPaths = roomRequestDto.getPhotos() != null ? roomRequestDto.getPhotos() : List.of();

        if (!incomingTempPaths.isEmpty()) {
            MediaPromotionEvent event = new MediaPromotionEvent(
                    savedRoom.getId(),
                    MediaPromotionEvent.EntityType.ROOM,
                    authenticatedUser.getId(),
                    incomingTempPaths,
                    List.of(),
                    List.of()
            );
            mediaEventPublisher.publishPromotionEvent(event);
        }

        log.info("Room created in hotel(id={}) with type: {}", hotelId, roomRequestDto.getType());

        if(hotel.getActive()){
            inventoryService.initRoomFor1Year(savedRoom);
        }

        return enrichWithPublicUrls(modelMapper.map(savedRoom, RoomResponseDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponseDto> getAllRoomsInHotel(Long hotelId) {
        log.info("Fetching all rooms for hotelId={}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(
                ()->new ResourceNotFoundException("Hotel Not Found with id: " + hotelId)
        );
        AuthenticatedUser authenticatedUser = currentUser();
        verifyHotelOwner(authenticatedUser, hotel);
        List<Room> rooms = roomRepository.findByHotelAndDeletedFalse(hotel);
        log.info("Found {} rooms for hotelId={}", rooms.size(), hotelId);

        return rooms.stream()
                .map(room -> enrichWithPublicUrls(modelMapper.map(room, RoomResponseDto.class)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponseDto getRoomById(Long roomId) {
        log.info("Getting room with id: {}", roomId);
        Room room = roomRepository.findById(roomId).orElseThrow(
                ()->new ResourceNotFoundException("Room Not Found with id: " + roomId)
        );
        verifyRoomOwner(room);
        log.info("Retrieved room with id: {}", roomId);
        return enrichWithPublicUrls(modelMapper.map(room, RoomResponseDto.class));
    }

    @Override
    @Transactional
    public void deleteRoomById(Long roomId) {
        log.info("Soft Deleting room with id: {}", roomId);
        Room room = roomRepository.findById(roomId).orElseThrow(
                ()->new ResourceNotFoundException("Room Not Found with id: " + roomId)
        );
        verifyRoomOwner(room);

        //soft delete room
        room.setActive(false);
        room.setDeleted(true);
        roomRepository.save(room);

        //hard delete inventory
        inventoryRepository.deleteByRoom(room);

        //delete pending bookings
        List<BookingStatus> checkList = List.of(BookingStatus.RESERVED, BookingStatus.GUESTS_ADDED, BookingStatus.PAYMENT_PENDING);
        bookingRepository.expireBookingsForRoom(room, BookingStatus.EXPIRED, checkList);

        log.info("Room with id = {} soft deleted and inventory cleared.", roomId);
    }

    private void verifyHotelOwner(AuthenticatedUser authenticatedUser, Hotel hotel){
        if(!authenticatedUser.getId().equals(hotel.getOwner().getId())){
            throw new AccessDeniedException("Hotel does not belong to the authenticated user");
        }
    }

    private void verifyRoomOwner(Room room){
        AuthenticatedUser authenticatedUser = currentUser();
        if(!authenticatedUser.getId().equals(room.getHotel().getOwner().getId())){
            throw new AccessDeniedException("Hotel does not belong to the authenticated user");
        }
    }

    private AuthenticatedUser currentUser() {
        return securityHelper.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new SessionNotFoundException("Cannot identify the authenticated user"));
    }

    private RoomResponseDto enrichWithPublicUrls(RoomResponseDto dto) {
        if (dto != null && dto.getPhotos() != null) {
            dto.setPhotos(mediaUrlProvider.generatePublicUrls(dto.getPhotos()));
        }
        return dto;
    }
}
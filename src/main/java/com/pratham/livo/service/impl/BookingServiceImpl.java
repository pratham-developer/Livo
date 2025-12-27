package com.pratham.livo.service.impl;

import com.pratham.livo.dto.booking.AddGuestDto;
import com.pratham.livo.dto.booking.BookingResponseDto;
import com.pratham.livo.dto.booking.BookingRequestDto;
import com.pratham.livo.dto.booking.GetGuestDto;
import com.pratham.livo.entity.*;
import com.pratham.livo.entity.enums.BookingStatus;
import com.pratham.livo.exception.BadRequestException;
import com.pratham.livo.exception.InventoryBusyException;
import com.pratham.livo.exception.ResourceNotFoundException;
import com.pratham.livo.repository.*;
import com.pratham.livo.service.BookingService;
import com.pratham.livo.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final GuestRepository guestRepository;
    private final ModelMapper modelMapper;
    private final DateValidator dateValidator;
    private final InventoryService inventoryService;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    public BookingResponseDto initBooking(BookingRequestDto bookingRequestDto) {
        log.info("Starting booking initialization for room: {}", bookingRequestDto.getRoomId());

        //validate the dates
        long days = dateValidator.countDays(bookingRequestDto.getStartDate(),bookingRequestDto.getEndDate());

        //check if room exists
        Room room = roomRepository.findById(bookingRequestDto.getRoomId()).orElseThrow(
                ()->new ResourceNotFoundException("Room not found with id: "+ bookingRequestDto.getRoomId())
        );

        //retrieve inventory for the room over the requested dates with pessimistic write lock
        List<Inventory> inventoryList;
        try{
            inventoryList = inventoryRepository.findInventoriesForRoom(
                    bookingRequestDto.getRoomId(),
                    bookingRequestDto.getStartDate(),
                    bookingRequestDto.getEndDate(),
                    bookingRequestDto.getRoomsCount()
            );
        //if timeout while acquiring lock
        }catch(PessimisticLockingFailureException e){
            log.error("Failed to acquire lock for room {}", bookingRequestDto.getRoomId());
            throw new InventoryBusyException("Room is currently being booked by another user. Please try again later.");
        }

        if(inventoryList.size()!=days){
            throw new BadRequestException("Room is not available for all selected dates");
        }

        //calculate amount
        BigDecimal amount = inventoryService.calculateTotalAmount(inventoryList);

        //reserve rooms in the inventories
        for(Inventory i : inventoryList){
            i.setReservedCount(i.getReservedCount() + bookingRequestDto.getRoomsCount());
        }

        //save inventories
        inventoryRepository.saveAll(inventoryList);

        //create booking with reserved state
        Booking booking = Booking.builder()
                .hotel(room.getHotel())
                .room(room)
                .user(getCurrentUser())
                .amount(amount)
                .bookingStatus(BookingStatus.RESERVED)
                .startDate(bookingRequestDto.getStartDate())
                .endDate(bookingRequestDto.getEndDate())
                .roomsCount(bookingRequestDto.getRoomsCount())
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking initialized with ID: {}", savedBooking.getId());

        return getBookingResponseDto(savedBooking);
    }

    @Override
    @Transactional
    public BookingResponseDto addGuests(Long bookingId, List<AddGuestDto> guestDtoList) {
        log.info("Adding guests to booking with id: {}",bookingId);

        //check if booking exists
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                ()->new ResourceNotFoundException("Booking not found with id: "+bookingId)
        );

        //ensure booking status is valid
        if(hasExpired(booking)) throw new BadRequestException("Booking has expired");

        if(booking.getBookingStatus()!=BookingStatus.RESERVED
                && booking.getBookingStatus()!=BookingStatus.GUESTS_ADDED){
            throw new BadRequestException("Guests cannot be added. Booking is in status: " + booking.getBookingStatus());
        }

        //max capacity = room capacity * number of rooms booked
        int maxCapacity = booking.getRoom().getCapacity() * booking.getRoomsCount();
        int currentGuests = booking.getGuests() == null ? 0 : booking.getGuests().size();

        if (currentGuests + guestDtoList.size() > maxCapacity) {
            throw new BadRequestException("Cannot add guests. Max capacity is: " + maxCapacity);
        }

        //get current guest list
        if(booking.getGuests() == null){
            booking.setGuests(new HashSet<>());
        }
        Set<Guest> guestList = booking.getGuests();

        //add new guests to this list
        for(AddGuestDto addGuestDto: guestDtoList){
            Guest guest = modelMapper.map(addGuestDto, Guest.class);
            guest.setUser(booking.getUser());
            guestList.add(guest);
        }

        //save new guest list to db
        guestRepository.saveAll(guestList);

        //assign guest list to booking
        booking.setGuests(guestList);
        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);

        //save booking
        Booking savedBooking = bookingRepository.save(booking);

        BookingResponseDto bookingResponseDto = getBookingResponseDto(savedBooking);

        Set<GetGuestDto> getGuestDtoSet = new HashSet<>();
        for(Guest guest : savedBooking.getGuests()){
            GetGuestDto getGuestDto = modelMapper.map(guest, GetGuestDto.class);
            getGuestDto.setUserId(guest.getUser().getId());
            getGuestDtoSet.add(getGuestDto);
        }
        bookingResponseDto.setGuests(getGuestDtoSet);
        return bookingResponseDto;

    }

    @Override
    @Scheduled(cron = "0 * * * * *") // Runs every minute
    //no Transactional here
    //we manage it manually inside
    public void cleanExpiredBookings() {
        long start = System.currentTimeMillis();

        //setup criteria
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        List<BookingStatus> statusList = List.of(
                BookingStatus.RESERVED,
                BookingStatus.GUESTS_ADDED,
                BookingStatus.PAYMENT_PENDING
        );

        int totalProcessed = 0;
        boolean hasMore = true;

        log.info("Starting Booking Cleanup Job");

        //Loop for batch processing
        while (hasMore) {

            // batch level transaction starts here
            // locks are acquired here
            Integer batchCount = transactionTemplate.execute(status -> {

                Pageable limit = PageRequest.of(0, 50);
                List<Booking> expiredBookings = bookingRepository.findByBookingStatusInAndUpdatedAtBefore(
                        statusList, threshold, limit
                );

                if (expiredBookings.isEmpty()) {
                    return 0;
                }

                // process batch of expired bookings
                for (Booking b : expiredBookings) {
                    try {
                        //set status as expired
                        b.setBookingStatus(BookingStatus.EXPIRED);

                        //find inventory list corresponding to the booking with locks
                        List<Inventory> inventoryList = inventoryRepository.findInventoriesForCleanup(
                                b.getRoom(), b.getStartDate(), b.getEndDate());

                        //reset the reserved count in inventories
                        for (Inventory i : inventoryList) {
                            int newReserved = i.getReservedCount() - b.getRoomsCount();
                            i.setReservedCount(Math.max(0, newReserved));
                        }

                        //saving inventories
                        inventoryRepository.saveAll(inventoryList);
                    } catch (Exception e) {
                        log.error("Error expiring booking ID: {}", b.getId(), e);
                        //catching exception to prevent rollback of the entire batch due to one booking
                    }
                }

                //saving expired bookings
                bookingRepository.saveAll(expiredBookings);
                return expiredBookings.size();
            });
            // transaction ends here
            // commit happens and locks are released immediately

            // if no batch processed then stop loop
            if (batchCount == null || batchCount == 0) {
                hasMore = false;
            } else {
                totalProcessed += batchCount;
                log.debug("Processed batch of {} bookings", batchCount);
            }

            // limit max batch size to 2000
            if (totalProcessed > 2000) {
                log.warn("Cleanup Job hit safety limit (2000). Aborting until next run.");
                break;
            }
        }

        if (totalProcessed > 0) {
            log.info("Cleanup Job Finished. Total expired: {} (Time: {}ms)",
                    totalProcessed, System.currentTimeMillis() - start);
        }
    }

    private User getCurrentUser(){
        //TODO: get current logged in user using spring security
        return userRepository.findById(1L).orElseThrow(
                ()->new ResourceNotFoundException("User not found")
        );
    }

    private BookingResponseDto getBookingResponseDto(Booking savedBooking) {
        BookingResponseDto bookingResponseDto = modelMapper.map(savedBooking, BookingResponseDto.class);
        bookingResponseDto.setHotelId(savedBooking.getHotel().getId());
        bookingResponseDto.setRoomId(savedBooking.getRoom().getId());
        bookingResponseDto.setUserId(savedBooking.getUser().getId());
        bookingResponseDto.setHotelName(savedBooking.getHotel().getName());
        bookingResponseDto.setRoomType(savedBooking.getRoom().getType());
        bookingResponseDto.setHotelCity(savedBooking.getHotel().getCity());
        return bookingResponseDto;
    }

    private Boolean hasExpired(Booking booking){
        return booking.getBookingStatus() == BookingStatus.EXPIRED;
    }
}

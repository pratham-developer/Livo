package com.pratham.livo.service.impl;

import com.pratham.livo.dto.auth.AuthenticatedUser;
import com.pratham.livo.dto.booking.*;
import com.pratham.livo.dto.message.RefundMessage;
import com.pratham.livo.entity.*;
import com.pratham.livo.enums.BookingStatus;
import com.pratham.livo.enums.PaymentStatus;
import com.pratham.livo.exception.BadRequestException;
import com.pratham.livo.exception.InventoryBusyException;
import com.pratham.livo.exception.ResourceNotFoundException;
import com.pratham.livo.exception.SessionNotFoundException;
import com.pratham.livo.projection.BookingWrapper;
import com.pratham.livo.repository.*;
import com.pratham.livo.security.SecurityHelper;
import com.pratham.livo.service.BookingService;
import com.pratham.livo.service.InventoryService;
import com.pratham.livo.service.MessagePublisher;
import com.pratham.livo.utils.DateValidator;
import com.pratham.livo.utils.IdempotencyUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final GuestRepository guestRepository;
    private final ModelMapper modelMapper;
    private final DateValidator dateValidator;
    private final InventoryService inventoryService;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final SecurityHelper securityHelper;

    private final static long BOOKING_SESSION_TIME_LIMIT = 10L; //in minutes
    private final static String KEY_PREFIX = "booking:idempotency:";
    private final IdempotencyUtil idempotencyUtil;
    private final PaymentRepository paymentRepository;
    private final MessagePublisher messagePublisher;

    @Override
    @Transactional
    public BookingResponseDto initBooking(BookingRequestDto bookingRequestDto) {
        log.info("Starting booking initialization for room: {}", bookingRequestDto.getRoomId());

        //check if not repeat request using idempotency key
        if (bookingRequestDto.getIdempotencyKey() == null) {
            throw new BadRequestException("Idempotency key is missing");
        }
        String idempotencyKey = bookingRequestDto.getIdempotencyKey().toString();
        String redisKey = KEY_PREFIX + idempotencyKey;
        if(!idempotencyUtil.acquireLock(redisKey,BOOKING_SESSION_TIME_LIMIT)){
            throw new BadRequestException("Booking is already initiated.");
        }

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

        //attach the current user
        AuthenticatedUser authenticatedUser = currentUser();
        User user = entityManager.getReference(User.class, authenticatedUser.getId());

        //create booking with reserved state
        Booking booking = Booking.builder()
                .hotel(room.getHotel())
                .room(room)
                .user(user)
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

        verifyBookingOwner(booking);

        //ensure booking status is valid
        if(hasExpired(booking)) throw new BadRequestException("Booking has expired");

        if(booking.getBookingStatus()!=BookingStatus.RESERVED
                && booking.getBookingStatus()!=BookingStatus.GUESTS_ADDED){
            throw new BadRequestException("Guests cannot be added. Booking is in status: " + booking.getBookingStatus());
        }

        //max capacity = room capacity * number of rooms booked
        int maxCapacity = booking.getRoom().getCapacity() * booking.getRoomsCount();

        if (guestDtoList.size() > maxCapacity) {
            throw new BadRequestException("Cannot add guests. Max capacity is: " + maxCapacity);
        }

        //delete the previously added guests if any
        if(booking.getGuests()!=null && !booking.getGuests().isEmpty()){
            guestRepository.deleteByBookingId(bookingId);
            booking.getGuests().clear();
            guestRepository.flush();
        }
        else{
            booking.setGuests(new HashSet<>());
        }

        Set<Guest> guestList = booking.getGuests();

        //add new guests to this list
        for(AddGuestDto addGuestDto: guestDtoList){
            Guest guest = modelMapper.map(addGuestDto, Guest.class);
            guest.setUser(booking.getUser());
            guest.setBooking(booking);
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
        log.info("Guests successfully added to booking with id: {}",bookingId);
        return bookingResponseDto;

    }

    @Override
    @Scheduled(cron = "0 * * * * *") // Runs every minute
    //no Transactional here
    //we manage it manually inside
    public void cleanExpiredBookings() {
        long start = System.currentTimeMillis();

        //setup criteria
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(BOOKING_SESSION_TIME_LIMIT);
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

                List<Booking> modifiedBookings = new ArrayList<>();

                // process batch of expired bookings
                for (Booking b : expiredBookings) {
                    try {
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

                        // if inventory save succeeds, then change the status
                        b.setBookingStatus(BookingStatus.EXPIRED);
                        modifiedBookings.add(b); // Add to the list of modified bookings
                    } catch (Exception e) {
                        log.error("Error expiring booking ID: {}", b.getId(), e);
                        //catching exception to prevent rollback of the entire batch due to one booking
                    }
                }

                //saving expired bookings
                bookingRepository.saveAll(modifiedBookings);

                //clear memory
                entityManager.flush();
                entityManager.clear();
                return modifiedBookings.size();
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

    @Override
    @Transactional
    public BookingResponseDto cancelBooking(Long bookingId) {
        log.info("Cancelling booking with id: {}",bookingId);
        //check if booking exists
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                ()->new ResourceNotFoundException("Booking not found with id: "+bookingId)
        );

        verifyBookingOwner(booking);

        if(booking.getBookingStatus() == BookingStatus.CANCELLED){
            log.info("Booking with id: {} is already cancelled",bookingId);
            return getBookingResponseDto(booking);
        }

        //ensure booking is confirmed
        if(booking.getBookingStatus()!=BookingStatus.CONFIRMED){
            throw new BadRequestException("Booking is not in confirmed state");
        }

        //get payment for booking
        Payment payment = paymentRepository.findByBooking(booking).orElseThrow(
                ()->new ResourceNotFoundException("Payment not found for booking with id: "+bookingId)
        );

        //ensure payment is in successful state
        if(payment.getPaymentStatus()!=PaymentStatus.SUCCESSFUL){
            throw new BadRequestException("Payment is not in successful state");
        }

        //update booking
        booking.setBookingStatus(BookingStatus.CANCELLED);

        //get inventory list to be updated
        List<Inventory> inventoryList = inventoryRepository.findInventoriesForBooking(
                booking.getRoom().getId(),
                booking.getStartDate(),
                booking.getEndDate()
        );

        for(Inventory i : inventoryList){
            //remove rooms from booked count
            int booked = i.getBookedCount()-booking.getRoomsCount();
            i.setBookedCount(Math.max(0,booked));
        }

        //save booking
        Booking savedBooking = bookingRepository.save(booking);

        //save inventory list
        inventoryRepository.saveAllAndFlush(inventoryList);

        //initiate refund
        RefundMessage refundMessage = RefundMessage.builder()
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .reason("User Manually Cancelled Booking")
                .percentage(calculateRefundPercentage(booking.getStartDate()))
                .build();
        messagePublisher.publishRefund(refundMessage);
        log.info("Successfully cancelled booking with id: {}",bookingId);
        return getBookingResponseDto(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedModel<BookingWrapperDto> getMyBookings(Integer page, Integer size) {
        log.info("Fetching bookings for a user");
        //get the authenticated user
        AuthenticatedUser user = currentUser();

        //get bookings page for the user
        Pageable pageable = PageRequest.of(page,size,
                Sort.by("startDate").descending()
                        .and(Sort.by("endDate").descending()));

        List<BookingStatus> statusList = List.of(BookingStatus.CONFIRMED,BookingStatus.CANCELLED);
        Page<BookingWrapper> bookingWrappers = bookingRepository.findMyBookings(
                user.getId(),statusList,pageable);

        Page<BookingWrapperDto> dtoPage = bookingWrappers
                .map(bookingWrapper -> modelMapper.map(bookingWrapper, BookingWrapperDto.class));
        log.info("Successfully fetched bookings for a user");
        return new PagedModel<>(dtoPage);

    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDto getBookingById(Long bookingId) {
        log.info("Fetching booking with id: {}",bookingId);
        Booking booking = bookingRepository.findByIdWithGuests(bookingId).orElseThrow(
                ()->new ResourceNotFoundException("Booking not found with id: "+bookingId)
        );
        verifyBookingOwner(booking);
        log.info("Successfully fetched booking with id: {}",bookingId);
        return getBookingResponseDto(booking);
    }

    private AuthenticatedUser currentUser() {
        return securityHelper.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new SessionNotFoundException("Cannot identify the authenticated user"));
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

    private void verifyBookingOwner(Booking booking){
        //check if booking belongs to the authenticated user
        AuthenticatedUser authenticatedUser = currentUser();
        if(!authenticatedUser.getId().equals(booking.getUser().getId())){
            throw new AccessDeniedException("Booking does not belong to the authenticated user");
        }
    }

    private int calculateRefundPercentage(LocalDate startDate){
        long daysLeft = dateValidator.countDaysToBooking(startDate);
        if(daysLeft==1) return 50;
        if(daysLeft==2) return 75;
        return 100;
    }
}

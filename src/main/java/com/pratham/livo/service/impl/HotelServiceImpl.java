package com.pratham.livo.service.impl;

import com.pratham.livo.dto.auth.AuthenticatedUser;
import com.pratham.livo.dto.hotel.*;
import com.pratham.livo.dto.review.ReviewDto;
import com.pratham.livo.dto.room.RoomResponseDto;
import com.pratham.livo.entity.Hotel;
import com.pratham.livo.entity.Room;
import com.pratham.livo.entity.User;
import com.pratham.livo.enums.BookingStatus;
import com.pratham.livo.exception.BadRequestException;
import com.pratham.livo.exception.ResourceNotFoundException;
import com.pratham.livo.exception.SessionNotFoundException;
import com.pratham.livo.media.event.MediaEventPublisher;
import com.pratham.livo.media.event.MediaPromotionEvent;
import com.pratham.livo.media.util.MediaUrlProvider;
import com.pratham.livo.projection.*;
import com.pratham.livo.repository.*;
import com.pratham.livo.security.SecurityHelper;
import com.pratham.livo.service.HotelService;
import com.pratham.livo.service.InventoryService;
import com.pratham.livo.utils.DateValidator;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final SecurityHelper securityHelper;
    private final EntityManager entityManager;
    public static final int MAX_HOTELS_PER_OWNER = 10;
    private final RefundRepository refundRepository;
    private final MediaEventPublisher mediaEventPublisher;
    private final MediaUrlProvider mediaUrlProvider;
    private final ReviewRepository reviewRepository;

    @Value("${count.best.hotels}")
    private int COUNT_BEST_HOTELS;

    @Override
    @Transactional
    public HotelResponseDto createHotel(HotelRequestDto hotelRequestDto) {
        AuthenticatedUser authenticatedUser = currentUser();
        long hotelCount = hotelRepository.countByOwnerIdAndDeletedFalse(authenticatedUser.getId());
        if (hotelCount >= MAX_HOTELS_PER_OWNER) {
            throw new BadRequestException("Maximum hotel limit reached");
        }

        log.info("Creating hotel with name: {}", hotelRequestDto.getName());
        Hotel hotel = modelMapper.map(hotelRequestDto, Hotel.class);

        User ownerRef = entityManager.getReference(User.class, authenticatedUser.getId());
        hotel.setOwner(ownerRef);
        hotel.setActive(false);
        hotel.setDeleted(false);

        // We initially save the hotel with the temporary paths in the DB.
        // Once the RabbitMQ worker finishes, it will overwrite these with permanent paths.
        hotel.setPhotos(hotelRequestDto.getPhotos());

        // 1. Save to generate the ID
        Hotel savedHotel = hotelRepository.save(hotel);

        // 2. Publish the async event
        List<String> incomingTempPaths = hotelRequestDto.getPhotos() != null ? hotelRequestDto.getPhotos() : List.of();

        if (!incomingTempPaths.isEmpty()) {
            MediaPromotionEvent event = new MediaPromotionEvent(
                    savedHotel.getId(),
                    MediaPromotionEvent.EntityType.HOTEL,
                    authenticatedUser.getId(),
                    incomingTempPaths,
                    List.of(), // No retained paths on creation
                    List.of()  // No paths to delete on creation
            );
            mediaEventPublisher.publishPromotionEvent(event);
        }

        log.info("Hotel created with id: {}", savedHotel.getId());
        return enrichWithPublicUrls(modelMapper.map(savedHotel, HotelResponseDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public HotelResponseDto getHotelById(Long id) {
        log.info("Getting hotel with id: {}",id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(()->
                new ResourceNotFoundException("Hotel Not Found with id: "+id));
        verifyHotelOwner(hotel);
        log.info("Hotel retrieved with id: {}",hotel.getId());
        return enrichWithPublicUrls(modelMapper.map(hotel, HotelResponseDto.class));
    }

    @Override
    @Transactional
    public HotelResponseDto updateHotelById(Long id, HotelRequestDto hotelRequestDto) {
        log.info("Updating hotel with id: {}", id);
        Hotel hotel = hotelRepository.findById(id).orElseThrow(()->
                new ResourceNotFoundException("Hotel Not Found with id: " + id));

        verifyHotelOwner(hotel);

        if(hotel.getDeleted()) {
            throw new BadRequestException("Cannot update a deleted hotel");
        }

        List<String> existingPhotosInDb = hotel.getPhotos() != null ? hotel.getPhotos() : List.of();
        List<String> incomingPaths = hotelRequestDto.getPhotos() != null ? hotelRequestDto.getPhotos() : List.of();

        List<String> newTempPaths = incomingPaths.stream()
                .filter(path -> path.startsWith("temp/"))
                .toList();

        List<String> retainedPermanentPaths = incomingPaths.stream()
                .filter(path -> !path.startsWith("temp/"))
                .toList();

        List<String> photosToDelete = existingPhotosInDb.stream()
                .filter(oldPhoto -> !retainedPermanentPaths.contains(oldPhoto))
                .toList();

        String destinationPrefix = "hotels/" + hotel.getId() + "/";
        for (String retainedPath : retainedPermanentPaths) {
            if (!retainedPath.startsWith(destinationPrefix)) {
                throw new AccessDeniedException("Unauthorized media path: " + retainedPath);
            }
        }

        // We temporarily save the exact mix of old permanent and new temp paths the user sent.
        // The RabbitMQ worker will clean this up shortly.
        hotelRequestDto.setActive(hotel.getActive());
        modelMapper.map(hotelRequestDto, hotel);
        hotel.setPhotos(incomingPaths);

        Hotel savedHotel = hotelRepository.save(hotel);

        // Publish the async event ONLY if there are new files to promote or old files to delete
        if (!newTempPaths.isEmpty() || !photosToDelete.isEmpty()) {
            MediaPromotionEvent event = new MediaPromotionEvent(
                    hotel.getId(),
                    MediaPromotionEvent.EntityType.HOTEL,
                    currentUser().getId(),
                    newTempPaths,
                    retainedPermanentPaths,
                    photosToDelete
            );
            mediaEventPublisher.publishPromotionEvent(event);
        }

        log.info("Hotel updated with id: {}", id);
        return enrichWithPublicUrls(modelMapper.map(savedHotel, HotelResponseDto.class));
    }


    @Override
    @Transactional
    public void deleteHotelById(Long id){
        log.info("Soft Deleting hotel with id: {}",id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found with id: "+id));

        verifyHotelOwner(hotel);

        //kill pending bookings
        List<BookingStatus> checkList = List.of(BookingStatus.RESERVED, BookingStatus.GUESTS_ADDED, BookingStatus.PAYMENT_PENDING);
        bookingRepository.expireBookingsForHotel(hotel,BookingStatus.EXPIRED,checkList);
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

        verifyHotelOwner(hotel);

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
                            dto.setPhotos(mediaUrlProvider.generatePublicUrls(hotel.getPhotos()));
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
                .hotel(enrichWithPublicUrls(modelMapper.map(hotel, HotelResponseDto.class)))
                .rooms(roomResponseDtos)
                .build();
    }

    @Override
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(
            name = "updatePopularityTask",
            lockAtLeastFor = "PT10M",
            lockAtMostFor = "PT1H"
    )
    @Transactional
    public void updatePopularityOfActiveHotels() {
        log.info("CRON JOB START: Updating Hotel Popularity Scores");
        try {
            hotelRepository.updatePopularityOfActiveHotels(BookingStatus.CONFIRMED.name());
            log.info("CRON JOB SUCCESS: Popularity Scores Updated");
        } catch (Exception e) {
            log.error("CRON JOB FAILED: error updating popularity scores", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelResponseDto> getBestHotels() {
        log.info("Fetching best hotels");
        Pageable pageable = PageRequest.of(0,COUNT_BEST_HOTELS,
                Sort.by("popularityScore").descending());
        List<BestHotelWrapper> bestHotelWrapperList = hotelRepository.findBestHotels(pageable);
        log.info("Successfully fetched best hotels");
        return bestHotelWrapperList.stream()
                .map(wrapper -> enrichWithPublicUrls(modelMapper.map(wrapper, HotelResponseDto.class)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedModel<HotelResponseDto> getHotelsForHotelManager(Integer page, Integer size) {
        log.info("Retrieving hotels for a hotel manager");
        AuthenticatedUser user = currentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Page<ManagersHotelWrapper> wrappers = hotelRepository.findManagersHotels(user.getId(),pageable);
        Page<HotelResponseDto> hotels = wrappers.map(
                wrapper -> enrichWithPublicUrls(modelMapper.map(wrapper, HotelResponseDto.class)));
        log.info("Successfully retrieved hotels for a hotel manager");
        return new PagedModel<>(hotels);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedModel<HotelBookingDto> getBookingsForHotel(Long hotelId, HotelBookingsRequestDto requestDto, Integer page, Integer size) {
        log.info("Getting bookings for hotel with id: {}", hotelId);

        LocalDate from = requestDto.getFrom();
        LocalDate to = requestDto.getTo();

        //validate the range if both are provided
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Invalid date range: from date cannot be after to date");
        }

        AuthenticatedUser user = currentUser();
        if(!hotelRepository.existsByIdAndOwnerId(hotelId, user.getId())){
            throw new ResourceNotFoundException("Hotel not found for the authenticated user");
        }

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("startDate").descending()
                        .and(Sort.by("endDate").descending()));

        List<BookingStatus> statusList = List.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED);

        //get bookings
        Page<HotelBookingWrapper> bookingWrappers = bookingRepository.findBookingsForHotel(
                hotelId, from, to, statusList, pageable);

        Page<HotelBookingDto> dtoPage = bookingWrappers
                .map(bookingWrapper -> modelMapper.map(bookingWrapper, HotelBookingDto.class));

        return new PagedModel<>(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public HotelReportDto getHotelReport(Long hotelId, HotelBookingsRequestDto requestDto) {
        log.info("Getting report for hotel with id: {}", hotelId);

        //validate dates
        LocalDate from = requestDto.getFrom();
        LocalDate to = requestDto.getTo();

        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Invalid date range: from date cannot be after to date");
        }

        //verify hotel owner
        AuthenticatedUser user = currentUser();
        if(!hotelRepository.existsByIdAndOwnerId(hotelId, user.getId())){
            throw new ResourceNotFoundException("Hotel not found for the authenticated user");
        }

        //fetch stats
        HotelBookingStats stats = bookingRepository.findBookingStats(hotelId, from, to,
                BookingStatus.CONFIRMED,BookingStatus.CANCELLED);

        //fetch total refund amount
        BigDecimal refundAmount = refundRepository.findTotalAmountRefunded(hotelId, from, to,
                BookingStatus.CANCELLED);

        //confirmed metrics
        Long confirmedBookings = stats.getConfirmedCount();
        BigDecimal confirmedRevenue = stats.getConfirmedRevenue();

        BigDecimal avgRevenue = BigDecimal.ZERO;
        if (confirmedBookings > 0) {
            avgRevenue = confirmedRevenue.divide(
                    BigDecimal.valueOf(confirmedBookings), 2, RoundingMode.HALF_UP
            );
        }

        //cancelled metrics
        Long cancelledBookings = stats.getCancelledCount();
        BigDecimal lostRevenue = stats.getLostRevenue();
        BigDecimal totalRefunds = (refundAmount != null) ? refundAmount : BigDecimal.ZERO;

        //cancellation rate
        double cancellationRate = 0.0;
        long totalBookings = confirmedBookings + cancelledBookings;

        if (totalBookings > 0) {
            double rate = ((double) cancelledBookings / totalBookings) * 100;
            cancellationRate = BigDecimal.valueOf(rate)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        log.info("Successfully generated report for hotel with id: {}", hotelId);

        return HotelReportDto.builder()
                .confirmedBookings(confirmedBookings)
                .confirmedRevenue(confirmedRevenue)
                .avgRevenuePerConfirmedBooking(avgRevenue)
                .cancelledBookings(cancelledBookings)
                .revenueLostToCancellations(lostRevenue)
                .totalRefundsProcessed(totalRefunds)
                .cancellationRate(cancellationRate)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedModel<ReviewDto> getHotelReviews(Long hotelId, Integer page, Integer size) {
        log.info("Fetching reviews for hotel ID: {}", hotelId);

        // Sort by newest reviews first
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // The repository now directly returns the exact wrapper we need
        Page<ReviewWrapper> wrapperPage = reviewRepository.findByHotelIdWithUser(hotelId, pageable);

        // Map the projection to the required DTO
        Page<ReviewDto> dtoPage = wrapperPage.map(wrapper ->
                ReviewDto.builder()
                        .rating(wrapper.getRating())
                        .text(wrapper.getText())
                        .reviewerName(wrapper.getReviewerName())
                        .build()
        );

        return new PagedModel<>(dtoPage);
    }

    private void verifyHotelOwner(Hotel hotel){
        //check if hotel belongs to the authenticated user
        AuthenticatedUser authenticatedUser = currentUser();

        if(!authenticatedUser.getId().equals(hotel.getOwner().getId())){
            throw new AccessDeniedException("Hotel does not belong to the authenticated user");
        }
    }

    private AuthenticatedUser currentUser() {
        return securityHelper.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new SessionNotFoundException("Cannot identify the authenticated user"));
    }

    private HotelResponseDto enrichWithPublicUrls(HotelResponseDto dto) {
        if (dto != null && dto.getPhotos() != null) {
            dto.setPhotos(mediaUrlProvider.generatePublicUrls(dto.getPhotos()));
        }
        return dto;
    }
}

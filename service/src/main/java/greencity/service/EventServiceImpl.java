package greencity.service;

import greencity.constant.ErrorMessage;
import greencity.dto.user.UserVO;
import greencity.enums.Role;
import greencity.exception.exceptions.NotFoundException;
import greencity.exception.exceptions.PastEventUpdateException;
import greencity.exception.exceptions.UnauthorizedException;
import greencity.repository.EventAttenderRepo;
import greencity.enums.EventStatus;
import greencity.enums.EventType;
import greencity.repository.EventDateTimeLocationRepo;
import greencity.repository.EventImageRepo;
import greencity.repository.EventRepo;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import greencity.dto.event.*;
import greencity.entity.*;
import greencity.exception.exceptions.BadRequestException;
import org.springframework.web.multipart.MultipartFile;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepo eventRepository;
    private final EventDateTimeLocationRepo dateTimeLocationRepository;
    private final EventImageRepo eventImageRepository;
    private final EventAttenderRepo eventAttenderRepo;
    private final ImageStorageService imageStorageService;
    private final EntityManager entityManager;
    private final ModelMapper mapper;
    private final UserService userService;

    /**
     * {@inheritDoc}
     *
     * @author Kateryna Holtvianska.
     */
    @Override
    @Transactional
    public EventDto createEvent(AddEventDtoRequest dto, MultipartFile[] images, Long organizerId) {
        validateEvent(dto, images);
        Event event = Event.builder()
                .title(dto.getTitle().trim())
                .description(dto.getDescription().trim())
                .open(dto.isOpen())
                .organizerId(organizerId)
                .createdAt(OffsetDateTime.now())
                .build();

        event = eventRepository.save(event);

        Event finalEvent = event;
        List<EventDateTimeLocation> dateLocations = dto.getDatesLocations().stream()
            .map(d -> EventDateTimeLocation.builder()
                .event(finalEvent)
                .startDate(d.getStartDate())
                .finishDate(d.getFinishDate())
                .latitude(d.getLatitude())
                .longitude(d.getLongitude())
                .onlineLink(d.getOnlineLink())
                .createdAt(OffsetDateTime.now())
                .updatedAt(null)
                .build())
            .toList();

        dateTimeLocationRepository.saveAll(dateLocations);
        event.setDateTimeLocations(dateLocations);

        List<String> imagePaths = imageStorageService.storeImages(images, event.getId());
        List<EventImage> eventImages = new ArrayList<>();

        for (int i = 0; i < imagePaths.size(); i++) {
            EventImage img = EventImage.builder()
                    .event(event)
                    .imagePath(imagePaths.get(i))
                    .main(i == 0) // перше зображення — головне
                    .createdAt(OffsetDateTime.now())
                    .build();
            eventImages.add(img);
        }

        eventImageRepository.saveAll(eventImages);
        event.setImages(eventImages);

        return toEventDto(event);
    }

    /**
     * {@inheritDoc}
     *
     * @author Oleksandr Obydalo
     */
    @Override
    @Transactional()
    public Page<EventPreviewDto> getMyEvents(Long userId, EventType eventType, EventStatus status,
        Double userLatitude, Double userLongitude, Pageable pageable) {
        OffsetDateTime currentTime = OffsetDateTime.now();

        Page<Event> events;

        if (eventType != null && eventType != EventType.BOTH) {
            // Use dummy coordinates (0.0) when not provided to avoid PostgreSQL casting
            // errors
            // For ONLINE events: dummy coordinates are ignored by the CASE statement
            // (eventType != 'PLACE')
            // For PLACE events without coordinates: will filter correctly but sort by
            // distance from (0,0)
            Double latitude = (userLatitude != null) ? userLatitude : 0.0;
            Double longitude = (userLongitude != null) ? userLongitude : 0.0;

            events = eventAttenderRepo.findJoinedEventsWithSorting(
                    userId, currentTime, eventType.name(), latitude, longitude, pageable);
        } else {
            events = eventAttenderRepo.findJoinedEventsDefaultSorting(
                    userId, currentTime, pageable);
        }

        // Resolve current user role for flag computation
        UserVO currentUser = userService.findById(userId);
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        List<EventPreviewDto> eventPreviews = events.getContent().stream()
            .map(this::toEventPreviewDto)
            .filter(event -> status == null || event.getStatus() == status)
            .toList();

        return new PageImpl<>(eventPreviews, pageable, events.getTotalElements());
    }

    /**
     * {@inheritDoc}
     *
     * @author Oleksandr Obydalo
     */
    @Override
    @Transactional
    public Page<EventPreviewDto> getMyCreatedEvents(Long userId, EventStatus status, Pageable pageable) {
        Page<Event> events = eventRepository.findByOrganizerIdOrderByNearestStart(userId, pageable);

        // Get current user to check roles
        UserVO currentUser = userService.findById(userId);
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        List<EventPreviewDto> eventPreviews = events.getContent().stream()
            .map(event -> toEventPreviewDtoWithCanEdit(event, userId, isAdmin))
            .filter(event -> status == null || event.getStatus() == status)
            .toList();

        return new PageImpl<>(eventPreviews, pageable, events.getTotalElements());
    }

    /**
     * {@inheritDoc}
     *
     * @author Oleksandr Obydalo
     */
    @Override
    @Transactional
    public Page<EventPreviewDto> getRelatedEvents(Long userId, EventStatus status, Pageable pageable) {
        Page<Event> events = eventRepository.findRelatedEventsByUserId(userId, pageable);

        // Get current user to check roles
        UserVO currentUser = userService.findById(userId);
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        List<EventPreviewDto> eventPreviews = events.getContent().stream()
            .map(event -> toEventPreviewDtoWithCanEdit(event, userId, isAdmin))
            .filter(event -> status == null || event.getStatus() == status)
            .toList();

        return new PageImpl<>(eventPreviews, pageable, events.getTotalElements());
    }

    /**
     * {@inheritDoc}
     *
     * @author Andrii Zakordonskyi
     */
    @Override
    @Transactional
    public EventDto updateEvent(Long eventId, UpdateEventDtoRequest dto, MultipartFile[] images, Long userId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException(ErrorMessage.EVENT_NOT_FOUND_BY_ID + eventId));

        UserVO currentUser = userService.findById(userId);
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isAdmin && !event.getOrganizerId().equals(currentUser.getId())) {
            throw new UnauthorizedException(ErrorMessage.EVENT_EDIT_DENIED);
        }

        boolean past = event.getDateTimeLocations().stream()
            .anyMatch(d -> d.getFinishDate().isBefore(OffsetDateTime.now()));

        if (past) {
            throw new PastEventUpdateException(ErrorMessage.EVENT_PAST_EDIT_DENIED);
        }

        // Update basic fields
        event.setTitle(dto.getTitle().trim());
        event.setDescription(dto.getDescription().trim());
        event.setUpdatedAt(OffsetDateTime.now());

        // Update date/locations
        event.getDateTimeLocations().clear();
        dto.getDatesLocations().forEach(d -> {
            EventDateTimeLocation dtl = EventDateTimeLocation.builder()
                .event(event)
                .startDate(d.getStartDate())
                .finishDate(d.getFinishDate())
                .latitude(d.getLatitude())
                .longitude(d.getLongitude())
                .onlineLink(d.getOnlineLink())
                .updatedAt(OffsetDateTime.now())
                .build();
            event.getDateTimeLocations().add(dtl);
        });

        // Update images only if new ones provided
        if (images != null && images.length > 0) {
            // remove old images
            event.getImages().forEach(img -> imageStorageService.deleteImage(img.getImagePath()));
            event.getImages().clear();

            // save new images
            List<String> newPaths = imageStorageService.storeImages(images, eventId);
            IntStream.range(0, newPaths.size()).forEach(i -> {
                EventImage newImg = EventImage.builder()
                    .event(event)
                    .imagePath(newPaths.get(i))
                    .main(i == 0)
                    .createdAt(OffsetDateTime.now())
                    .build();
                event.getImages().add(newImg);
            });
        }

        return toEventDto(event);
    }

    /**
     * {@inheritDoc}
     *
     * @author Oleksandr Obydalo
     */
    private EventPreviewDto toEventPreviewDto(Event event) {
        return toEventPreviewDtoWithContext(event, null, false, null, null, null);
    }

    /**
     * {@inheritDoc}
     *
     * @author Oleksandr Obydalo.
     */
    private EventPreviewDto toEventPreviewDtoWithCanEdit(Event event, Long currentUserId, boolean isAdmin) {
        return toEventPreviewDtoWithContext(event, currentUserId, isAdmin, null, null, null);
    }

    private EventPreviewDto toEventPreviewDtoWithContext(
            Event event,
            Long currentUserId,
            boolean isAdmin,
            Double userLatitude,
            Double userLongitude,
            EventType eventType) {

        // Compute status and nearest times using shared calculator
        EventStatusCalculator.EventStatusResult statusResult =
                EventStatusCalculator.computeStatus(event.getDateTimeLocations(), OffsetDateTime.now());

        // Determine flags and ownership
        boolean isOrganizer = currentUserId != null && event.getOrganizerId() != null
                && event.getOrganizerId().equals(currentUserId);
        boolean canEdit = (isOrganizer || isAdmin) && statusResult.getStatus() != EventStatus.PASSED;
        boolean canCancelJoin = statusResult.getStatus() != EventStatus.LIVE
                && statusResult.getStatus() != EventStatus.PASSED;

        // Determine event types
        boolean hasPlace = event.getDateTimeLocations().stream()
                .anyMatch(loc -> loc.getLatitude() != null && loc.getLongitude() != null);
        boolean hasOnline = event.getDateTimeLocations().stream()
                .anyMatch(loc -> loc.getOnlineLink() != null && !loc.getOnlineLink().isBlank());

        EventTypesDto types = EventTypesDto.builder()
                .place(hasPlace)
                .online(hasOnline)
                .build();

        // Compute distance only when user coords provided (nullable)
        Double distance = computeMinDistanceKm(event, userLatitude, userLongitude,
                eventType != null ? eventType : EventType.BOTH);

        // Title image
        String titleImage = event.getImages().stream()
                .filter(EventImage::isMain)
                .findFirst()
                .map(EventImage::getImagePath)
                .orElse(null);

        return EventPreviewDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .titleImage(titleImage)
                .status(statusResult.getStatus())
                .nearestStart(statusResult.getNearestStart())
                .nearestFinish(statusResult.getNearestFinish())
                .types(types)
                .distance(distance)
                .visibility(event.isOpen() ? "open" : "closed")
                .canCancelJoin(canCancelJoin)
                .canEdit(canEdit)
                .isFavourite(false)
                .isSubscribed(false)
                .isOrganizer(isOrganizer)
                .build();
    }

    private Double computeMinDistanceKm(Event event, Double userLatitude, Double userLongitude, EventType eventType) {
        if (userLatitude == null || userLongitude == null) {
            return null;
        }
        if (eventType != EventType.PLACE) {
            return null;
        }
        List<EventDateTimeLocation> locations = event.getDateTimeLocations();
        if (locations == null || locations.isEmpty()) {
            return null;
        }
        Double min = null;
        for (EventDateTimeLocation loc : locations) {
            if (loc.getLatitude() == null || loc.getLongitude() == null) {
                continue;
            }
            double d = haversineKm(userLatitude, userLongitude, loc.getLatitude(), loc.getLongitude());
            if (min == null || d < min) {
                min = d;
            }
        }
        return min;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * {@inheritDoc}
     *
     * @author Oleksandr Obydalo.
     */
    private EventStatus determineEventStatus(OffsetDateTime nearestStart, OffsetDateTime finishDate) {
        if (nearestStart == null) {
            return EventStatus.PASSED;
        }

        OffsetDateTime now = OffsetDateTime.now();

        // Use actual finish date if available, otherwise treat as passed
        OffsetDateTime endTime = finishDate != null ? finishDate : nearestStart;

        if (now.isBefore(nearestStart)) {
            return EventStatus.UPCOMING;
        } else if (now.isAfter(nearestStart) && (finishDate == null || now.isBefore(endTime))) {
            return EventStatus.LIVE;
        } else {
            return EventStatus.PASSED;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @author Kateryna Holtvianska.
     */
    public EventDto getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found"));

        return toEventDto(event);
    }

    /**
     * {@inheritDoc}
     *
     * @author Kateryna Holtvianska.
     */
    @Override
    @Transactional
    public List<EventDto> getVisibleEvents(UserVO userVO) {
        List<Event> allEvents = eventRepository.findAll();

        return allEvents.stream()
            .filter(event -> event.isOpen() || isFriend(event.getOrganizerId(), userVO))
            .map(this::toEventDto)
            .toList();
    }

    /**
     * {@inheritDoc}
     *
     * @author Kateryna Holtvianska.
     */
    private boolean isFriend(Long organizerId, UserVO userVO) {
        // We need FriendService for this method to check friendship, but the
        // FriendService is not ready yet.
        // It is temporary solution
        return Objects.equals(userVO.getId(), organizerId);
    }

    /**
     * {@inheritDoc}
     *
     * @author Kateryna Holtvianska.
     */
    private EventDto toEventDto(Event event) {
        List<EventDateLocationDto> dateDtos = event.getDateTimeLocations().stream()
            .map(loc -> EventDateLocationDto.builder()
                .startDate(loc.getStartDate())
                .finishDate(loc.getFinishDate())
                .latitude(loc.getLatitude())
                .longitude(loc.getLongitude())
                .onlineLink(loc.getOnlineLink())
                .build())
            .toList();

        List<String> imageUrls = event.getImages().stream()
            .map(EventImage::getImagePath)
            .toList();

        // Compute event status based on date/time occurrences
        EventStatusCalculator.EventStatusResult statusResult =
                EventStatusCalculator.computeStatus(event.getDateTimeLocations(), OffsetDateTime.now());

        return EventDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .open(event.isOpen())
                .organizerId(event.getOrganizerId())
                .titleImage(event.getImages().stream()
                        .filter(EventImage::isMain)
                        .findFirst()
                        .map(EventImage::getImagePath)
                        .orElse(null))
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .datesLocations(dateDtos)
                .imageUrls(imageUrls)
                .status(statusResult.getStatus())
                .nearestStart(statusResult.getNearestStart())
                .nearestFinish(statusResult.getNearestFinish())
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * @author Kateryna Holtvianska & Oleksandr Obydalo & Andrii Zakordonskyi
     */
    private void validateEvent(EventRequest dto, MultipartFile[] images) {
        if (dto.getTitle() == null || dto.getTitle().isBlank() || dto.getTitle().length() > 70) {
            throw new BadRequestException("Title must be between 1 and 70 characters");
        }

        if (dto.getDescription() == null
                || dto.getDescription().length() < 20
                || dto.getDescription().length() > 63206) {
            throw new BadRequestException("Description must be between 20 and 63,206 characters");
        }

        if (dto.getDatesLocations() == null || dto.getDatesLocations().isEmpty()) {
            throw new BadRequestException("Event must have at least one date/location");
        }

        if (dto.getDatesLocations().size() > 7) {
            throw new BadRequestException("Event cannot have more than 7 date/time pairs");
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (EventDateLocationDto d : dto.getDatesLocations()) {
            if (d.getStartDate() == null || d.getFinishDate() == null) {
                throw new BadRequestException("Each event date must have start and finish times");
            }
            if (!d.getFinishDate().isAfter(d.getStartDate())) {
                throw new BadRequestException("Event finish time must be after start time");
            }
            if (!d.getStartDate().isAfter(now) || !d.getFinishDate().isAfter(now)) {
                throw new BadRequestException("Event dates must be in the future");
            }
            boolean hasCoords = d.getLatitude() != null && d.getLongitude() != null;
            boolean hasOnline = d.getOnlineLink() != null && !d.getOnlineLink().isBlank();
            if (!hasCoords && !hasOnline) {
                throw new BadRequestException("Each event must have either coordinates or an online link");
            }
        }

        if (images != null && images.length > 5) {
            throw new BadRequestException("Maximum 5 images allowed");
        }

        if (images != null) {
            for (MultipartFile img : images) {
                String filename = img.getOriginalFilename();
                if (filename == null || filename.isBlank()) {
                    throw new BadRequestException("Image filename is missing");
                }
                String name = filename.toLowerCase();
                if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))) {
                    throw new BadRequestException("Only JPG and PNG formats are allowed");
                }
                if (img.getSize() > 10 * 1024 * 1024) {
                    throw new BadRequestException("Image size must be ≤ 10 MB");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @author Oleksandr Braiko.
     */
    @Transactional
    @Override
    public void deleteEvent(Long id, UserVO user) {
        EventDto eventDto = findById(id);
        if (user.getRole() != Role.ROLE_ADMIN && !user.getId().equals(eventDto.getOrganizerId())) {
            throw new UnauthorizedException(ErrorMessage.USER_HAS_NO_PERMISSION);
        }
        eventImageRepository.deleteAllByEventId(id);
        dateTimeLocationRepository.deleteAllByEventId(id);

        entityManager.flush();
        entityManager.clear();

        eventRepository.deleteById(id);

        List<String> imagesToDelete = eventDto.getImageUrls() != null
                ? new ArrayList<>(eventDto.getImageUrls())
                : Collections.emptyList();

        imagesToDelete.forEach(imageStorageService::deleteImage);
    }

    /**
     * {@inheritDoc}
     *
     * @author Oleksandr Braiko.
     */
    @Override
    public EventDto findById(Long id) {
        Event event = eventRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.EVENT_NOT_FOUND_BY_ID + id));
        return toEventDto(event);
    }

    /**
     * {@inheritDoc}
     *
     * @author Oleksandr Obydalo
     */
    @Override
    @Transactional
    public boolean addAttender(Long eventId, UserVO user) {
        // Check if event exists
        eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.EVENT_NOT_FOUND_BY_ID + eventId));

        // Check if user is already an attender
        if (eventAttenderRepo.existsByEventIdAndUserId(eventId, user.getId())) {
            return false; // Already an attender
        }

        // Create new attender record
        EventAttender attender = EventAttender.builder()
                .eventId(eventId)
                .userId(user.getId())
                .createdAt(OffsetDateTime.now())
                .build();

        eventAttenderRepo.save(attender);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @author Oleksandr Obydalo
     */
    @Override
    @Transactional
    public boolean removeAttender(Long eventId, UserVO user) {
        // Check if event exists
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.EVENT_NOT_FOUND_BY_ID + eventId));

        // Check if event status is PASSED - disallow cancellation
        EventDto eventDto = toEventDto(event);
        if (eventDto.getStatus() == EventStatus.PASSED) {
            throw new BadRequestException(ErrorMessage.EVENT_CANT_UNATTEND_PAST);
        }

        // Check if user is an attender
        if (!eventAttenderRepo.existsByEventIdAndUserId(eventId, user.getId())) {
            return false; // Not an attender
        }

        // Remove attender
        int deletedCount = eventAttenderRepo.deleteByEventIdAndUserId(eventId, user.getId());
        return deletedCount > 0;
    }
}

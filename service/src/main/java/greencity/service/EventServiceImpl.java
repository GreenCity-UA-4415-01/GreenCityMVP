package greencity.service;

import greencity.constant.ErrorMessage;
import greencity.annotations.RatingCalculationEnum;
import greencity.constant.CacheConstants;
import greencity.constant.ErrorMessage;
import greencity.dto.user.UserVO;
import greencity.enums.Role;
import greencity.exception.exceptions.NotFoundException;
import greencity.exception.exceptions.UnauthorizedException;
import greencity.dto.user.UserVO;
import greencity.enums.Role;
import greencity.exception.exceptions.NotFoundException;
import greencity.exception.exceptions.UnauthorizedException;
import greencity.repository.EventAttenderRepo;
import greencity.enums.EventStatus;
import greencity.enums.EventType;
import greencity.repository.EventDateTimeLocationRepo;
import greencity.repository.EventImageRepo;
import greencity.repository.EventRepo;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import greencity.enums.EventStatus;
import greencity.enums.EventType;
import greencity.enums.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
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
import java.util.stream.Collectors;

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
    private final HttpServletRequest httpServletRequest;

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
            .collect(Collectors.toList());

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

    @Override
    @Transactional()
    public Page<EventPreviewDto> getMyEvents(Long userId, EventType eventType, Double userLatitude,
        Double userLongitude, Pageable pageable) {
        OffsetDateTime currentTime = OffsetDateTime.now();

        Page<Event> events;

        if (eventType != null && eventType != EventType.BOTH) {
            events = eventAttenderRepo.findJoinedEventsWithSorting(
                userId, currentTime, eventType.name(), userLatitude, userLongitude, pageable);
        } else {
            events = eventAttenderRepo.findJoinedEventsDefaultSorting(
                userId, currentTime, pageable);
        }

        List<EventPreviewDto> eventPreviews = events.getContent().stream()
            .map(this::toEventPreviewDto)
            .collect(Collectors.toList());

        return new PageImpl<>(eventPreviews, pageable, events.getTotalElements());
    }

    @Override
    @Transactional
    public Page<EventPreviewDto> getMyCreatedEvents(Long userId, Pageable pageable) {
        Page<Event> events = eventRepository.findByOrganizerIdOrderByNearestStart(userId, pageable);

        // Get current user to check roles
        UserVO currentUser = userService.findById(userId);
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;

        List<EventPreviewDto> eventPreviews = events.getContent().stream()
            .map(event -> toEventPreviewDtoWithCanEdit(event, userId, isAdmin))
            .collect(Collectors.toList());

        return new PageImpl<>(eventPreviews, pageable, events.getTotalElements());
    }

    private EventPreviewDto toEventPreviewDto(Event event) {
        // Find the nearest start date
        OffsetDateTime nearestStart = event.getDateTimeLocations().stream()
            .map(EventDateTimeLocation::getStartDate)
            .min(OffsetDateTime::compareTo)
            .orElse(null);

        // Find the corresponding finish date for the nearest start date
        OffsetDateTime nearestFinish = event.getDateTimeLocations().stream()
            .filter(loc -> loc.getStartDate().equals(nearestStart))
            .findFirst()
            .map(EventDateTimeLocation::getFinishDate)
            .orElse(null);

        // Determine event status using actual finish date
        EventStatus status = determineEventStatus(nearestStart, nearestFinish);

        // Get the first date location for coordinates and online link
        EventDateTimeLocation firstLocation = event.getDateTimeLocations().stream()
            .findFirst()
            .orElse(null);

        // Get main image
        String titleImage = event.getImages().stream()
            .filter(EventImage::isMain)
            .findFirst()
            .map(EventImage::getImagePath)
            .orElse(null);

        return EventPreviewDto.builder()
            .id(event.getId())
            .title(event.getTitle())
            .description(event.getDescription())
            .open(event.isOpen())
            .organizerId(event.getOrganizerId())
            .titleImage(titleImage)
            .createdAt(event.getCreatedAt())
            .updatedAt(event.getUpdatedAt())
            .status(status)
            .nearestStart(nearestStart)
            .canCancelJoin(status != EventStatus.LIVE && status != EventStatus.PASSED)
            .isFavourite(false) // TODO: Implement when favorites feature is added
            .isSubscribed(false) // TODO: Implement when subscription feature is added
            .visibility(event.isOpen() ? "PUBLIC" : "PRIVATE")
            .latitude(firstLocation != null ? firstLocation.getLatitude() : null)
            .longitude(firstLocation != null ? firstLocation.getLongitude() : null)
            .onlineLink(firstLocation != null ? firstLocation.getOnlineLink() : null)
            .build();
    }

    private EventPreviewDto toEventPreviewDtoWithCanEdit(Event event, Long currentUserId, boolean isAdmin) {
        // Find the nearest start date
        OffsetDateTime nearestStart = event.getDateTimeLocations().stream()
            .map(EventDateTimeLocation::getStartDate)
            .min(OffsetDateTime::compareTo)
            .orElse(null);

        // Find the corresponding finish date for the nearest start date
        OffsetDateTime nearestFinish = event.getDateTimeLocations().stream()
            .filter(loc -> loc.getStartDate().equals(nearestStart))
            .findFirst()
            .map(EventDateTimeLocation::getFinishDate)
            .orElse(null);

        // Determine event status using actual finish date
        EventStatus status = determineEventStatus(nearestStart, nearestFinish);

        // Get the first date location for coordinates and online link
        EventDateTimeLocation firstLocation = event.getDateTimeLocations().stream()
            .findFirst()
            .orElse(null);

        // Get main image
        String titleImage = event.getImages().stream()
            .filter(EventImage::isMain)
            .findFirst()
            .map(EventImage::getImagePath)
            .orElse(null);

        // Determine canEdit: true if user is organizer or admin
        boolean canEdit = event.getOrganizerId().equals(currentUserId) || isAdmin;

        return EventPreviewDto.builder()
            .id(event.getId())
            .title(event.getTitle())
            .description(event.getDescription())
            .open(event.isOpen())
            .organizerId(event.getOrganizerId())
            .titleImage(titleImage)
            .createdAt(event.getCreatedAt())
            .updatedAt(event.getUpdatedAt())
            .status(status)
            .nearestStart(nearestStart)
            .canCancelJoin(status != EventStatus.LIVE && status != EventStatus.PASSED)
            .canEdit(canEdit)
            .isFavourite(false) // TODO: Implement when favorites feature is added
            .isSubscribed(false) // TODO: Implement when subscription feature is added
            .visibility(event.isOpen() ? "PUBLIC" : "PRIVATE")
            .latitude(firstLocation != null ? firstLocation.getLatitude() : null)
            .longitude(firstLocation != null ? firstLocation.getLongitude() : null)
            .onlineLink(firstLocation != null ? firstLocation.getOnlineLink() : null)
            .build();
    }

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
            .collect(Collectors.toList());
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
            .collect(Collectors.toList());

        List<String> imageUrls = event.getImages().stream()
            .map(EventImage::getImagePath)
            .collect(Collectors.toList());

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
     * @author Kateryna Holtvianska & Oleksandr Obydalo.
     */
    private void validateEvent(AddEventDtoRequest dto, MultipartFile[] images) {
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
}

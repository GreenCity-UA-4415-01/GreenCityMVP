package greencity.service;

import greencity.repository.EventDateTimeLocationRepo;
import greencity.repository.EventImageRepo;
import greencity.repository.EventRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import greencity.dto.event.*;
import greencity.entity.*;
import greencity.exception.exceptions.BadRequestException;
import org.springframework.web.multipart.MultipartFile;
import org.modelmapper.ModelMapper;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepo eventRepository;
    private final EventDateTimeLocationRepo dateTimeLocationRepository;
    private final EventImageRepo eventImageRepository;
    private final ImageStorageService imageStorageService;
    private final ModelMapper mapper;

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
            .build();
    }

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
}

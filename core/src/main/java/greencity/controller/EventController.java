package greencity.controller;

import greencity.annotations.CurrentUser;
import greencity.dto.event.AddEventDtoRequest;
import greencity.dto.event.EventDto;
import greencity.dto.user.UserVO;
import greencity.exception.exceptions.BadRequestException;
import greencity.service.EventService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tika.Tika;
import java.io.IOException;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;
    private final Tika tika = new Tika();

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventDto> createEvent(
        @RequestPart("addEventDtoRequest") @Valid AddEventDtoRequest addEventDtoRequest,
        @RequestPart(value = "images", required = false) MultipartFile[] images,
        @Parameter(hidden = true) @CurrentUser UserVO currentUser) throws IOException {
        validateUser(currentUser);
        validateEventRequest(addEventDtoRequest);
        validateImages(images);

        EventDto created = eventService.createEvent(addEventDtoRequest, images, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private void validateUser(UserVO currentUser) {
        if (currentUser == null) {
            throw new BadRequestException("User must be authenticated to create an event.");
        }
    }

    private void validateEventRequest(AddEventDtoRequest addEventDtoRequest) {
        if (addEventDtoRequest.getTitle() == null || addEventDtoRequest.getTitle().isBlank()) {
            throw new BadRequestException("Title is required.");
        }
        if (addEventDtoRequest.getTitle().length() > 70) {
            throw new BadRequestException("Title length must not exceed 70 characters.");
        }
        if (addEventDtoRequest.getDescription() == null
            || addEventDtoRequest.getDescription().length() < 20
            || addEventDtoRequest.getDescription().length() > 63206) {
            throw new BadRequestException("Description must be between 20 and 63,206 characters.");
        }
        if (addEventDtoRequest.getDatesLocations() == null
            || addEventDtoRequest.getDatesLocations().isEmpty()
            || addEventDtoRequest.getDatesLocations().size() > 7) {
            throw new BadRequestException("Event must contain between 1 and 7 date/location entries.");
        }
    }

    private void validateImages(MultipartFile[] images) throws IOException {
        if (images != null) {
            if (images.length > 5) {
                throw new BadRequestException("A maximum of 5 images are allowed.");
            }
            for (MultipartFile image : images) {
                validateImage(image);
            }
        }
    }

    private void validateImage(MultipartFile image) throws IOException {
        // Use Apache Tika to detect actual file type based on content
        String detectedType = tika.detect(image.getInputStream());

        if (!detectedType.equals("image/jpeg") && !detectedType.equals("image/png")) {
            throw new BadRequestException("Invalid image format. Only JPEG, PNG are allowed.");
        }

        if (image.getSize() > 10 * 1024 * 1024) { // 10MB size limit
            throw new BadRequestException("Image size must not exceed 10MB.");
        }
    }
}

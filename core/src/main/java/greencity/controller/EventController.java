package greencity.controller;

import greencity.annotations.CurrentUser;
import greencity.constant.HttpStatuses;
import greencity.dto.event.AddEventDtoRequest;
import greencity.dto.event.EventDto;
import greencity.dto.event.EventPreviewDto;
import greencity.dto.user.UserVO;
import greencity.enums.EventType;
import greencity.exception.exceptions.BadRequestException;
import greencity.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    /**
     * Endpoint for event creation.
     * @param addEventDtoRequest
     * @param images
     * @param currentUser
     * @throws IOException
     */
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

    @GetMapping("/myEvents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<EventPreviewDto>> getMyEvents(
            @Parameter(hidden = true) @CurrentUser UserVO currentUser,
            @Parameter(hidden = true) @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(value = "eventType", required = false) EventType eventType,
            @RequestParam(value = "userLatitude", required = false) Double userLatitude,
            @RequestParam(value = "userLongitude", required = false) Double userLongitude) {

        validateUser(currentUser);

        Page<EventPreviewDto> events = eventService.getMyEvents(
                currentUser.getId(), eventType, userLatitude, userLongitude, pageable);

        return ResponseEntity.ok(events);
    }

    /**
     * Endpoint for event deletion.
     * @param eventId
     * @param user
     * @author Oleksandr Braiko
     */
    @Operation(summary = "Delete event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
            @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED),
            @ApiResponse(responseCode = "403", description = HttpStatuses.BAD_REQUEST),
            @ApiResponse(responseCode = "404", description = HttpStatuses.NOT_FOUND)
    })
    @DeleteMapping(value = "/{eventId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventDto> deleteEvent(@PathVariable Long eventId,
        @Parameter(hidden = true) @CurrentUser UserVO user
    ) {
        eventService.deleteEvent(eventId, user);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     *
     * @param currentUser
     * @author Oleksandr Obydalo
     */
    private void validateUser(UserVO currentUser) {
        if (currentUser == null) {
            throw new BadRequestException("User must be authenticated to create an event.");
        }
    }

    /**
     * Helper method to validate {@link AddEventDtoRequest}
     * @param addEventDtoRequest
     * @author Kateryna Holtvianska
     */
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

    /**
     * Helper method to validate quantity of uploaded images.
     * @param images
     * @throws IOException
     * @author Oleksandr Obydalo
     */
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

    /**
     * Helper method to validate a single image.
     * @param image
     * @throws IOException
     * @author Oleksandr Obydalo
     */
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

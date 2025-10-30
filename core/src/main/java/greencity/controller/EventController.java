package greencity.controller;

import greencity.annotations.CurrentUser;
import greencity.constant.HttpStatuses;
import greencity.dto.event.AddEventDtoRequest;
import greencity.dto.event.EventDto;
import greencity.dto.event.EventPreviewDto;
import greencity.dto.user.UserVO;
import greencity.enums.EventStatus;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tika.Tika;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;
    private final Tika tika = new Tika();

    /**
     * Endpoint for event creation.
     *
     * @param addEventDtoRequest DTO for event
     * @param images             images files for the event
     * @param currentUser        current user
     * @author Kateryna Holtvianska & Oleksandr Obydalo.
     */
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
            @ApiResponse(responseCode = "400", description = HttpStatuses.BAD_REQUEST)
    })
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

    @GetMapping("/visible")
    @Operation(summary = "Get events visible to the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
            @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    public ResponseEntity<List<EventDto>> getVisibleEvents(@AuthenticationPrincipal UserVO user) {
        return ResponseEntity.ok(eventService.getVisibleEvents(user));
    }

    /**
     * Endpoint for event deletion.
     *
     * @param eventId ID of the Event instance.
     * @param user    Current User.
     * @author Oleksandr Braiko
     */
    @Operation(summary = "Delete event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
            @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED),
            @ApiResponse(responseCode = "404", description = HttpStatuses.NOT_FOUND)
    })
    @DeleteMapping(value = "/delete/{eventId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId,
                                            @Parameter(hidden = true) @CurrentUser UserVO user) {
        eventService.deleteEvent(eventId, user);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Helper method to validate {@link AddEventDtoRequest}.
     *
     * @param addEventDtoRequest DTO under validation.
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
     *
     * @param images Images for the Event.
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
     *
     * @param image Image under validation.
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

    /**
     * Endpoint for events organized by the authenticated user.
     *
     * @param currentUser   User that is currently logged in.
     * @param pageable      Pageable.
     * @param eventType     Type of the event.
     * @param status        Event status filter (UPCOMING, LIVE, PASSED).
     * @param userLatitude  User coordinates.
     * @param userLongitude User coordinates.
     * @author Oleksandr Obydalo.
     */
    @GetMapping("/myEvents")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get events that the authenticated user has joined")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<EventPreviewDto>> getMyEvents(
            @Parameter(hidden = true) @CurrentUser UserVO currentUser,
            @Parameter(hidden = true) @PageableDefault(size = 10) Pageable pageable,
            @Parameter(description = "Filter by event type: ONLINE, PLACE, BOTH") @RequestParam(value = "eventType", required = false) EventType eventType,
            @Parameter(description = "Filter by status: UPCOMING, LIVE, PASSED") @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "User latitude for distance-based sorting (for PLACE events)") @RequestParam(value = "userLatitude", required = false) Double userLatitude,
            @Parameter(description = "User longitude for distance-based sorting (for PLACE events)") @RequestParam(value = "userLongitude", required = false) Double userLongitude) {
        validateUser(currentUser);

        Page<EventPreviewDto> events = eventService.getMyEvents(
            currentUser.getId(), eventType, parseEventStatus(status), userLatitude, userLongitude, pageable);

        return ResponseEntity.ok(events);
    }

    private EventStatus parseEventStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return EventStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                "Invalid event status: " + status + ". Allowed values are UPCOMING, LIVE, PASSED.");
        }
    }

    @GetMapping("/myEvents/createdEvents")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get events created by the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<EventPreviewDto>> getMyCreatedEvents(
            @Parameter(hidden = true) @CurrentUser UserVO currentUser,
            @Parameter(hidden = true) @PageableDefault(size = 10) Pageable pageable,
            @Parameter(description = "Filter by status: UPCOMING, LIVE, PASSED") @RequestParam(value = "status", required = false) String status) {
        validateUser(currentUser);

        Page<EventPreviewDto> events =
            eventService.getMyCreatedEvents(currentUser.getId(), parseEventStatus(status), pageable);

        return ResponseEntity.ok(events);
    }

    /**
     * Endpoint for getting all events related to the authenticated user. Returns
     * union of events created by user and events user has joined. Duplicates are
     * removed, preferring organizer view when user is both creator and attendee.
     *
     * @param currentUser User that is currently logged in.
     * @param pageable    Pageable.
     * @param status      Event status filter (UPCOMING, LIVE, PASSED).
     * @author Oleksandr Obydalo.
     */
    @GetMapping("/myEvents/relatedEvents")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all events related to the authenticated user (created and joined)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<EventPreviewDto>> getRelatedEvents(
            @Parameter(hidden = true) @CurrentUser UserVO currentUser,
            @Parameter(hidden = true) @PageableDefault(size = 10) Pageable pageable,
            @Parameter(description = "Filter by status: UPCOMING, LIVE, PASSED") @RequestParam(value = "status", required = false) String status) {
        validateUser(currentUser);

        Page<EventPreviewDto> events =
            eventService.getRelatedEvents(currentUser.getId(), parseEventStatus(status), pageable);

        return ResponseEntity.ok(events);
    }

    /**
     * Endpoint for removing an attender from an event.
     *
     * @param eventId     ID of the event
     * @param currentUser Current authenticated user
     * @return ResponseEntity with removal result
     * @author Generated
     */
    @DeleteMapping("/removeAttender/{eventId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove an attender from the event",
            description = "Cancel attendance for an upcoming or live event. Cannot cancel attendance for passed events.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully removed attender"),
            @ApiResponse(responseCode = "400", description = "Bad request - event has passed or user is not an attender"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<Map<String, Object>> removeAttender(
            @Parameter(description = "Event ID") @PathVariable Long eventId,
            @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        validateUser(currentUser);

        boolean removed = eventService.removeAttender(eventId, currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("removed", removed);

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint for adding an attender to an event.
     *
     * @param eventId     ID of the event
     * @param currentUser Current authenticated user
     * @return ResponseEntity with addition result
     * @author Generated
     */
    @PostMapping("/addAttender/{eventId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add an attender to the event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
            @ApiResponse(responseCode = "400", description = HttpStatuses.BAD_REQUEST),
            @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED),
            @ApiResponse(responseCode = "404", description = HttpStatuses.NOT_FOUND)
    })
    public ResponseEntity<Map<String, Object>> addAttender(
            @PathVariable Long eventId,
            @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        validateUser(currentUser);

        boolean added = eventService.addAttender(eventId, currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("added", added);

        return ResponseEntity.ok(response);
    }

    /**
     * Method to validate authentication of the User.
     *
     * @param currentUser User that is currently logged in.
     * @author Oleksandr Obydalo
     */
    private void validateUser(UserVO currentUser) {
        if (currentUser == null) {
            throw new BadRequestException("User must be authenticated.");
        }
    }
}

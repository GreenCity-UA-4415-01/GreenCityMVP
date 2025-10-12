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
import java.io.IOException;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventDto> createEvent(
            @RequestPart("addEventDtoRequest") @Valid AddEventDtoRequest addEventDtoRequest,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @Parameter(hidden = true) @CurrentUser UserVO currentUser) throws IOException {
        if (currentUser == null) {
            throw new BadRequestException("User must be authenticated to create an event.");
        }
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

        EventDto created = eventService.createEvent(addEventDtoRequest, images, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}

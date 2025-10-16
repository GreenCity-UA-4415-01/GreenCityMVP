package greencity.controller;

import greencity.constant.HttpStatuses;
import greencity.dto.newssubscriber.NewsSubscriberRequestDto;
import greencity.dto.newssubscriber.NewsSubscriberResponseDto;
import greencity.service.NewsSubscriberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@Validated
@RequestMapping("/newsSubscriber")
@Tag(name = "News Subscriber API", description = "API for subscribing and unsubscribing users to the newsletter")
public class NewsSubscriberController {
    private final NewsSubscriberService newsSubscriberService;

    /**
     * Endpoint for subscribing a user to the newsletter.
     *
     * @param newsSubscriberRequestDto DTO with user's email
     * @return {@link NewsSubscriberResponseDto} with email and unsubscribe token or
     *         status message
     */
    @Operation(
        summary = "Subscribe to the newsletter",
        description = "Subscribes a user by email and returns an unsubscribe token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = HttpStatuses.CREATED),
        @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
        @ApiResponse(responseCode = "400", description = HttpStatuses.BAD_REQUEST),
        @ApiResponse(responseCode = "500", description = HttpStatuses.INTERNAL_SERVER_ERROR)
    })
    @PostMapping
    public ResponseEntity<NewsSubscriberResponseDto> subscribe(
        @Parameter(description = "DTO containing the email to subscribe, "
            + "which will be used to generate"
            + "an unsubscribe token") @RequestBody @Valid NewsSubscriberRequestDto newsSubscriberRequestDto) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(newsSubscriberService.subscribe(newsSubscriberRequestDto));
    }

    /**
     * Endpoint for unsubscribing a user from the newsletter.
     *
     * @param email user's email
     * @param token unsubscribe token
     * @return {@link NewsSubscriberResponseDto} with email and status message
     */
    @Operation(
        summary = "Unsubscribe from the newsletter",
        description = "Unsubscribes a user using email and unsubscribe token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
        @ApiResponse(responseCode = "400", description = HttpStatuses.BAD_REQUEST),
        @ApiResponse(responseCode = "500", description = HttpStatuses.INTERNAL_SERVER_ERROR)
    })
    @GetMapping("/unsubscribe")
    public ResponseEntity<NewsSubscriberResponseDto> unsubscribe(
        @Parameter(description = "User's email") @RequestParam("email") String email,
        @Parameter(description = "Unsubscribe token") @RequestParam("unsubscribeToken") String token) {
        NewsSubscriberResponseDto response =
            newsSubscriberService.unsubscribe(email, token);
        return ResponseEntity.ok(response);
    }
}

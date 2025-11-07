package greencity.controller;

import greencity.annotations.ApiPageable;
import greencity.annotations.CurrentUser;
import greencity.constant.HttpStatuses;
import greencity.dto.PageableAdvancedDto;
import greencity.dto.notification.NotificationDto;
import greencity.dto.user.UserVO;
import greencity.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    /**
     * Retrieves all notifications for the authenticated user.
     * Notifications are ordered by creation date (newest first).
     * Only the authenticated user's own notifications are returned.
     *
     * @param currentUser the currently authenticated user
     * @param pageable    pagination parameters (page, size, sort)
     * @return page of notifications for the current user
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all notifications for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
            @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    @ApiPageable
    public ResponseEntity<PageableAdvancedDto<NotificationDto>> getAllNotifications(
            @Parameter(hidden = true) @CurrentUser UserVO currentUser,
            @Parameter(hidden = true) Pageable pageable) {
        PageableAdvancedDto<NotificationDto> notifications = notificationService.findAllByUserId(
                currentUser.getId(), pageable);
        return ResponseEntity.status(HttpStatus.OK).body(notifications);
    }

    /**
     * Marks a single notification as read for the authenticated user.
     * Only the notification owner can mark it as read.
     *
     * @param notificationId the ID of the notification to mark as read
     * @param currentUser    the currently authenticated user
     * @return the updated notification DTO
     */
    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark a notification as read")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
            @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED),
            @ApiResponse(responseCode = "404", description = HttpStatuses.NOT_FOUND)
    })
    public ResponseEntity<NotificationDto> markAsRead(
            @Parameter(description = "Id of the notification to mark as read") @PathVariable("id") Long notificationId,
            @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        NotificationDto updatedNotification = notificationService.markAsRead(notificationId, currentUser.getId());
        return ResponseEntity.status(HttpStatus.OK).body(updatedNotification);
    }

    /**
     * Marks all unread notifications as read for the authenticated user.
     *
     * @param currentUser the currently authenticated user
     * @return the number of notifications marked as read
     */
    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all notifications as read")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
            @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    public ResponseEntity<Integer> markAllAsRead(
            @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        int count = notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.status(HttpStatus.OK).body(count);
    }
}


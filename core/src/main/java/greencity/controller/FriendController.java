package greencity.controller;

import greencity.annotations.ApiPageableWithLocale;
import greencity.annotations.CurrentUser;
import greencity.constant.HttpStatuses;
import greencity.dto.PageableDto;
import greencity.dto.user.UserFriendCardDto;
import greencity.dto.user.UserVO;
import greencity.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {
    private final FriendService friendService;

    @Operation(summary = "Find all users that are not friend for current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
        @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    @GetMapping("/not-friends-yet")
    @ApiPageableWithLocale
    public ResponseEntity<PageableDto<UserFriendCardDto>> findNotFriendsYet(
        @RequestParam(defaultValue = "") String name,
        Pageable pageable,
        @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        PageableDto<UserFriendCardDto> result = friendService.search(currentUser.getId(), name, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Add new user friend (send request)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
        @ApiResponse(responseCode = "400", description = HttpStatuses.BAD_REQUEST),
        @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    @PostMapping("/{friendId}")
    public ResponseEntity<Void> sendFriendRequest(
        @PathVariable Long friendId,
        @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        friendService.sendFriendRequest(currentUser.getId(), friendId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Cancel friend request")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
        @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    @DeleteMapping("/{friendId}/cancel-request")
    public ResponseEntity<Void> cancelRequest(
        @PathVariable Long friendId,
        @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        friendService.cancelFriendRequest(currentUser.getId(), friendId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Unfriend another user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
        @ApiResponse(responseCode = "400", description = HttpStatuses.BAD_REQUEST),
        @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED),
        @ApiResponse(responseCode = "404", description = HttpStatuses.NOT_FOUND)
    })
    @DeleteMapping("/{friendId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unfriendUser(
        @PathVariable Long friendId,
        @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        friendService.unfriendUser(currentUser.getId(), friendId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Accept friend request from friendId")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
        @ApiResponse(responseCode = "400", description = HttpStatuses.BAD_REQUEST),
        @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    @PatchMapping("/{friendId}/acceptFriend")
    public ResponseEntity<Void> accept(
        @PathVariable Long friendId,
        @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        friendService.acceptFriendRequest(currentUser.getId(), friendId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reject friend request from requesterId")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
        @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    @DeleteMapping("/{friendId}/declineFriend")
    public ResponseEntity<Void> reject(
        @PathVariable Long friendId,
        @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        friendService.rejectFriendRequest(currentUser.getId(), friendId);
        return ResponseEntity.ok().build();
    }
}

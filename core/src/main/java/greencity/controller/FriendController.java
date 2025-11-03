package greencity.controller;

import greencity.annotations.ApiPageableWithLocale;
import greencity.annotations.CurrentUser;
import greencity.constant.HttpStatuses;
import greencity.dto.PageableDto;
import greencity.dto.user.*;
import greencity.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
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
    public ResponseEntity<PageableDto<UserFriendCandidateCardDto>> findNotFriendsYet(
        @RequestParam(defaultValue = "") String name,
        Pageable pageable,
        @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        PageableDto<UserFriendCandidateCardDto> result = friendService.search(currentUser.getId(), name, pageable);
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

    @Operation(summary = "Reject friend request from friendId")
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

    @ApiPageableWithLocale
    @Operation(summary = "Get paged list of my friends (tab: All friends)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
        @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    @GetMapping
    public ResponseEntity<PageableDto<UserFriendCardDto>> myFriends(
        Pageable pageable,
        @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        log.warn(pageable.toString());
        return ResponseEntity.ok(friendService.listFriends(currentUser.getId(), pageable));
    }

    @GetMapping("/top6")
    @Operation(summary = "Get up to 6 most relevant friends for My Habits widget")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
        @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    public ResponseEntity<List<FriendShortDto>> top6(
        @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        return ResponseEntity.ok(friendService.topFriends(currentUser.getId()));
    }

    @Operation(summary = "Get friend profile (for friend card/avatar click)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
        @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<FriendProfileDto> friendProfile(
        @PathVariable Long userId,
        @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        return ResponseEntity.ok(friendService.friendProfile(currentUser.getId(), userId));
    }

    @Operation(summary = "Get friend requests for the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
        @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    @GetMapping("/friendRequests")
    public ResponseEntity<PageableDto<UserFriendCandidateCardDto>> friendRequests(
        Pageable pageable,
        @Parameter(hidden = true) @CurrentUser UserVO currentUser) {
        return ResponseEntity.ok(friendService.friendRequests(currentUser.getId(), pageable));
    }
}

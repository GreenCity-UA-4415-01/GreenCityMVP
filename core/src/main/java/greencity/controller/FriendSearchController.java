package greencity.controller;

import greencity.annotations.ApiPageableWithLocale;
import greencity.annotations.ValidLanguage;
import greencity.constant.HttpStatuses;
import greencity.dto.PageableDto;
import greencity.dto.user.UserFriendCardDto;
import greencity.security.jwt.JwtTool;
import greencity.service.FriendSearchService;
import greencity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendSearchController {
    private final FriendSearchService friendSearchService;
    private final JwtTool jwtTool;
    private final UserService userService;

    private Long currentUserId(HttpServletRequest req) {
        String token = jwtTool.getTokenFromHttpServletRequest(req);
        String email = jwtTool.getEmailOutOfAccessToken(token);
        return userService.findIdByEmail(email);
    }

    @Operation(summary = "Find all users that are not friend for current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
            @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    @GetMapping("/not-friends-yet")
    @ApiPageableWithLocale
    public ResponseEntity<PageableDto<UserFriendCardDto>> findNotFriendsYet(
            @RequestParam(defaultValue = "") String name,
            @ValidLanguage Locale locale,
            Pageable pageable,
            HttpServletRequest request) {

        Long me = currentUserId(request);
        PageableDto<UserFriendCardDto> result = friendSearchService.search(me, name, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Add new user friend (send request)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
            @ApiResponse(responseCode = "400", description = HttpStatuses.BAD_REQUEST),
            @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    @PostMapping("/{friendId}")
    public ResponseEntity<Void> sendFriendRequest(
            @PathVariable Long friendId,
            HttpServletRequest request) {
        Long me = currentUserId(request);
        friendSearchService.sendFriendRequest(me, friendId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Cancel friend request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = HttpStatuses.OK),
            @ApiResponse(responseCode = "401", description = HttpStatuses.UNAUTHORIZED)
    })
    @DeleteMapping("/{friendId}/cancel-request")
    public ResponseEntity<Void> cancelRequest(
            @PathVariable Long friendId,
            HttpServletRequest request) {
        Long me = currentUserId(request);
        friendSearchService.cancelFriendRequest(me, friendId);
        return ResponseEntity.ok().build();
    }
}

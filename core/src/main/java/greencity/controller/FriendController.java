package greencity.controller;

import greencity.security.jwt.JwtTool;
import greencity.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {
    private final JwtTool jwtTool;
    private final UserService userService;      // у вас це вже є в проекті
    private final greencity.service.FriendService friendService;

    /** Дістаємо id поточного користувача з access-токена */
    private Long currentUserId(HttpServletRequest req) {
        String token = jwtTool.getTokenFromHttpServletRequest(req);
        if (token == null) {
            throw new RuntimeException("Missing Authorization header");
        }
        // (опційно) перевірка підпису access-токена:
        if (!jwtTool.isTokenValid(token, jwtTool.getAccessTokenKey())) {
            throw new RuntimeException("Invalid access token");
        }
        String email = jwtTool.getEmailOutOfAccessToken(token);
        return userService.findIdByEmail(email); // у вас такий метод є/легко додати в UserService
    }

    // ------------ ЕНДПОІНТИ ---------------

    /** GET /friends — всі друзча (пі поточного користуваоки що повертаємо Page<Long> з id) */
    @GetMapping
    public Page<Long> findMyFriends(Pageable pageable, HttpServletRequest req) {
        Long me = currentUserId(req);
        return friendService.findFriendIds(me, pageable);
    }

    /** GET /friends/friendRequests — вхідні запити у друзі */
    @GetMapping("/friendRequests")
    public Page<Long> incomingRequests(Pageable pageable, HttpServletRequest req) {
        Long me = currentUserId(req);
        return friendService.findIncomingRequestUserIds(me, pageable);
    }

    /** GET /friends/not-friends-yet — кого ще не додано в друзі */
    @GetMapping("/not-friends-yet")
    public Page<Long> notFriendsYet(@RequestParam(required = false) String name,
                                    Pageable pageable,
                                    HttpServletRequest req) {
        Long me = currentUserId(req);
        return friendService.findNotFriendsYetUserIds(me, name, pageable);
    }

    /** POST /friends/{friendId} — надіслати запит у друзі */
    @PostMapping("/{friendId}")
    public ResponseEntity<Void> addFriend(@PathVariable Long friendId, HttpServletRequest req) {
        Long me = currentUserId(req);
        friendService.addFriendRequest(me, friendId);
        return ResponseEntity.ok().build();
    }

    /** PATCH /friends/{friendId}/acceptFriend — прийняти запит */
    @PatchMapping("/{friendId}/acceptFriend")
    public ResponseEntity<Void> accept(@PathVariable Long friendId, HttpServletRequest req) {
        Long me = currentUserId(req);
        friendService.acceptFriendRequest(me, friendId);
        return ResponseEntity.ok().build();
    }

    /** DELETE /friends/{friendId}/declineFriend — відхилити запит */
    @DeleteMapping("/{friendId}/declineFriend")
    public ResponseEntity<Void> decline(@PathVariable Long friendId, HttpServletRequest req) {
        Long me = currentUserId(req);
        friendService.declineFriendRequest(me, friendId);
        return ResponseEntity.ok().build();
    }

    /** DELETE /friends/{friendId} — видалити друга */
    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> delete(@PathVariable Long friendId, HttpServletRequest req) {
        Long me = currentUserId(req);
        friendService.deleteFriend(me, friendId);
        return ResponseEntity.ok().build();
    }
}
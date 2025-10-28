package greencity.service;

import greencity.dto.PageableDto;
import greencity.dto.user.UserFriendCardDto;
import org.springframework.data.domain.Pageable;

public interface FriendSearchService {
    PageableDto<UserFriendCardDto> search(Long me, String query, Pageable pageable);
    void sendFriendRequest(Long me, Long friendId);   // для POST /friends/{friendId}
    void cancelFriendRequest(Long me, Long friendId); // опційно (кнопка “cancel request”)
}

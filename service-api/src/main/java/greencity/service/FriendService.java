package greencity.service;

import greencity.dto.PageableDto;
import greencity.dto.user.UserFriendCardDto;
import org.springframework.data.domain.Pageable;

public interface FriendService {
    PageableDto<UserFriendCardDto> search(Long me, String query, Pageable pageable);

    void sendFriendRequest(Long me, Long friendId); // для POST /friends/{friendId}

    void cancelFriendRequest(Long me, Long friendId); // опційно (кнопка “cancel request”)

    void acceptFriendRequest(Long me, Long requesterId);
    void rejectFriendRequest(Long me, Long requesterId);

}

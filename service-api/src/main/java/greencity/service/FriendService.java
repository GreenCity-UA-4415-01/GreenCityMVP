package greencity.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FriendService {
    Page<Long> findFriendIds(Long me, Pageable pageable);
    Page<Long> findIncomingRequestUserIds(Long me, Pageable pageable);
    Page<Long> findNotFriendsYetUserIds(Long me, String name, Pageable pageable);

    void addFriendRequest(Long me, Long friendId);
    void acceptFriendRequest(Long me, Long friendId);
    void declineFriendRequest(Long me, Long friendId);
    void deleteFriend(Long me, Long friendId);
}

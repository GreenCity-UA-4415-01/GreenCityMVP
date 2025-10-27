package greencity.service;

import greencity.dto.PageableDto;
import greencity.dto.user.UserFriendCardDto;
import org.springframework.data.domain.Pageable;

public interface FriendSearchService {
    PageableDto<UserFriendCardDto> searchCandidates(
            Long me,
            String query,                 // name / username (1..30, letters . and space)
            Pageable pageable,            // size=10 for infinite scroll
            Boolean sameCity,             // optional filter
            Boolean friendsOfFriends      // optional filter
    );

    void sendFriendRequest(Long me, Long friendId);

    void cancelFriendRequest(Long me, Long friendId);
}

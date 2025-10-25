package greencity.dto.user;

import greencity.enums.FriendshipStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFriendDto {
    private Long id;               // id друга
    private String name;           // імʼя/нік
    private String profilePicture; // шлях до аватарки (якщо є)
    private FriendshipStatus status; // PENDING/ACCEPTED/... (для списків заявок/друзів)
}
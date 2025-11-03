package greencity.dto.user;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFriendCardDto {
    private Long id;
    private String name;
    private String profilePicture;
    private String city;
    private Double personalRate;
    private Long mutualFriends;
}

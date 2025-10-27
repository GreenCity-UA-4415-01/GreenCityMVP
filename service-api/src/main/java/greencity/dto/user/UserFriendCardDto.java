package greencity.dto.user;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFriendCardDto {
    private Long id;              // candidate user id
    private String name;          // display name
    private String username;      // optional if present in model
    private String avatar;        // avatar url/path
    private String city;          // city name
    private Integer personalRate; // personal rating
    private Integer mutualFriends;// how many mutual friends with me
    private boolean requestSent;  // have I already sent a pending request to this user
}

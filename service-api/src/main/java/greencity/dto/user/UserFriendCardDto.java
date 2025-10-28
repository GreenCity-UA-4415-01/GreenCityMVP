package greencity.dto.user;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFriendCardDto {
    private Long id;               // candidate user id
    private String name;           // display name
    private String profilePicture; // users.profile_picture
    private String city;           // users.city
    private Double personalRate;   // users.rating
    private Long mutualFriends;    // computed later
    private Boolean requestSent;   // true if there is a pending request from me to this user
}

package greencity.dto.user;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FriendShortDto {
    private Long id;
    private String name;
    private String profilePicture;
    private Boolean online;
}

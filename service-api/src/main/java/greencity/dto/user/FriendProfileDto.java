package greencity.dto.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendProfileDto {
    private Long id;
    private String name;
    private String profilePicture;
    private Boolean online;
    private Double personalRate;
    private String credo;
    // private Integer habitsInProgress;
    // private Integer habitsAcquired;
    // private Integer tipsAndTricksCount;
    // private Integer newsCount;
    private String city;
}

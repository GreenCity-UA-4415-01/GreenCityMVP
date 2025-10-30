package greencity.entity;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FriendRequestKey implements Serializable {
    private Long requesterId;
    private Long receiverId;
}
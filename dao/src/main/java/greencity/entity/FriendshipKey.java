package greencity.entity;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FriendshipKey implements Serializable {
    private Long userId;
    private Long friendId;
}

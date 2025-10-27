package greencity.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "friendships")
@IdClass(FriendshipKey.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friendship {
    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "friend_id", nullable = false)
    private Long friendId;
}

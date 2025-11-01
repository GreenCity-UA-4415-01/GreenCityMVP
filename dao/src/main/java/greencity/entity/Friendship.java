package greencity.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "friendships",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_friendships_user_friend", columnNames = {"user_id", "friend_id"})
    },
    indexes = {
        @Index(name = "idx_friendships_user", columnList = "user_id"),
        @Index(name = "idx_friendships_friend", columnList = "friend_id")
    })
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

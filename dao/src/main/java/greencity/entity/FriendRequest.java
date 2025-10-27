package greencity.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "friendship_requests")
@IdClass(FriendRequestKey.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendRequest {
    @Id
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Id
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "status", nullable = false, length = 10)
    private String status; // PENDING / ACCEPTED / REJECTED / BLOCKED
}

package greencity.entity;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EventAttenderId implements Serializable {
    private Long eventId;
    private Long userId;
}
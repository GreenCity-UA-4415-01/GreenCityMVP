package greencity.dto.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
public class UpdateEventDtoRequest implements EventRequest {
    @NotBlank
    @Size(max = 70)
    private String title;

    @NotBlank
    @Size(min = 20, max = 63206)
    private String description;

    @NotEmpty
    @Size(min = 1, max = 7)
    @Valid
    private List<EventDateLocationDto> datesLocations;
}

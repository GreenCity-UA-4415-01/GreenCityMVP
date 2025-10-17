package greencity.dto.event;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
public class AddEventDtoRequest {
    @NotBlank
    @Size(max = 70)
    private String title;

    @NotBlank
    @Size(min = 20, max = 63206)
    private String description;

    private boolean open;

    @NotEmpty
    @Size(min = 1, max = 7)
    @Valid
    private List<EventDateLocationDto> datesLocations;
}

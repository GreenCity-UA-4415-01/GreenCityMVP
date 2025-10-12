package greencity.dto.newssubscriber;

import greencity.constant.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsSubscriberRequestDto {
    @NotBlank
    @Email(
        regexp = ValidationConstants.VALIDATION_EMAIL,
        message = "{greenCity.validation.invalid.email}")
    private String email;
}

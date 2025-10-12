package greencity.dto.newssubscriber;

import java.io.Serializable;
import greencity.constant.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NewsSubscriberResponseDto implements Serializable {
    @NotBlank
    @Email(regexp = ValidationConstants.VALIDATION_EMAIL)
    private String email;
    @NotBlank
    private String unsubscribeToken;
}

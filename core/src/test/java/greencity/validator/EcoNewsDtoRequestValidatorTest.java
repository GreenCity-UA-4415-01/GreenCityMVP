package greencity.validator;

import greencity.dto.econews.AddEcoNewsDtoRequest;
import greencity.exception.exceptions.WrongCountOfTagsException;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.mockito.Mockito.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static greencity.ModelUtils.getAddEcoNewsDtoRequest;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EcoNewsDtoRequestValidatorTest {
    @InjectMocks
    private EcoNewsDtoRequestValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    private static final int TEST_MAX_TAGS = 3;

    @BeforeEach
    void setUp() {
        validator.initialize(null);
    }

    private AddEcoNewsDtoRequest getAddEcoNewsDtoRequest() {
        AddEcoNewsDtoRequest request = new AddEcoNewsDtoRequest();
        request.setTitle("Valid Title");
        request.setText("This is a valid text and it is definitely longer than 20 characters.");

        request.setTags(Arrays.asList("eco", "news"));

        request.setSource(null);
        return request;
    }

    @Test
    void isValidTrueTest_WithUrl() {
        AddEcoNewsDtoRequest request = getAddEcoNewsDtoRequest();
        request.setSource("https://eco-lavca.ua/");

        assertTrue(validator.isValid(request, context),
            "The validator should return true if the DTO is valid and the Source exists.");
    }

    @Test
    void isValidTrueTest_WithNullSource() {
        AddEcoNewsDtoRequest request = getAddEcoNewsDtoRequest();
        request.setSource(null);

        assertTrue(validator.isValid(request, context),
            "The validator should return true if Source is missing.");
    }

    @Test
    void isValidTrueTest_WithEmptySource() {
        AddEcoNewsDtoRequest request = getAddEcoNewsDtoRequest();
        request.setSource("");

        assertTrue(validator.isValid(request, context),
            "The validator should return true if the Source is empty.");
    }

    @Test
    void isValidTrueTest_MaxAmountOfTags() {
        List<String> maxTags = IntStream.range(0, TEST_MAX_TAGS)
            .mapToObj(i -> "Tag" + i)
            .collect(Collectors.toList());

        AddEcoNewsDtoRequest request = new AddEcoNewsDtoRequest();
        request.setSource(null);
        request.setTags(maxTags);

        assertTrue(validator.isValid(request, context),
            "The validator should return true for the maximum number of tags.");
    }

    @Test
    void isValid_ShouldThrowException_WhenTagsListIsEmpty() {
        AddEcoNewsDtoRequest request = new AddEcoNewsDtoRequest();
        request.setSource(null);
        request.setTags(Collections.emptyList());

        assertThrows(WrongCountOfTagsException.class,
            () -> validator.isValid(request, context),
            "A WrongCountOfTagsException should be thrown when the tag list is empty.");
    }

    @Test
    void isValid_ShouldThrowException_WhenTagsListExceedsMaxSize() {
        List<String> tooManyTags = IntStream.range(0, TEST_MAX_TAGS + 1)
            .mapToObj(i -> "Tag" + i)
            .collect(Collectors.toList());

        AddEcoNewsDtoRequest request = new AddEcoNewsDtoRequest();
        request.setSource("http://any.url");
        request.setTags(tooManyTags);

        assertThrows(WrongCountOfTagsException.class,
            () -> validator.isValid(request, context),
            "A WrongCountOfTagsException should be thrown when the tag list exceeds the limit.");
    }

}
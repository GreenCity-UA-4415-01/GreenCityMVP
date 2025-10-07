package greencity.validator;

import greencity.service.LanguageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LanguageValidatorTest {
    @Mock
    private LanguageService languageService;

    @InjectMocks
    private LanguageValidator languageValidator;

    @BeforeEach
    void setUp() {
        List<String> supportedCodes = Arrays.asList("en", "ua");
        when(languageService.findAllLanguageCodes()).thenReturn(supportedCodes);
        languageValidator.initialize(null);
    }

    @Test
    void isValid_ReturnsTrue_ForSupportedLanguage() {
        Locale locale = new Locale("en");

        boolean result = languageValidator.isValid(locale, null);

        assertTrue(result);
    }

    @Test
    void isValid_ReturnsFalse_ForUnsupportedLanguage() {
        Locale locale = new Locale("fr");

        boolean result = languageValidator.isValid(locale, null);

        assertFalse(result);
    }

    @Test
    void isValid_ThrowsException_WhenLocaleIsNull() {
        assertThrows(NullPointerException.class,
            () -> languageValidator.isValid(null, null));
    }
}
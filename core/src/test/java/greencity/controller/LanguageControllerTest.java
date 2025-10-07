package greencity.controller;

import greencity.service.LanguageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LanguageControllerTest {
    private MockMvc mockMvc;

    @InjectMocks
    private LanguageController languageController;

    @Mock
    private LanguageService languageService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(languageController).build();
    }

    @Test
    void getAllLanguageCodes_ReturnsListOfLanguages() throws Exception {
        when(languageService.findAllLanguageCodes()).thenReturn(Arrays.asList("en", "ua", "fr"));

        mockMvc.perform(get("/language")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"en\",\"ua\",\"fr\"]"));

        verify(languageService).findAllLanguageCodes();
    }

    @Test
    void getAllLanguageCodes_ReturnsEmptyList() throws Exception {
        when(languageService.findAllLanguageCodes()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/language")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(languageService).findAllLanguageCodes();
    }
}
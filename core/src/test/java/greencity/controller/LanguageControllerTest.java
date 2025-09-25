package greencity.controller;

import greencity.service.LanguageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LanguageController.class)
@ContextConfiguration(classes = {LanguageController.class})
@AutoConfigureMockMvc(addFilters = false)
class LanguageControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LanguageService languageService;

    @Test
    void getAllLanguageCodes_ReturnsListOfLanguages() throws Exception {
        when(languageService.findAllLanguageCodes()).thenReturn(Arrays.asList("en", "ua", "fr"));

        mockMvc.perform(get("/language")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"en\",\"ua\",\"fr\"]"));

        verify(languageService).findAllLanguageCodes();
    }

    @Test
    void getAllLanguageCodes_ReturnsEmptyList() throws Exception {
        when(languageService.findAllLanguageCodes()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/language")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(languageService).findAllLanguageCodes();
    }
}
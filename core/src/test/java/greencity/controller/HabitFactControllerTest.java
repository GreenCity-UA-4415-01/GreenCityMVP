package greencity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import greencity.config.SecurityConfig;
import greencity.dto.PageableDto;
import greencity.dto.habitfact.HabitFactDtoResponse;
import greencity.dto.habitfact.HabitFactPostDto;
import greencity.dto.habitfact.HabitFactUpdateDto;
import greencity.dto.habitfact.HabitFactVO;
import greencity.dto.language.LanguageTranslationDTO;
import greencity.dto.user.HabitIdRequestDto;
import greencity.exception.exceptions.BadRequestException;
import greencity.exception.handler.CustomExceptionHandler;
import greencity.service.HabitFactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.modelmapper.ModelMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@ContextConfiguration
@Import(SecurityConfig.class)
public class HabitFactControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private HabitFactController habitFactController;

    @Mock
    private HabitFactService habitFactService;

    @Mock
    private Validator mockValidator;

    private ObjectMapper objectMapper;

    private ErrorAttributes errorAttributes = new DefaultErrorAttributes();

    @Mock
    private ModelMapper mapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        this.mockMvc = MockMvcBuilders.standaloneSetup(habitFactController)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setValidator(mockValidator)
            .setControllerAdvice(new CustomExceptionHandler(errorAttributes, objectMapper))
            .build();
    }

    @Test
    public void getRandomFactByHabitId_ShouldReturn200() throws Exception {
        LanguageTranslationDTO response = new LanguageTranslationDTO();
        response.setContent("Random fact");

        when(habitFactService.getRandomHabitFactByHabitIdAndLanguage(eq(1L), eq("en")))
            .thenReturn(response);

        this.mockMvc.perform(get("/facts/random/{habitId}", 1L)
            .accept(MediaType.APPLICATION_JSON)
            .locale(Locale.ENGLISH))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(habitFactService).getRandomHabitFactByHabitIdAndLanguage(1L, "en");
    }

    @Test
    public void getHabitFactOfTheDay_ShouldReturn200() throws Exception {
        LanguageTranslationDTO response = new LanguageTranslationDTO();
        response.setContent("Fact of the day");

        when(habitFactService.getHabitFactOfTheDay(1L)).thenReturn(response);

        this.mockMvc.perform(get("/facts/dayFact/{languageId}", 1L)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(habitFactService).getHabitFactOfTheDay(1L);
    }

    @Test
    public void getAll_ShouldReturn200() throws Exception {
        LanguageTranslationDTO res1 = new LanguageTranslationDTO();
        res1.setContent("Fact #1");
        LanguageTranslationDTO res2 = new LanguageTranslationDTO();
        res2.setContent("Fact #2");

        PageableDto<LanguageTranslationDTO> pageableDto = new PageableDto<>(
            List.of(res1, res2), 2, 0, 1);

        when(habitFactService.getAllHabitFacts(any(Pageable.class), eq("en"))).thenReturn(pageableDto);

        this.mockMvc.perform(get("/facts")
            .param("page", "1")
            .param("size", "10")
            .locale(Locale.ENGLISH)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.currentPage").value(0))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.page[0].content").value("Fact #1"))
            .andExpect(jsonPath("$.page[1].content").value("Fact #2"));

        verify(habitFactService).getAllHabitFacts(any(Pageable.class), eq("en"));
    }

    @Test
    public void saveFact_ShouldReturn201Created() throws Exception {
        HabitIdRequestDto habitIdRequestDto = new HabitIdRequestDto();
        habitIdRequestDto.setId(1L);
        HabitFactPostDto habitFactPostDto = new HabitFactPostDto();
        habitFactPostDto.setHabit(habitIdRequestDto);
        HabitFactVO savedVo = new HabitFactVO();
        savedVo.setId(100L);
        HabitFactDtoResponse responseDto = new HabitFactDtoResponse();
        responseDto.setId(100L);

        when(habitFactService.save(any(HabitFactPostDto.class))).thenReturn(savedVo);
        when(mapper.map(savedVo, HabitFactDtoResponse.class)).thenReturn(responseDto);

        mockMvc.perform(post("/facts")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(habitFactPostDto)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(100L));

        verify(habitFactService).save(any(HabitFactPostDto.class));
        verify(mapper).map(savedVo, HabitFactDtoResponse.class);
    }

    @Test
    public void updateFact_ShouldReturn200() throws Exception {
        HabitIdRequestDto habitIdRequestDto = new HabitIdRequestDto();
        habitIdRequestDto.setId(2L);
        HabitFactUpdateDto dto = new HabitFactUpdateDto();
        dto.setHabit(habitIdRequestDto);
        HabitFactVO updatedVo = new HabitFactVO();
        updatedVo.setId(2L);
        HabitFactPostDto postDto = new HabitFactPostDto();
        postDto.setHabit(habitIdRequestDto);

        when(habitFactService.update(any(HabitFactUpdateDto.class), eq(2L))).thenReturn(updatedVo);
        when(mapper.map(updatedVo, HabitFactPostDto.class)).thenReturn(postDto);

        mockMvc.perform(put("/facts/{id}", 2L)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.habit.id").value(2L));

        verify(habitFactService).update(any(HabitFactUpdateDto.class), eq(2L));
        verify(mapper).map(updatedVo, HabitFactPostDto.class);
    }

    @Test
    public void deleteFact_ShouldReturn200() throws Exception {
        HabitFactVO deletedVO = new HabitFactVO();
        deletedVO.setId(1L);

        when(habitFactService.delete(1L)).thenReturn(deletedVO.getId());

        mockMvc.perform(delete("/facts/{id}", 1L))
            .andExpect(status().isOk());

        verify(habitFactService).delete(1L);
    }

    @Test
    public void deleteFact_ShouldReturn404() throws Exception {
        when(habitFactService.delete(99L)).thenThrow(new BadRequestException("Invalid habit fact id!"));

        mockMvc.perform(delete("/facts/{id}", 99L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(habitFactService).delete(99L);
    }
}

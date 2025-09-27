package greencity.controller;

import greencity.converters.UserArgumentResolver;
import greencity.dto.PageableDto;
import greencity.dto.habit.AddCustomHabitDtoRequest;
import greencity.dto.habit.AddCustomHabitDtoResponse;
import greencity.dto.habit.HabitDto;

import greencity.dto.user.UserProfilePictureDto;
import greencity.dto.user.UserVO;
import greencity.exception.handler.CustomExceptionHandler;
import greencity.service.HabitService;

import greencity.service.TagsService;
import greencity.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.security.Principal;
import java.util.List;
import java.util.Locale;

import java.util.Set;

import static greencity.ModelUtils.getPrincipal;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class HabitControllerTest {
    public static final String habitLink = "/habit";
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @InjectMocks
    private HabitController habitController;

    @Mock
    private HabitService habitService;

    @Mock
    private TagsService tagsService;

    @Mock
    private UserService userService;

    @Mock
    private ModelMapper modelMapper;

    private Principal principal = getPrincipal();
    private final ErrorAttributes errorAttributes = new DefaultErrorAttributes();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(habitController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(),
                        new UserArgumentResolver(null, null))
                .setControllerAdvice(new CustomExceptionHandler(errorAttributes, objectMapper))
                .build();
    }

    @Test
    void getHabitByIdTest() throws Exception {
        mockMvc.perform(get(habitLink + "/{id}", 1L)
                        .locale(Locale.ENGLISH))
                .andExpect(status().isOk());

        verify(habitService).getByIdAndLanguageCode(1L, "en");
    }

    @Test
    void getAllTest() throws Exception {
        mockMvc.perform(get(habitLink)
                        .locale(Locale.ENGLISH))
                .andExpect(status().isOk());

        verify(habitService).getAllHabitsByLanguageCode(isNull(), any(Pageable.class), eq("en"));
    }

    @Test
    void getShoppingListItemsTest() throws Exception {
        mockMvc.perform(get(habitLink + "/{id}/shopping-list", 1L)
                        .locale(Locale.ENGLISH))
                .andExpect(status().isOk());

        verify(habitService).getShoppingListForHabit(1L, "en");
    }

    @Test
    void getAllByTagsAndLanguageCodeTest() throws Exception {
        PageableDto<HabitDto> emptyResult = new PageableDto<>();
        when(habitService.getAllByTagsAndLanguageCode(any(Pageable.class), anyList(), anyString()))
                .thenReturn(emptyResult);

        mockMvc.perform(get(habitLink + "/tags/search")
                        .param("tags", "tag1", "tag2")
                        .locale(Locale.ENGLISH))
                .andExpect(status().isOk());

        verify(habitService).getAllByTagsAndLanguageCode(any(Pageable.class),
                anyList(), eq("en"));
    }

    @Test
    void getAllByDifferentParametersTest() throws Exception {
        when(habitService.getAllByDifferentParameters(
                any(), any(), any(), any(), any(), eq("en")
        )).thenReturn(new PageableDto<>());

        mockMvc.perform(get(habitLink + "/search")
                        .param("tags", "tag1", "tag2")
                        .locale(Locale.ENGLISH))
                .andExpect(status().isOk());

        verify(habitService).getAllByDifferentParameters(any(), any(), any(), any(), any(), eq("en"));
    }

    @Test
    void findAllHabitsTagsTest() throws Exception {
        when(tagsService.findAllHabitsTags(anyString()))
                .thenReturn(List.of("tag1", "tag2"));

        mockMvc.perform(get(habitLink + "/tags")
                        .locale(Locale.ENGLISH))
                .andExpect(status().isOk());

        verify(tagsService).findAllHabitsTags("en");
    }

    @Test
    void addCustomHabitTest() throws Exception {
        AddCustomHabitDtoRequest request = AddCustomHabitDtoRequest.builder()
                .complexity(2)
                .tagIds(Set.of(1L))
                .build();

        MockMultipartFile jsonFile = new MockMultipartFile(
                "request", "request.json", "application/json",
                new ObjectMapper().writeValueAsBytes(request)
        );

        MockMultipartFile image = new MockMultipartFile(
                "image", "image.png", "image/png",
                "dummy".getBytes()
        );

        when(habitService.addCustomHabit(any(), any(), anyString()))
                .thenReturn(new AddCustomHabitDtoResponse());

        mockMvc.perform(multipart(habitLink + "/custom")
                        .file(jsonFile)
                        .file(image)
                        .principal(principal))
                .andExpect(status().isCreated());

        verify(habitService).addCustomHabit(any(), any(), eq("test@gmail.com"));
    }
    @Test
    void getFriendsAssignedToHabitProfilePicturesTest() throws Exception {
        Long habitId = 1L;
        UserVO userVO = new UserVO();
        userVO.setId(1L);

        when(habitService.getFriendsAssignedToHabitProfilePictures(habitId, userVO.getId()))
                .thenReturn(List.of(new UserProfilePictureDto(), new UserProfilePictureDto()));

        mockMvc.perform(get(habitLink + "/{habitId}/friends/profile-pictures", habitId)
                        .principal(principal)
                        .locale(Locale.ENGLISH))
                .andExpect(status().isOk());

        verify(habitService).getFriendsAssignedToHabitProfilePictures(habitId, userVO.getId());
    }
}

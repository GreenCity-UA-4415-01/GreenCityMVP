package greencity.controller;

import greencity.converters.UserArgumentResolver;
import greencity.dto.PageableDto;
import greencity.dto.habit.AddCustomHabitDtoRequest;
import greencity.dto.habit.AddCustomHabitDtoResponse;
import greencity.dto.habit.HabitDto;

import greencity.dto.habittranslation.HabitTranslationDto;
import greencity.dto.shoppinglistitem.CustomShoppingListItemResponseDto;
import greencity.dto.shoppinglistitem.ShoppingListItemDto;
import greencity.dto.user.UserProfilePictureDto;
import greencity.dto.user.UserVO;
import greencity.enums.HabitAssignStatus;
import greencity.enums.Role;
import greencity.enums.ShoppingListItemStatus;
import greencity.enums.UserStatus;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class HabitControllerTest {

    public static final String habitLink = "/habit";

    private MockMvc mockMvc;

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

    ObjectMapper objectMapper;

    Principal principal = () -> "test@mail.com";

    UserVO user = new UserVO();
    HabitDto habit1 = new HabitDto();
    HabitDto habit2 = new HabitDto();
    List<HabitDto> habits = new ArrayList<>();
    PageableDto<HabitDto> pageableDto = new PageableDto<>();
    Locale locale = Locale.ENGLISH;
    Pageable pageable = PageRequest.of(0, 20);

    private final ErrorAttributes errorAttributes = new DefaultErrorAttributes();

    @BeforeEach
    void setUp() {

        this.mockMvc = MockMvcBuilders
            .standaloneSetup(habitController)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(),
                new UserArgumentResolver(userService, modelMapper))
            .setControllerAdvice(new CustomExceptionHandler(errorAttributes, objectMapper))
            .build();

        user = UserVO.builder()
            .id(1L)
            .name("Andrii Shevchenko")
            .email("test@mail.com")
            .role(Role.ROLE_USER)
            .userCredo("Do it!")
            .userStatus(UserStatus.ACTIVATED)
            .rating(4.0)
            .dateOfRegistration(LocalDateTime.now().minusMonths(3))
            .lastActivityTime(LocalDateTime.now())
            .firstName("Andrii")
            .city("Chernivtsi")
            .build();

        habit1 = HabitDto.builder()
            .id(1L)
            .defaultDuration(30)
            .amountAcquiredUsers(100L)
            .complexity(2)
            .image("habit1.png")
            .tags(List.of("fitness", "morning"))
            .shoppingListItems(List.of(
                ShoppingListItemDto.builder()
                    .id(1L)
                    .text("Water Bottle")
                    .status("Ready to start")
                    .build()))
            .customShoppingListItems(Collections.emptyList())
            .isCustomHabit(false)
            .usersIdWhoCreatedCustomHabit(null)
            .habitAssignStatus(HabitAssignStatus.INPROGRESS)
            .habitTranslation(HabitTranslationDto.builder()
                .name("Running")
                .languageCode("en")
                .description("Running in park")
                .habitItem(null)
                .build())
            .build();

        habit2 = HabitDto.builder()
            .id(2L)
            .defaultDuration(45)
            .amountAcquiredUsers(10L)
            .complexity(3)
            .image("habit2.png")
            .tags(List.of("nutrition", "evening"))
            .shoppingListItems(List.of(
                ShoppingListItemDto.builder()
                    .id(2L)
                    .text("Protein Powder")
                    .status("Ready to start")
                    .build()))
            .customShoppingListItems(List.of(
                CustomShoppingListItemResponseDto.builder()
                    .id(1L)
                    .text("Almond Milk")
                    .status(ShoppingListItemStatus.ACTIVE)
                    .build()))
            .isCustomHabit(true)
            .usersIdWhoCreatedCustomHabit(123L)
            .habitAssignStatus(HabitAssignStatus.REQUESTED)
            .habitTranslation(HabitTranslationDto.builder()
                .name("Evening Protein Shake")
                .languageCode("en")
                .description("Evening protein shake for recovery")
                .habitItem(null)
                .build())
            .build();
    }

    @Test
    void getHabitByIdTest() throws Exception {
        when(habitService.getByIdAndLanguageCode(eq(habit1.getId()), eq(locale.getLanguage())))
                .thenReturn(habit1);

        mockMvc.perform(get(habitLink + "/{id}", habit1.getId())
                        .locale(locale)
                        .principal(principal))
                .andExpect(status().isOk());

        verify(habitService).getByIdAndLanguageCode(eq(habit1.getId()), eq(locale.getLanguage()));
    }

    @Test
    void getAllTest() throws Exception {
        habits = List.of(habit1, habit2);
        pageableDto = new PageableDto<>(habits, habits.size(), 0, 1);

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(habitService.getAllHabitsByLanguageCode(eq(user), eq(pageable), eq(locale.getLanguage())))
            .thenReturn(pageableDto);

        mockMvc.perform(get(habitLink)
            .locale(locale)
            .principal(principal)).andExpect(status().isOk());

        verify(habitService).getAllHabitsByLanguageCode(eq(user), eq(pageable), eq(locale.getLanguage()));
    }

    @Test
    void getShoppingListItemsTest() throws Exception {
        List<ShoppingListItemDto> shoppingListItemDtos = Stream.concat(
            habit1.getShoppingListItems().stream(),
            habit2.getShoppingListItems().stream()).toList();

        when(habitService.getShoppingListForHabit(eq(habit1.getId()), eq(locale.getLanguage())))
            .thenReturn(shoppingListItemDtos);

        mockMvc.perform(get(habitLink + "/{id}/shopping-list", habit1.getId())
            .locale(locale))
            .andExpect(status().isOk());

        verify(habitService).getShoppingListForHabit(eq(habit1.getId()), eq(locale.getLanguage()));
    }

    @Test
    void getAllByTagsAndLanguageCodeTest() throws Exception {
        List<String> tags = Stream.concat(
            habit1.getTags().stream(),
            habit2.getTags().stream()).toList();
        pageableDto = new PageableDto<>(habits, tags.size(), 0, 1);

        when(habitService.getAllByTagsAndLanguageCode(eq(pageable), eq(tags), eq(locale.getLanguage())))
            .thenReturn(pageableDto);

        mockMvc.perform(get(habitLink + "/tags/search")
            .locale(locale)
            .param("tags", tags.toArray(new String[0]))
            .param("page", String.valueOf(pageable.getPageNumber()))
            .param("size", String.valueOf(pageable.getPageSize())))
            .andExpect(status().isOk());

        verify(habitService).getAllByTagsAndLanguageCode(eq(pageable), eq(tags), eq(locale.getLanguage()));
    }

    @Test
    void getAllByDifferentParametersTest() throws Exception {
        when(userService.findByEmail(anyString())).thenReturn(user);

        List<String> tags = Stream.concat(
                habit1.getTags().stream(),
                habit2.getTags().stream()
        ).toList();
        Boolean isCustomHabit = habit1.getIsCustomHabit();
        List<Integer> complexities = List.of(habit1.getComplexity(), habit2.getComplexity());
        pageableDto = new PageableDto<>(habits, habits.size(), 0, 1);

        when(habitService.getAllByDifferentParameters(
                eq(user),
                eq(pageable),
                eq(Optional.of(tags)),
                eq(Optional.of(isCustomHabit)),
                eq(Optional.of(complexities)),
                eq(locale.getLanguage()))
        ).thenReturn(pageableDto);

        mockMvc.perform(get(habitLink + "/search")
                        .principal(principal)
                        .param("tags", tags.toArray(new String[0]))
                        .param("isCustomHabit", String.valueOf(isCustomHabit))
                        .param("complexities", complexities.stream().map(String::valueOf).toArray(String[]::new))
                        .param("page", String.valueOf(pageable.getPageNumber()))
                        .param("size", String.valueOf(pageable.getPageSize())))
                .andExpect(status().isOk());

        verify(habitService).getAllByDifferentParameters(eq(user),
                eq(pageable),
                eq(Optional.of(tags)),
                eq(Optional.of(isCustomHabit)),
                eq(Optional.of(complexities)),
                eq(locale.getLanguage()));
    }

    @Test
    void getAllByDifferentParametersBadRequestTest() throws Exception {
        when(userService.findByEmail(anyString())).thenReturn(user);
        mockMvc.perform(get(habitLink + "/search")
                        .principal(principal)
                        .locale(locale)
                        .param("page", String.valueOf(pageable.getPageNumber()))
                        .param("size", String.valueOf(pageable.getPageSize()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(xpath("/ExceptionResponse/message").string("You should enter at least one parameter"));
    }

    @Test
    void findAllHabitsTagsTest() throws Exception {
        List<String> tags = Stream.concat(
            habit1.getTags().stream(),
            habit2.getTags().stream()).toList();

        when(tagsService.findAllHabitsTags(eq(locale.getLanguage())))
            .thenReturn(tags);

        mockMvc.perform(get(habitLink + "/tags")
            .locale(locale))
            .andExpect(status().isOk());

        verify(tagsService).findAllHabitsTags(eq(locale.getLanguage()));
    }

    @Test
    void addCustomHabitTest() throws Exception {
        AddCustomHabitDtoRequest request = AddCustomHabitDtoRequest.builder()
            .complexity(2)
            .defaultDuration(7)
            .habitTranslations(List.of(
                HabitTranslationDto.builder()
                    .name("Running")
                    .languageCode("en")
                    .description("Running in park")
                    .habitItem(null)
                    .build(),
                HabitTranslationDto.builder()
                    .name("Evening Protein Shake")
                    .languageCode("en")
                    .description("Evening protein shake for recovery")
                    .habitItem("Cup for protein")
                    .build()))
            .image("image.png")
            .customShoppingListItemDto(List.of(
                CustomShoppingListItemResponseDto.builder()
                    .id(1L)
                    .text("Almond Milk")
                    .status(ShoppingListItemStatus.ACTIVE)
                    .build()))
            .tagIds(Set.of(1L, 2L))
            .build();

        AddCustomHabitDtoResponse response = AddCustomHabitDtoResponse.builder()
            .id(1L)
            .userId(42L)
            .complexity(request.getComplexity())
            .defaultDuration(request.getDefaultDuration())
            .habitTranslations(request.getHabitTranslations())
            .customShoppingListItemDto(request.getCustomShoppingListItemDto())
            .tagIds(request.getTagIds())
            .image(request.getImage())
            .build();

        MockMultipartFile image = new MockMultipartFile(
            "image",
            "image.png",
            "image/png",
            "dummy image content".getBytes());
        MockMultipartFile jsonPart = new MockMultipartFile(
            "request",
            "",
            "application/json",
            new ObjectMapper().writeValueAsBytes(request));

        when(habitService.addCustomHabit(eq(request), any(MultipartFile.class), eq(principal.getName())))
            .thenReturn(response);

        mockMvc.perform(multipart(habitLink + "/custom")
            .file(image)
            .file(jsonPart)
            .principal(principal)
            .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isCreated());
    }

    @Test
    void addCustomHabitBadRequestTest() throws Exception {
        AddCustomHabitDtoRequest emptyRequest = new AddCustomHabitDtoRequest();

        MockMultipartFile image = new MockMultipartFile(
            "image",
            "image.png",
            "image/png",
            "dummy image content".getBytes());
        MockMultipartFile jsonPart = new MockMultipartFile(
            "request",
            "",
            "application/json",
            new ObjectMapper().writeValueAsBytes(emptyRequest));

        mockMvc.perform(multipart(habitLink + "/custom")
            .file(image)
            .file(jsonPart)
            .principal(principal)
            .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getFriendsAssignedToHabitProfilePicturesTest() throws Exception {
        when(userService.findByEmail(anyString())).thenReturn(user);

        List<UserProfilePictureDto> userProfilePictureDtos = List.of(UserProfilePictureDto.builder()
                        .id(1L)
                        .name("Carrot")
                        .profilePicturePath("carrot.png")
                        .build(),
                UserProfilePictureDto.builder()
                        .id(1L)
                        .name("Watermelon")
                        .profilePicturePath("watermelon.png")
                        .build()
        );

        when(habitService.getFriendsAssignedToHabitProfilePictures(eq(habit1.getId()), eq(user.getId())))
                .thenReturn(userProfilePictureDtos);

        mockMvc.perform(get(habitLink + "/{habitId}/friends/profile-pictures", habit1.getId())
                .principal(principal)).andExpect(status().isOk());

        verify(habitService).getFriendsAssignedToHabitProfilePictures(eq(habit1.getId()), eq(user.getId()));
    }
}
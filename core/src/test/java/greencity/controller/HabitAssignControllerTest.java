package greencity.controller;

import greencity.converters.UserArgumentResolver;
import greencity.dto.habit.*;
import greencity.dto.habittranslation.HabitTranslationDto;
import greencity.dto.shoppinglistitem.CustomShoppingListItemResponseDto;
import greencity.dto.shoppinglistitem.ShoppingListItemDto;
import greencity.dto.user.UserShoppingListItemAdvanceDto;
import greencity.dto.user.UserShoppingListItemResponseDto;
import greencity.dto.user.UserVO;
import greencity.enums.HabitAssignStatus;
import greencity.enums.Role;
import greencity.enums.ShoppingListItemStatus;
import greencity.enums.UserStatus;
import greencity.service.HabitAssignService;
import greencity.service.LanguageService;
import greencity.service.UserService;
import greencity.validator.LanguageValidator;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.security.Principal;

@ExtendWith(MockitoExtension.class)
class HabitAssignControllerTest {

    private MockMvc mockMvc;
    @InjectMocks
    HabitAssignController habitAssignController;

    @Mock
    HabitAssignService habitAssignService;

    @Mock
    UserService userService;

    @Mock
    ModelMapper modelMapper;

    Principal principal = () -> "test@example.com";

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    UserVO user;
    HabitDto habit;
    HabitAssignManagementDto habitAssignManagement;
    HabitAssignDto habitAssign;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(habitAssignController)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(),
                new UserArgumentResolver(userService, modelMapper))
            .build();

        user = UserVO.builder()
            .id(1L)
            .name("Іван Петренко")
            .email("test@example.com")
            .role(Role.ROLE_USER)
            .userCredo("Жити екологічно!")
            .userStatus(UserStatus.ACTIVATED)
            .rating(4.5)
            .dateOfRegistration(LocalDateTime.now().minusMonths(2))
            .lastActivityTime(LocalDateTime.now())
            .firstName("Іван")
            .city("Київ")
            .build();

        habit = HabitDto.builder()
            .id(1L)
            .defaultDuration(14)
            .amountAcquiredUsers(123L)
            .habitTranslation(HabitTranslationDto.builder()
                .name("Сортування сміття")
                .description("Навчитися правильно сортувати побутові відходи")
                .build())
            .image("https://example.com/images/habit.jpg")
            .complexity(2)
            .tags(List.of("екологія", "звички", "відходи"))
            .shoppingListItems(List.of(
                ShoppingListItemDto.builder().id(1L).text("Сортувати пластик").build(),
                ShoppingListItemDto.builder().id(2L).text("Знайти місцевий пункт прийому").build()))
            .customShoppingListItems(List.of(
                CustomShoppingListItemResponseDto.builder().id(101L).text("Придбати контейнери для сортування")
                    .status(ShoppingListItemStatus.INPROGRESS).build()))
            .isCustomHabit(false)
            .usersIdWhoCreatedCustomHabit(user.getId())
            .habitAssignStatus(HabitAssignStatus.INPROGRESS)
            .build();

        habitAssignManagement = HabitAssignManagementDto.builder()
            .id(1L)
            .status(HabitAssignStatus.INPROGRESS)
            .createDateTime(ZonedDateTime.now().minusDays(10))
            .habitId(5L)
            .userId(1L)
            .duration(14)
            .workingDays(10)
            .habitStreak(3)
            .lastEnrollment(ZonedDateTime.now().minusDays(1))
            .progressNotificationHasDisplayed(false)
            .build();

        habitAssign = HabitAssignDto.builder()
            .id(1l)
            .userId(user.getId())
            .habitStreak(3)
            .duration(14)
            .status(HabitAssignStatus.INPROGRESS)
            .workingDays(10)
            .habit(null)
            .createDateTime(ZonedDateTime.now().minusDays(10))
            .lastEnrollmentDate(ZonedDateTime.now().minusDays(1))
            .progressNotificationHasDisplayed(false)
            .build();

    }

    @Test
    void testAssignHabitWithDefaultProperties() throws Exception {
        Long habitId = 5L;

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(habitAssignService.assignDefaultHabitForUser(eq(habitId), any(UserVO.class)))
            .thenReturn(habitAssignManagement);

        mockMvc.perform(post("/habit/assign/" + habitId)
            .accept(MediaType.APPLICATION_JSON)
            .principal(principal))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(habitAssignManagement.getId()))
            .andExpect(jsonPath("$.status").value(habitAssignManagement.getStatus().toString()))
            .andExpect(jsonPath("$.habitId").value(habitAssignManagement.getHabitId()))
            .andExpect(jsonPath("$.userId").value(habitAssignManagement.getUserId()))
            .andExpect(jsonPath("$.duration").value(habitAssignManagement.getDuration()))
            .andExpect(jsonPath("$.workingDays").value(habitAssignManagement.getWorkingDays()))
            .andExpect(jsonPath("$.habitStreak").value(habitAssignManagement.getHabitStreak()))
            .andExpect(jsonPath("$.progressNotificationHasDisplayed")
                .value(habitAssignManagement.getProgressNotificationHasDisplayed()))
            .andExpect(jsonPath("$.createDateTime").isNumber())
            .andExpect(jsonPath("$.lastEnrollment").isNumber());

    }

    @Test
    void testAssignHabitWithCustomProperties() throws Exception {
        Long habitId = 5L;

        HabitAssignCustomPropertiesDto requestDto = HabitAssignCustomPropertiesDto.builder()
            .habitAssignPropertiesDto(
                HabitAssignPropertiesDto.builder()
                    .duration(14)
                    .defaultShoppingListItems(List.of(1001L, 1002L, 1003L))
                    .build())
            .friendsIdsList(List.of(2L, 3L, 4L))
            .build();

        when(userService.findByEmail(anyString())).thenReturn(user);
        List<HabitAssignManagementDto> responseList = List.of(habitAssignManagement);

        when(habitAssignService.assignCustomHabitForUser(eq(habitId), any(UserVO.class), eq(requestDto)))
            .thenReturn(responseList);

        mockMvc.perform(post("/habit/assign/" + habitId + "/custom")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto))
            .principal(principal))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$[0].id").value(habitAssignManagement.getId()))
            .andExpect(jsonPath("$[0].status").value(habitAssignManagement.getStatus().toString()))
            .andExpect(jsonPath("$[0].habitId").value(habitAssignManagement.getHabitId()))
            .andExpect(jsonPath("$[0].userId").value(habitAssignManagement.getUserId()))
            .andExpect(jsonPath("$[0].duration").value(habitAssignManagement.getDuration()))
            .andExpect(jsonPath("$[0].workingDays").value(habitAssignManagement.getWorkingDays()))
            .andExpect(jsonPath("$[0].habitStreak").value(habitAssignManagement.getHabitStreak()))
            .andExpect(jsonPath("$[0].progressNotificationHasDisplayed")
                .value(habitAssignManagement.getProgressNotificationHasDisplayed()));
    }

    @Test
    void testUpdateHabitAssignDuration() throws Exception {
        Long habitAssignId = 5L;
        Integer duration = 14;

        HabitAssignUserDurationDto responseDto = HabitAssignUserDurationDto.builder()
            .habitAssignId(habitAssignId)
            .userId(user.getId())
            .habitId(10L)
            .status(HabitAssignStatus.INPROGRESS)
            .workingDays(10)
            .duration(duration)
            .build();

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(habitAssignService.updateUserHabitInfoDuration(eq(habitAssignId), anyLong(), eq(duration)))
            .thenReturn(responseDto);

        mockMvc.perform(put("/habit/assign/" + habitAssignId + "/update-habit-duration")
            .param("duration", String.valueOf(duration))
            .principal(principal)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.habitAssignId").value(responseDto.getHabitAssignId()))
            .andExpect(jsonPath("$.userId").value(responseDto.getUserId()))
            .andExpect(jsonPath("$.habitId").value(responseDto.getHabitId()))
            .andExpect(jsonPath("$.status").value(responseDto.getStatus().toString()))
            .andExpect(jsonPath("$.workingDays").value(responseDto.getWorkingDays()))
            .andExpect(jsonPath("$.duration").value(responseDto.getDuration()));
    }

    @Test
    void testGetHabitAssign() throws Exception {
        Long habitAssignId = habitAssign.getId();
        String language = "ua";

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(habitAssignService.getByHabitAssignIdAndUserId(eq(habitAssignId), eq(user.getId()), eq(language)))
            .thenReturn(habitAssign);

        mockMvc.perform(get("/habit/assign/" + habitAssignId)
            .accept(MediaType.APPLICATION_JSON)
            .header("Accept-Language", language)
            .principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(habitAssign.getId()))
            .andExpect(jsonPath("$.userId").value(habitAssign.getUserId()))
            .andExpect(jsonPath("$.habitStreak").value(habitAssign.getHabitStreak()))
            .andExpect(jsonPath("$.duration").value(habitAssign.getDuration()));
    }

    @Test
    void testGetCurrentUserHabitAssigns() throws Exception {
        String language = "ua";

        List<HabitAssignDto> habitAssigns = List.of(habitAssign);
        when(userService.findByEmail(anyString())).thenReturn(user);
        when(habitAssignService.getAllHabitAssignsByUserIdAndStatusNotCancelled(user.getId(), language))
            .thenReturn(habitAssigns);

        mockMvc.perform(get("/habit/assign/allForCurrentUser")
            .accept(MediaType.APPLICATION_JSON)
            .header("Accept-Language", language)
            .principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(habitAssigns.getFirst().getId()))
            .andExpect(jsonPath("$[0].userId").value(habitAssigns.getFirst().getUserId()))
            .andExpect(jsonPath("$[0].habitStreak").value(habitAssigns.getFirst().getHabitStreak()))
            .andExpect(jsonPath("$[0].duration").value(habitAssigns.getFirst().getDuration()));
    }

    @Test
    void testGetUserShoppingAndCustomShoppingLists() throws Exception {
        String language = "ua";

        UserShoppingListItemResponseDto userShoppingResponse = UserShoppingListItemResponseDto
            .builder()
            .id(1L)
            .text("User Shopping List Item")
            .status(ShoppingListItemStatus.INPROGRESS)
            .build();

        CustomShoppingListItemResponseDto customShoppingResponse = CustomShoppingListItemResponseDto
            .builder()
            .id(1L)
            .text("Custom Shopping List Item")
            .status(ShoppingListItemStatus.INPROGRESS)
            .build();

        UserShoppingAndCustomShoppingListsDto resultDto = UserShoppingAndCustomShoppingListsDto
            .builder()
            .userShoppingListItemDto(List.of(userShoppingResponse))
            .customShoppingListItemDto(List.of(customShoppingResponse))
            .build();

        List<UserShoppingAndCustomShoppingListsDto> responseList = List.of(resultDto);

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(habitAssignService.getListOfUserAndCustomShoppingListsWithStatusInprogress(eq(user.getId()), eq(language)))
            .thenReturn(responseList);

        mockMvc.perform(get("/habit/assign/allUserAndCustomShoppingListsInprogress")
            .accept(MediaType.APPLICATION_JSON)
            .header("Accept-Language", language)
            .principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userShoppingListItemDto[0].id").value(userShoppingResponse.getId()))
            .andExpect(jsonPath("$[0].userShoppingListItemDto[0].text").value(userShoppingResponse.getText()))
            .andExpect(
                jsonPath("$[0].userShoppingListItemDto[0].status").value(userShoppingResponse.getStatus().toString()))
            .andExpect(jsonPath("$[0].customShoppingListItemDto[0].id").value(customShoppingResponse.getId()))
            .andExpect(jsonPath("$[0].customShoppingListItemDto[0].text").value(customShoppingResponse.getText()))
            .andExpect(jsonPath("$[0].customShoppingListItemDto[0].status")
                .value(customShoppingResponse.getStatus().toString()));
    }

    @Test
    void testGetAllHabitAssignsByHabitIdAndAcquired() throws Exception {
        Long habitId = 1l;
        String language = "ua";

        List<HabitAssignDto> habitAssigns = List.of(habitAssign);

        when(habitAssignService.getAllHabitAssignsByHabitIdAndStatusNotCancelled(habitId, language))
            .thenReturn(habitAssigns);

        mockMvc.perform(get("/habit/assign/" + habitId + "/all")
            .accept(MediaType.APPLICATION_JSON)
            .header("Accept-Language", language))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(habitAssigns.getFirst().getId()))
            .andExpect(jsonPath("$[0].userId").value(habitAssigns.getFirst().getUserId()))
            .andExpect(jsonPath("$[0].habitStreak").value(habitAssigns.getFirst().getHabitStreak()))
            .andExpect(jsonPath("$[0].duration").value(habitAssigns.getFirst().getDuration()));

    }

    @Test
    void testGetActiveHabitAssignByHabitId() throws Exception {
        Long habitId = 1l;
        String language = "ua";

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(habitAssignService.findHabitAssignByUserIdAndHabitId(user.getId(), habitId, language))
            .thenReturn(habitAssign);

        mockMvc.perform(get("/habit/assign/" + habitId + "/active")
            .accept(MediaType.APPLICATION_JSON)
            .header("Accept-Language", language)
            .principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(habitAssign.getId()))
            .andExpect(jsonPath("$.userId").value(habitAssign.getUserId()))
            .andExpect(jsonPath("$.habitStreak").value(habitAssign.getHabitStreak()))
            .andExpect(jsonPath("$.duration").value(habitAssign.getDuration()));
    }

    @Test
    void testGetMoreHabitAssignByHabitId() throws Exception {
        Long habitAssignId = habitAssign.getId();
        String language = "ua";

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(habitAssignService.findHabitByUserIdAndHabitAssignId(user.getId(), habitAssignId, language))
            .thenReturn(habit);

        mockMvc.perform(get("/habit/assign/" + habitAssignId + "/more")
            .accept(MediaType.APPLICATION_JSON)
            .header("Accept-Language", language)
            .principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(habit.getId()))
            .andExpect(jsonPath("$.defaultDuration").value(habit.getDefaultDuration()))
            .andExpect(jsonPath("$.amountAcquiredUsers").value(habit.getAmountAcquiredUsers()))
            .andExpect(jsonPath("$.habitTranslation.name").value(habit.getHabitTranslation().getName()))
            .andExpect(jsonPath("$.habitTranslation.description").value(habit.getHabitTranslation().getDescription()))
            .andExpect(jsonPath("$.image").value(habit.getImage()))
            .andExpect(jsonPath("$.complexity").value(habit.getComplexity()))
            .andExpect(jsonPath("$.tags[0]").value(habit.getTags().get(0)))
            .andExpect(jsonPath("$.tags[1]").value(habit.getTags().get(1)))
            .andExpect(jsonPath("$.tags[2]").value(habit.getTags().get(2)))
            .andExpect(jsonPath("$.shoppingListItems[0].id").value(habit.getShoppingListItems().get(0).getId()))
            .andExpect(jsonPath("$.shoppingListItems[0].text").value(habit.getShoppingListItems().get(0).getText()))
            .andExpect(jsonPath("$.shoppingListItems[1].id").value(habit.getShoppingListItems().get(1).getId()))
            .andExpect(jsonPath("$.shoppingListItems[1].text").value(habit.getShoppingListItems().get(1).getText()))
            .andExpect(
                jsonPath("$.customShoppingListItems[0].id").value(habit.getCustomShoppingListItems().get(0).getId()))
            .andExpect(jsonPath("$.customShoppingListItems[0].text")
                .value(habit.getCustomShoppingListItems().get(0).getText()))
            .andExpect(jsonPath("$.customShoppingListItems[0].status")
                .value(habit.getCustomShoppingListItems().get(0).getStatus().toString()))
            .andExpect(jsonPath("$.isCustomHabit").value(habit.getIsCustomHabit()))
            .andExpect(jsonPath("$.usersIdWhoCreatedCustomHabit").value(habit.getUsersIdWhoCreatedCustomHabit()))
            .andExpect(jsonPath("$.habitAssignStatus").value(habit.getHabitAssignStatus().toString()));
    }

    @Test
    void testUpdateAssignByHabitId() throws Exception {
        Long habitAssignId = habitAssign.getId();
        String language = "ua";

        HabitAssignStatDto habitAssignStatus = new HabitAssignStatDto(HabitAssignStatus.INPROGRESS);

        when(habitAssignService.updateStatusByHabitAssignId(eq(habitAssignId), eq(habitAssignStatus)))
            .thenReturn(habitAssignManagement);

        mockMvc.perform(patch("/habit/assign/" + habitAssignId)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Accept-Language", language)
            .content(objectMapper.writeValueAsString(habitAssignStatus)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(habitAssignManagement.getId()))
            .andExpect(jsonPath("$.status").value(habitAssignManagement.getStatus().toString()))
            .andExpect(jsonPath("$.habitId").value(habitAssignManagement.getHabitId()))
            .andExpect(jsonPath("$.userId").value(habitAssignManagement.getUserId()))
            .andExpect(jsonPath("$.duration").value(habitAssignManagement.getDuration()))
            .andExpect(jsonPath("$.workingDays").value(habitAssignManagement.getWorkingDays()))
            .andExpect(jsonPath("$.habitStreak").value(habitAssignManagement.getHabitStreak()))
            .andExpect(jsonPath("$.progressNotificationHasDisplayed")
                .value(habitAssignManagement.getProgressNotificationHasDisplayed()))
            .andExpect(jsonPath("$.createDateTime").isNumber())
            .andExpect(jsonPath("$.lastEnrollment").isNumber());
    }

    @Test
    void testEnrollHabit() throws Exception {
        Long habitAssignId = habitAssign.getId();
        String language = "ua";
        LocalDate date = LocalDate.now();

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(habitAssignService.enrollHabit(habitAssignId, user.getId(), date, language))
            .thenReturn(habitAssign);

        mockMvc.perform(post("/habit/assign/" + habitAssignId + "/enroll/" + date)
            .accept(MediaType.APPLICATION_JSON)
            .header("Accept-Language", language)
            .principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(habitAssign.getId()))
            .andExpect(jsonPath("$.userId").value(habitAssign.getUserId()))
            .andExpect(jsonPath("$.habitStreak").value(habitAssign.getHabitStreak()))
            .andExpect(jsonPath("$.duration").value(habitAssign.getDuration()));
    }

    @Test
    void testUnEnrollHabit() throws Exception {
        Long habitAssignId = habitAssign.getId();
        LocalDate date = LocalDate.now();

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(habitAssignService.unenrollHabit(habitAssignId, user.getId(), date))
            .thenReturn(habitAssign);

        mockMvc.perform(post("/habit/assign/" + habitAssignId + "/unenroll/" + date)
            .accept(MediaType.APPLICATION_JSON)
            .principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(habitAssign.getId()))
            .andExpect(jsonPath("$.userId").value(habitAssign.getUserId()))
            .andExpect(jsonPath("$.habitStreak").value(habitAssign.getHabitStreak()))
            .andExpect(jsonPath("$.duration").value(habitAssign.getDuration()));
    }

    @Test
    void testGetInprogressHabitAssignOnDate() throws Exception {
        String language = "ua";
        LocalDate date = LocalDate.now();

        List<HabitAssignDto> habitAssigns = List.of(habitAssign);

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(habitAssignService.findInprogressHabitAssignsOnDate(user.getId(), date, language))
            .thenReturn(habitAssigns);

        mockMvc.perform(get("/habit/assign/active/" + date)
            .accept(MediaType.APPLICATION_JSON)
            .header("Accept-Language", language)
            .principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(habitAssigns.getFirst().getId()))
            .andExpect(jsonPath("$[0].userId").value(habitAssigns.getFirst().getUserId()))
            .andExpect(jsonPath("$[0].habitStreak").value(habitAssigns.getFirst().getHabitStreak()))
            .andExpect(jsonPath("$[0].duration").value(habitAssigns.getFirst().getDuration()));
    }

    @Test
    void testGetHabitAssignBetweenDates() throws Exception {
        String language = "ua";
        LocalDate fromDate = LocalDate.now().minusMonths(2);
        LocalDate toDate = LocalDate.now().minusMonths(1);

        when(userService.findByEmail(anyString())).thenReturn(user);

        HabitEnrollDto habitEnroll = HabitEnrollDto.builder()
            .habitAssignId(1L)
            .habitDescription("Growing plant")
            .isEnrolled(true)
            .build();

        HabitsDateEnrollmentDto habitsDateEnrollment = HabitsDateEnrollmentDto.builder()
            .enrollDate(fromDate)
            .habitAssigns(List.of(habitEnroll))
            .build();

        List<HabitsDateEnrollmentDto> habitsDateEnrollments = List.of(habitsDateEnrollment);

        when(habitAssignService.findHabitAssignsBetweenDates(user.getId(), fromDate, toDate, language))
            .thenReturn(habitsDateEnrollments);

        mockMvc.perform(get("/habit/assign/activity/{from}/to/{to}", fromDate, toDate)
            .accept(MediaType.APPLICATION_JSON)
            .header("Accept-Language", language)
            .principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].enrollDate").isNotEmpty())
            .andExpect(jsonPath("$[0].habitAssigns[0].habitAssignId").value(habitEnroll.getHabitAssignId()))
            .andExpect(jsonPath("$[0].habitAssigns[0].habitDescription").value(habitEnroll.getHabitDescription()))
            .andExpect(jsonPath("$[0].habitAssigns[0].enrolled").value(habitEnroll.isEnrolled()));
    }

    @Test
    void testCancelHabitAssign() throws Exception {
        Long habitId = habit.getId();

        when(userService.findByEmail(anyString())).thenReturn(user);
        when(habitAssignService.cancelHabitAssign(habitId, user.getId()))
            .thenReturn(habitAssign);

        mockMvc.perform(patch("/habit/assign/cancel/" + habitId)
            .accept(MediaType.APPLICATION_JSON)
            .principal(principal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(habitAssign.getId()))
            .andExpect(jsonPath("$.userId").value(habitAssign.getUserId()))
            .andExpect(jsonPath("$.habitStreak").value(habitAssign.getHabitStreak()))
            .andExpect(jsonPath("$.duration").value(habitAssign.getDuration()));
    }

    @Test
    void testDeleteHabitAssign() throws Exception {
        Long habitId = habit.getId();

        when(userService.findByEmail(anyString())).thenReturn(user);
        mockMvc.perform(delete("/habit/assign/delete/" + habitId)
            .accept(MediaType.APPLICATION_JSON)
            .principal(principal))
            .andExpect(status().isOk());
    }

    @Test
    void testUpdateShoppingListStatus() throws Exception {
        UpdateUserShoppingListDto requestDto = UpdateUserShoppingListDto.builder()
            .habitAssignId(1L)
            .userShoppingListItemId(101L)
            .userShoppingListAdvanceDto(List.of(
                UserShoppingListItemAdvanceDto.builder()
                    .id(1L)
                    .shoppingListItemId(1001L)
                    .status(ShoppingListItemStatus.INPROGRESS)
                    .dateCompleted(LocalDateTime.now())
                    .content("Купити контейнери для сортування")
                    .build(),

                UserShoppingListItemAdvanceDto.builder()
                    .id(2L)
                    .shoppingListItemId(1002L)
                    .status(ShoppingListItemStatus.DONE)
                    .dateCompleted(LocalDateTime.now().minusDays(1))
                    .content("Знайти пункти збору батарейок")
                    .build()))
            .build();

        mockMvc.perform(put("/habit/assign/saveShoppingListForHabitAssign")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isOk());
    }

    @Test
    void testUpdateProgressNotificationHasDisplayed() throws Exception {
        Long habitAssignId = habitAssign.getId();
        when(userService.findByEmail(anyString())).thenReturn(user);
        mockMvc.perform(put("/habit/assign/" + habitAssignId + "/updateProgressNotificationHasDisplayed")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .principal(principal))
            .andExpect(status().isOk());
    }

}
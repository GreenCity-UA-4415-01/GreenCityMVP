package greencity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import greencity.dto.habitstatistic.*;
import greencity.dto.user.UserVO;
import greencity.enums.HabitRate;
import greencity.service.HabitStatisticService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HabitStatisticControllerTest {

    private static final String HABIT_STATISTIC_LINK = "/habit/statistic";

    @Mock
    private HabitStatisticService habitStatisticService;

    @InjectMocks
    private HabitStatisticController habitStatisticController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UserVO testUser = UserVO.builder().id(1L).email("test@example.com").build();

    static class TestUserArgumentResolver implements HandlerMethodArgumentResolver {
        private final UserVO userVO;

        public TestUserArgumentResolver(UserVO userVO) {
            this.userVO = userVO;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(UserVO.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) throws Exception {
            return userVO;
        }
    }

    @BeforeEach
    void setup() {
        objectMapper.findAndRegisterModules();

        MappingJackson2HttpMessageConverter jacksonMessageConverter = new MappingJackson2HttpMessageConverter();
        jacksonMessageConverter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(habitStatisticController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(), new TestUserArgumentResolver(testUser))
                .setMessageConverters(jacksonMessageConverter)
                .build();
    }

    @Test
    void findAllByHabitId_Success() throws Exception {
        Long habitId = 1L;
        Long habitAssignId = 100L;

        HabitStatisticDto statisticDto = HabitStatisticDto.builder()
                .id(10L)
                .habitRate(HabitRate.GOOD)
                .createDate(ZonedDateTime.now())
                .amountOfItems(5)
                .habitAssignId(habitAssignId)
                .build();

        GetHabitStatisticDto mockDto = GetHabitStatisticDto.builder()
                .amountOfUsersAcquired(100L)
                .habitStatisticDtoList(List.of(statisticDto))
                .build();

        when(habitStatisticService.findAllStatsByHabitId(habitId)).thenReturn(mockDto);

        mockMvc.perform(get(HABIT_STATISTIC_LINK + "/{habitId}", habitId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountOfUsersAcquired", is(100)))
                .andExpect(jsonPath("$.habitStatisticDtoList[0].id", is(10)))
                .andExpect(jsonPath("$.habitStatisticDtoList[0].amountOfItems", is(5)));

        verify(habitStatisticService).findAllStatsByHabitId(habitId);
    }

    @Test
    void findAllStatsByHabitAssignId_Success() throws Exception {
        Long habitAssignId = 2L;
        List<HabitStatisticDto> mockList = List.of(
                HabitStatisticDto.builder().id(10L).amountOfItems(1).habitAssignId(habitAssignId).habitRate(HabitRate.GOOD).createDate(ZonedDateTime.now()).build()
        );

        when(habitStatisticService.findAllStatsByHabitAssignId(habitAssignId)).thenReturn(mockList);

        mockMvc.perform(get(HABIT_STATISTIC_LINK + "/assign/{habitAssignId}", habitAssignId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(10)))
                .andExpect(jsonPath("$[0].amountOfItems", is(1)));

        verify(habitStatisticService).findAllStatsByHabitAssignId(habitAssignId);
    }

    @Test
    void saveHabitStatistic_Success() throws Exception {
        Long habitId = 3L;
        Long habitAssignId = 300L;
        AddHabitStatisticDto addDto = AddHabitStatisticDto.builder()
                .amountOfItems(1)
                .habitRate(HabitRate.GOOD)
                .createDate(ZonedDateTime.now())
                .build();
        HabitStatisticDto savedDto = HabitStatisticDto.builder()
                .id(20L)
                .amountOfItems(1)
                .habitAssignId(habitAssignId)
                .habitRate(HabitRate.GOOD)
                .createDate(ZonedDateTime.now())
                .build();

        when(habitStatisticService.saveByHabitIdAndUserId(eq(habitId), eq(testUser.getId()), any(AddHabitStatisticDto.class))).thenReturn(savedDto);

        mockMvc.perform(post(HABIT_STATISTIC_LINK + "/{habitId}", habitId)
                        .param("habitId", String.valueOf(habitId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(20)))
                .andExpect(jsonPath("$.amountOfItems", is(1)));

        verify(habitStatisticService).saveByHabitIdAndUserId(eq(habitId), eq(testUser.getId()), any(AddHabitStatisticDto.class));
    }

    @Test
    void saveHabitStatistic_InvalidDto_ReturnsBadRequest() throws Exception {
        Long habitId = 3L;
        AddHabitStatisticDto invalidDto = AddHabitStatisticDto.builder()
                .amountOfItems(null)
                .createDate(ZonedDateTime.now())
                .build();

        mockMvc.perform(post(HABIT_STATISTIC_LINK + "/{habitId}", habitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(habitStatisticService, never()).saveByHabitIdAndUserId(any(), any(), any());
    }

    @Test
    void updateHabitStatistic_Success() throws Exception {
        Long statisticId = 4L;

        UpdateHabitStatisticDto updateDto = UpdateHabitStatisticDto.builder()
                .amountOfItems(5)
                .habitRate(HabitRate.GOOD) // Include mandatory 'habitRate'
                .build();

        UpdateHabitStatisticDto updatedDto = UpdateHabitStatisticDto.builder()
                .amountOfItems(5)
                .habitRate(HabitRate.GOOD)
                .build();

        when(habitStatisticService.update(
                eq(statisticId),
                eq(testUser.getId()),
                any(UpdateHabitStatisticDto.class)))
                .thenReturn(updatedDto);

        mockMvc.perform(put(HABIT_STATISTIC_LINK + "/{id}", statisticId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountOfItems", is(5)))
                .andExpect(jsonPath("$.habitRate", is(HabitRate.GOOD.toString()))); // Assert the enum field

        verify(habitStatisticService).update(
                eq(statisticId),
                eq(testUser.getId()),
                any(UpdateHabitStatisticDto.class));
    }

    @Test
    void getTodayStatisticsForAllHabitItems_Success() throws Exception {
        long expectedNotTakenItems = 3L;

        HabitItemsAmountStatisticDto mockItem = HabitItemsAmountStatisticDto.builder()
                .habitItem("Water")
                .notTakenItems(expectedNotTakenItems)
                .build();
        List<HabitItemsAmountStatisticDto> mockList = List.of(mockItem);

        String localeLanguage = "en";

        when(habitStatisticService.getTodayStatisticsForAllHabitItems(localeLanguage)).thenReturn(mockList);

        mockMvc.perform(get(HABIT_STATISTIC_LINK + "/todayStatisticsForAllHabitItems")
                        .locale(new Locale(localeLanguage))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].habitItem", is("Water")))
                .andExpect(jsonPath("$[0].notTakenItems", is((int) expectedNotTakenItems)));

        verify(habitStatisticService).getTodayStatisticsForAllHabitItems(localeLanguage);
    }

    @Test
    void findAmountOfAcquiredHabits_Success() throws Exception {
        Long userId = 5L;
        Long acquiredCount = 10L;

        when(habitStatisticService.getAmountOfAcquiredHabitsByUserId(userId)).thenReturn(acquiredCount);

        mockMvc.perform(get(HABIT_STATISTIC_LINK + "/acquired/count")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(acquiredCount.intValue())));

        verify(habitStatisticService).getAmountOfAcquiredHabitsByUserId(userId);
    }

    @Test
    void findAmountOfHabitsInProgress_Success() throws Exception {
        Long userId = 6L;
        Long inProgressCount = 20L;

        when(habitStatisticService.getAmountOfHabitsInProgressByUserId(userId)).thenReturn(inProgressCount);

        mockMvc.perform(get(HABIT_STATISTIC_LINK + "/in-progress/count")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(inProgressCount.intValue())));

        verify(habitStatisticService).getAmountOfHabitsInProgressByUserId(userId);
    }

}

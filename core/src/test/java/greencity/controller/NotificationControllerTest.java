package greencity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import greencity.annotations.CurrentUser;
import greencity.dto.PageableAdvancedDto;
import greencity.dto.notification.NotificationDto;
import greencity.dto.user.UserVO;
import greencity.exception.handler.CustomExceptionHandler;
import greencity.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationControllerTest {
    private static final String NOTIFICATIONS_LINK = "/notifications";

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;

    private UserVO mockUser;

    ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    static class TestUserArgumentResolver implements HandlerMethodArgumentResolver {
        private final UserVO userVO;

        public TestUserArgumentResolver(UserVO userVO) {
            this.userVO = userVO;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterAnnotation(CurrentUser.class) != null
                    && parameter.getParameterType().equals(UserVO.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            return userVO;
        }
    }

    @BeforeEach
    void setup() {
        mockUser = UserVO.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        DefaultErrorAttributes errorAttributes = new EventControllerTest.ForcedMessageErrorAttributes();
        CustomExceptionHandler exceptionHandler = new CustomExceptionHandler(errorAttributes, objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new TestUserArgumentResolver(mockUser))
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void getAllNotifications_Success_Returns200() throws Exception {
        // Given
        List<NotificationDto> notifications = createNotificationDtoList();
        PageableAdvancedDto<NotificationDto> pageableDto = new PageableAdvancedDto<>(
                notifications,
                2L,
                0,
                1,
                0,
                false,
                false,
                true,
                true
        );

        when(notificationService.findAllByUserId(eq(mockUser.getId()), any(Pageable.class)))
                .thenReturn(pageableDto);

        // When & Then
        mockMvc.perform(get(NOTIFICATIONS_LINK)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page").isArray())
                .andExpect(jsonPath("$.page.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.page[0].id").value(1))
                .andExpect(jsonPath("$.page[0].actorUsernames").value("john.doe"))
                .andExpect(jsonPath("$.page[0].action").value("liked"))
                .andExpect(jsonPath("$.page[0].objectTitle").value("Test News"))
                .andExpect(jsonPath("$.page[0].isRead").value(false))
                .andExpect(jsonPath("$.page[1].id").value(2))
                .andExpect(jsonPath("$.page[1].isRead").value(true));

        verify(notificationService, times(1)).findAllByUserId(eq(mockUser.getId()), any(Pageable.class));
    }

    @Test
    void getAllNotifications_EmptyResult_Returns200() throws Exception {
        // Given
        PageableAdvancedDto<NotificationDto> emptyDto = new PageableAdvancedDto<>(
                new ArrayList<>(),
                0L,
                0,
                0,
                0,
                false,
                false,
                true,
                true
        );

        when(notificationService.findAllByUserId(eq(mockUser.getId()), any(Pageable.class)))
                .thenReturn(emptyDto);

        // When & Then
        mockMvc.perform(get(NOTIFICATIONS_LINK)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page").isArray())
                .andExpect(jsonPath("$.page.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(notificationService, times(1)).findAllByUserId(eq(mockUser.getId()), any(Pageable.class));
    }

    @Test
    void getAllNotifications_WithPagination_ReturnsCorrectPage() throws Exception {
        // Given
        List<NotificationDto> notifications = createNotificationDtoList();
        PageableAdvancedDto<NotificationDto> pageableDto = new PageableAdvancedDto<>(
                notifications,
                12L,
                1,
                3,
                1,
                true,
                true,
                false,
                false
        );

        when(notificationService.findAllByUserId(eq(mockUser.getId()), any(Pageable.class)))
                .thenReturn(pageableDto);

        // When & Then
        mockMvc.perform(get(NOTIFICATIONS_LINK)
                        .param("page", "1")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.hasPrevious").value(true))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));

        verify(notificationService, times(1)).findAllByUserId(eq(mockUser.getId()), any(Pageable.class));
    }

    @Test
    void getAllNotifications_ServiceThrowsException_Returns500() throws Exception {
        // Given
        when(notificationService.findAllByUserId(eq(mockUser.getId()), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get(NOTIFICATIONS_LINK)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(notificationService, times(1)).findAllByUserId(eq(mockUser.getId()), any(Pageable.class));
    }

    @Test
    void getAllNotifications_DefaultPagination_ReturnsFirstPage() throws Exception {
        // Given
        List<NotificationDto> notifications = createNotificationDtoList();
        PageableAdvancedDto<NotificationDto> pageableDto = new PageableAdvancedDto<>(
                notifications,
                2L,
                0,
                1,
                0,
                false,
                false,
                true,
                true
        );

        when(notificationService.findAllByUserId(eq(mockUser.getId()), any(Pageable.class)))
                .thenReturn(pageableDto);

        // When & Then
        mockMvc.perform(get(NOTIFICATIONS_LINK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0));

        verify(notificationService, times(1)).findAllByUserId(eq(mockUser.getId()), any(Pageable.class));
    }

    private List<NotificationDto> createNotificationDtoList() {
        NotificationDto notification1 = NotificationDto.builder()
                .id(1L)
                .actorUsernames("john.doe")
                .action("liked")
                .objectTitle("Test News")
                .occurredAt(OffsetDateTime.now().minusHours(2))
                .isRead(false)
                .build();

        NotificationDto notification2 = NotificationDto.builder()
                .id(2L)
                .actorUsernames("jane.smith")
                .action("commented")
                .objectTitle("Another News")
                .occurredAt(OffsetDateTime.now().minusDays(1))
                .isRead(true)
                .build();

        return List.of(notification1, notification2);
    }
}


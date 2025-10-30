package greencity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import greencity.annotations.CurrentUser;
import greencity.constant.ErrorMessage;
import greencity.dto.user.UserVO;
import greencity.exception.exceptions.NotFoundException;
import greencity.exception.exceptions.SelfFriendException;
import greencity.service.FriendService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.core.MethodParameter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FriendControllerContextTest {

    private MockMvc mockMvc;

    @Mock
    private FriendService friendService;

    @InjectMocks
    private FriendController friendController;

    private final UserVO mockCurrentUser = UserVO.builder()
        .id(5L)
        .email("test@example.com")
        .name("Test User")
        .build();

    private final Long currentUserId = mockCurrentUser.getId();
    private final Long friendId = 2L;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    static class ForcedMessageErrorAttributes extends DefaultErrorAttributes {
        @Override
        public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
            ErrorAttributeOptions newOptions = options.including(
                ErrorAttributeOptions.Include.MESSAGE,
                ErrorAttributeOptions.Include.EXCEPTION);
            return super.getErrorAttributes(webRequest, newOptions);
        }
    }

    /**
     * Minimal, runnable version of the real CustomExceptionHandler for standalone
     * testing.
     */
    @ControllerAdvice
    static class TestCustomExceptionHandler extends ResponseEntityExceptionHandler {

        public TestCustomExceptionHandler(DefaultErrorAttributes errorAttributes, ObjectMapper objectMapper) {
        }

        @ExceptionHandler(NotFoundException.class)
        public ResponseEntity<Object> handleNotFoundException(NotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }

        @ExceptionHandler(SelfFriendException.class)
        public ResponseEntity<Object> handleSelfFriendException(SelfFriendException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<Object> handleGenericException(RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    /**
     * Helper to resolve @CurrentUser. Checks for the annotation to be more
     * realistic.
     */
    private static class TestUserArgumentResolver implements HandlerMethodArgumentResolver {
        private final UserVO mockUser;

        public TestUserArgumentResolver(UserVO mockUser) {
            this.mockUser = mockUser;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterAnnotation(CurrentUser.class) != null
                && parameter.getParameterType().equals(UserVO.class);
        }

        @Override
        public Object resolveArgument(@NotNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            @NotNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
            return mockUser;
        }
    }

    @BeforeEach
    void setUp() {
        ForcedMessageErrorAttributes errorAttributes = new ForcedMessageErrorAttributes();

        TestCustomExceptionHandler exceptionHandler = new TestCustomExceptionHandler(errorAttributes, objectMapper);

        this.mockMvc = MockMvcBuilders.standaloneSetup(friendController)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(),
                new TestUserArgumentResolver(mockCurrentUser))
            .setControllerAdvice(exceptionHandler)
            .build();
    }

    @Test
    void unfriendUser_Successful() throws Exception {
        doNothing().when(friendService).unfriendUser(currentUserId, friendId);

        mockMvc.perform(delete("/friends/{friendId}", friendId))
            .andExpect(status().isOk());

        verify(friendService, times(1)).unfriendUser(currentUserId, friendId);
    }

    @Test
    void unfriendUser_ThrowsSelfFriendException() throws Exception {
        doThrow(new SelfFriendException(ErrorMessage.USER_CANT_SELF_FRIEND))
            .when(friendService).unfriendUser(currentUserId, currentUserId);

        mockMvc.perform(delete("/friends/{friendId}", currentUserId))
            .andExpect(status().isBadRequest());
    }

    @Test
    void unfriendUser_ThrowsNotFoundException_FriendshipNotFound() throws Exception {
        doThrow(new NotFoundException(ErrorMessage.FRIENDSHIP_NOT_FOUND))
            .when(friendService).unfriendUser(currentUserId, friendId);

        mockMvc.perform(delete("/friends/{friendId}", friendId))
            .andExpect(status().isNotFound());
    }

    @Test
    void unfriendUser_ThrowsNotFoundException_UserOrFriendNotFound() throws Exception {
        doThrow(new NotFoundException(ErrorMessage.USER_NOT_FOUND_BY_ID + friendId))
            .when(friendService).unfriendUser(currentUserId, friendId);

        mockMvc.perform(delete("/friends/{friendId}", friendId))
            .andExpect(status().isNotFound());
    }

    @Test
    void unfriendUser_InternalServerErrorForUnexpectedException() throws Exception {
        doThrow(new RuntimeException("Database connection failed"))
            .when(friendService).unfriendUser(currentUserId, friendId);

        mockMvc.perform(delete("/friends/{friendId}", friendId))
            .andExpect(status().isInternalServerError());
    }
}
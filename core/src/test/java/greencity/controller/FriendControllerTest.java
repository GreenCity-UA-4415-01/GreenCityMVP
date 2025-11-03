package greencity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import greencity.annotations.CurrentUser;
import greencity.constant.ErrorMessage;
import greencity.dto.PageableDto;
import greencity.dto.user.UserFriendCandidateCardDto;
import greencity.dto.user.UserVO;
import greencity.exception.exceptions.NotFoundException;
import greencity.exception.exceptions.SelfFriendException;
import greencity.service.FriendService;
import greencity.exception.handler.CustomExceptionHandler;
import org.jetbrains.annotations.NotNull;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FriendControllerTest {

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

    private final PageableDto<UserFriendCandidateCardDto> mockPageableDto = new PageableDto<>(
        List.of(
            UserFriendCandidateCardDto.builder().id(10L).name("Candidate A").build(),
            UserFriendCandidateCardDto.builder().id(11L).name("Candidate B").build()),
        2, 10, 0);

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

    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    @BeforeEach
    void setUp() {
        DefaultErrorAttributes errorAttributes = new EventControllerTest.ForcedMessageErrorAttributes();

        CustomExceptionHandler exceptionHandler = new CustomExceptionHandler(errorAttributes, objectMapper);
        this.mockMvc = MockMvcBuilders.standaloneSetup(friendController)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(),
                new TestUserArgumentResolver(mockCurrentUser))
            .setControllerAdvice(exceptionHandler)
            .build();
    }

    // --- FIND NOT FRIENDS YET TESTS ---

    @Test
    void findNotFriendsYet_Successful_NoName() throws Exception {
        when(friendService.search(eq(currentUserId), eq(""), any(Pageable.class)))
                .thenReturn(mockPageableDto);

        mockMvc.perform(get("/friends/not-friends-yet")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk());

        verify(friendService, times(1)).search(eq(currentUserId), eq(""), any(Pageable.class));
    }

    @Test
    void findNotFriendsYet_Successful_WithName() throws Exception {
        String searchName = "Test";
        when(friendService.search(eq(currentUserId), eq(searchName), any(Pageable.class)))
            .thenReturn(mockPageableDto);

        mockMvc.perform(get("/friends/not-friends-yet")
            .param("name", searchName)
            .param("page", "0").param("size", "10"))
            .andExpect(status().isOk());

        verify(friendService, times(1)).search(eq(currentUserId), eq(searchName), any(Pageable.class));
    }

    // --- SEND FRIEND REQUEST TESTS ---

    @Test
    void sendFriendRequest_Successful() throws Exception {
        doNothing().when(friendService).sendFriendRequest(currentUserId, friendId);

        mockMvc.perform(post("/friends/{friendId}", friendId))
            .andExpect(status().isOk());

        verify(friendService, times(1)).sendFriendRequest(currentUserId, friendId);
    }

    @Test
    void sendFriendRequest_ThrowsSelfFriendException() throws Exception {
        doThrow(new SelfFriendException(ErrorMessage.USER_CANT_SELF_FRIEND))
            .when(friendService).sendFriendRequest(currentUserId, currentUserId);

        mockMvc.perform(post("/friends/{friendId}", currentUserId))
            .andExpect(status().isBadRequest());
    }

    @Test
    void sendFriendRequest_ThrowsNotFoundException_FriendUserNotFound() throws Exception {
        doThrow(new NotFoundException(ErrorMessage.USER_NOT_FOUND_BY_ID + friendId))
            .when(friendService).sendFriendRequest(currentUserId, friendId);

        mockMvc.perform(post("/friends/{friendId}", friendId))
            .andExpect(status().isNotFound());
    }

    @Test
    void sendFriendRequest_InternalServerErrorForUnexpectedException() throws Exception {
        doThrow(new RuntimeException("Service failure"))
            .when(friendService).sendFriendRequest(currentUserId, friendId);

        mockMvc.perform(post("/friends/{friendId}", friendId))
            .andExpect(status().isInternalServerError());
    }

    // --- CANCEL FRIEND REQUEST TESTS ---

    @Test
    void cancelRequest_Successful() throws Exception {
        doNothing().when(friendService).cancelFriendRequest(currentUserId, friendId);

        mockMvc.perform(delete("/friends/{friendId}/cancel-request", friendId))
            .andExpect(status().isOk());

        verify(friendService, times(1)).cancelFriendRequest(currentUserId, friendId);
    }

    @Test
    void cancelRequest_ThrowsNotFoundException_RequestNotFound() throws Exception {
        doThrow(new NotFoundException(ErrorMessage.FRIENDSHIP_REQUEST_NOT_FOUND))
            .when(friendService).cancelFriendRequest(currentUserId, friendId);

        mockMvc.perform(delete("/friends/{friendId}/cancel-request", friendId))
            .andExpect(status().isNotFound());
    }

    @Test
    void cancelRequest_InternalServerErrorForUnexpectedException() throws Exception {
        doThrow(new RuntimeException("Network error"))
            .when(friendService).cancelFriendRequest(currentUserId, friendId);

        mockMvc.perform(delete("/friends/{friendId}/cancel-request", friendId))
            .andExpect(status().isInternalServerError());
    }

    // --- UNFRIEND REQUEST TESTS ---

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

    // --- ACCEPT FRIEND REQUEST TESTS ---

    @Test
    void acceptFriendRequest_Successful() throws Exception {
        doNothing().when(friendService).acceptFriendRequest(currentUserId, friendId);

        mockMvc.perform(patch("/friends/{friendId}/acceptFriend", friendId))
            .andExpect(status().isOk());

        verify(friendService, times(1)).acceptFriendRequest(currentUserId, friendId);
    }

    @Test
    void acceptFriendRequest_ThrowsSelfFriendException() throws Exception {
        doThrow(new SelfFriendException(ErrorMessage.USER_CANT_SELF_FRIEND))
            .when(friendService).acceptFriendRequest(currentUserId, currentUserId);

        mockMvc.perform(patch("/friends/{friendId}/acceptFriend", currentUserId))
            .andExpect(status().isBadRequest());
    }

    @Test
    void acceptFriendRequest_ThrowsNotFoundException_RequestNotFound() throws Exception {
        doThrow(new NotFoundException(ErrorMessage.FRIENDSHIP_REQUEST_NOT_FOUND))
            .when(friendService).acceptFriendRequest(currentUserId, friendId);

        mockMvc.perform(patch("/friends/{friendId}/acceptFriend", friendId))
            .andExpect(status().isNotFound());
    }

    @Test
    void acceptFriendRequest_InternalServerErrorForUnexpectedException() throws Exception {
        doThrow(new RuntimeException("System overload"))
            .when(friendService).acceptFriendRequest(currentUserId, friendId);

        mockMvc.perform(patch("/friends/{friendId}/acceptFriend", friendId))
            .andExpect(status().isInternalServerError());
    }

    // --- REJECT FRIEND REQUEST TESTS ---

    @Test
    void rejectFriendRequest_Successful() throws Exception {
        doNothing().when(friendService).rejectFriendRequest(currentUserId, friendId);

        mockMvc.perform(delete("/friends/{friendId}/declineFriend", friendId))
            .andExpect(status().isOk());

        verify(friendService, times(1)).rejectFriendRequest(currentUserId, friendId);
    }

    @Test
    void rejectFriendRequest_ThrowsNotFoundException_RequestNotFound() throws Exception {
        doThrow(new NotFoundException(ErrorMessage.FRIENDSHIP_REQUEST_NOT_FOUND))
            .when(friendService).rejectFriendRequest(currentUserId, friendId);

        mockMvc.perform(delete("/friends/{friendId}/declineFriend", friendId))
            .andExpect(status().isNotFound());
    }

    @Test
    void rejectFriendRequest_InternalServerErrorForUnexpectedException() throws Exception {
        doThrow(new RuntimeException("Timeout error"))
            .when(friendService).rejectFriendRequest(currentUserId, friendId);

        mockMvc.perform(delete("/friends/{friendId}/declineFriend", friendId))
            .andExpect(status().isInternalServerError());
    }
}
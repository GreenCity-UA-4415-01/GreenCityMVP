package greencity.service;

import greencity.constant.ErrorMessage;
import greencity.entity.User;
import greencity.exception.exceptions.NotFoundException;
import greencity.exception.exceptions.SelfFriendException;
import greencity.repository.FriendshipRepo;
import greencity.repository.FriendshipRequestRepo;
import greencity.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendServiceImplTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private FriendshipRequestRepo friendshipRequestRepo;

    @Mock
    private FriendshipRepo friendshipRepo;

    @InjectMocks
    private FriendServiceImpl friendService;

    private final Long userId = 1L;
    private final Long friendId = 2L;
    private User user;
    private User friend;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(userId);
        friend = new User();
        friend.setId(friendId);
    }

    @Test
    void unfriendUser_SuccessfulUnfriend() {
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.findById(friendId)).thenReturn(Optional.of(friend));
        when(friendshipRepo.existsByUserIdAndFriendId(userId, friendId)).thenReturn(true);

        assertDoesNotThrow(() -> friendService.unfriendUser(userId, friendId));

        verify(friendshipRepo, times(1)).existsByUserIdAndFriendId(userId, friendId);
        verify(friendshipRepo, times(1)).deleteByUserIdAndFriendId(userId, friendId);
    }

    @Test
    void unfriendUser_ThrowsSelfFriendException() {
        SelfFriendException thrown = assertThrows(SelfFriendException.class,
            () -> friendService.unfriendUser(userId, userId));

        verify(userRepo, never()).findById(anyLong());
        verify(friendshipRepo, never()).existsByUserIdAndFriendId(anyLong(), anyLong());
        verify(friendshipRepo, never()).deleteByUserIdAndFriendId(anyLong(), anyLong());

        String expectedMessage = ErrorMessage.USER_CANT_SELF_FRIEND;
        assert (thrown.getMessage().contains(expectedMessage));
    }

    @Test
    void unfriendUser_ThrowsNotFoundException_UserNotFound() {
        when(userRepo.findById(userId)).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> friendService.unfriendUser(userId, friendId));

        verify(userRepo, times(1)).findById(userId);
        verify(userRepo, never()).findById(friendId);
        verify(friendshipRepo, never()).existsByUserIdAndFriendId(anyLong(), anyLong());

        String expectedMessage = ErrorMessage.USER_NOT_FOUND_BY_ID + userId;
        assert(thrown.getMessage().contains(expectedMessage));
    }

    @Test
    void unfriendUser_ThrowsNotFoundException_FriendNotFound() {
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.findById(friendId)).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> friendService.unfriendUser(userId, friendId));

        verify(userRepo, times(1)).findById(userId);
        verify(userRepo, times(1)).findById(friendId);
        verify(friendshipRepo, never()).existsByUserIdAndFriendId(anyLong(), anyLong());

        String expectedMessage = ErrorMessage.USER_NOT_FOUND_BY_ID + friendId;
        assert(thrown.getMessage().contains(expectedMessage));
    }

    @Test
    void unfriendUser_ThrowsNotFoundException_FriendshipNotFound() {
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.findById(friendId)).thenReturn(Optional.of(friend));
        when(friendshipRepo.existsByUserIdAndFriendId(userId, friendId)).thenReturn(false);

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> friendService.unfriendUser(userId, friendId));

        verify(friendshipRepo, times(1)).existsByUserIdAndFriendId(userId, friendId);
        verify(friendshipRepo, never()).deleteByUserIdAndFriendId(anyLong(), anyLong());

        String expectedMessage = ErrorMessage.FRIENDSHIP_NOT_FOUND;
        assert(thrown.getMessage().contains(expectedMessage));
    }

    @Test
    void validateUsersPair_ValidUsersPair() {
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.findById(friendId)).thenReturn(Optional.of(friend));

        when(friendshipRepo.existsByUserIdAndFriendId(userId, friendId)).thenReturn(true);
        assertDoesNotThrow(() -> friendService.unfriendUser(userId, friendId));

        verify(userRepo, times(1)).findById(userId);
        verify(userRepo, times(1)).findById(friendId);
    }

    @Test
    void validateUsersPair_ThrowsSelfFriendException() {
        SelfFriendException thrown = assertThrows(SelfFriendException.class,
            () -> friendService.unfriendUser(userId, userId));

        verify(userRepo, never()).findById(anyLong());
        String expectedMessage = ErrorMessage.USER_CANT_SELF_FRIEND;
        assert (thrown.getMessage().contains(expectedMessage));
    }

    @Test
    void validateUsersPair_ThrowsNotFoundException_UserNotFound() {
        when(userRepo.findById(userId)).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> friendService.unfriendUser(userId, friendId));

        verify(userRepo, times(1)).findById(userId);
        verify(userRepo, never()).findById(friendId);
        String expectedMessage = ErrorMessage.USER_NOT_FOUND_BY_ID + userId;
        assert(thrown.getMessage().contains(expectedMessage));
    }

    @Test
    void validateUsersPair_ThrowsNotFoundException_FriendNotFound() {
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.findById(friendId)).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> friendService.unfriendUser(userId, friendId));

        verify(userRepo, times(1)).findById(userId);
        verify(userRepo, times(1)).findById(friendId);
        String expectedMessage = ErrorMessage.USER_NOT_FOUND_BY_ID + friendId;
        assert(thrown.getMessage().contains(expectedMessage));
    }
}

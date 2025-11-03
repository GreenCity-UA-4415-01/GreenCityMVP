package greencity.service;

import greencity.constant.ErrorMessage;
import greencity.dto.PageableDto;
import greencity.dto.user.UserFriendCandidateCardDto;
import greencity.dto.user.UserFriendCardDto;
import greencity.entity.Friendship;
import greencity.entity.User;
import greencity.exception.exceptions.FriendExistsException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
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

    private Page<User> createMockUserPage(Pageable pageable) {
        User candidate1 = new User();
        candidate1.setId(10L);
        candidate1.setName("User Candidate A");
        candidate1.setCity("Lviv");
        candidate1.setProfilePicturePath("path/to/a.jpg");
        candidate1.setRating(500.0);

        User candidate2 = new User();
        candidate2.setId(11L);
        candidate2.setName("User Candidate B");
        candidate2.setCity("Kyiv");
        candidate2.setProfilePicturePath("path/to/b.jpg");
        candidate2.setRating(600.0);

        List<User> userList = List.of(candidate1, candidate2);
        return new PageImpl<>(userList, pageable, userList.size());
    }

    @Test
    void search_Successful_NoQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = createMockUserPage(pageable);

        when(userRepo.searchCandidates(userId, "", pageable)).thenReturn(userPage);
        when(friendshipRequestRepo.existsPending(userId, 10L)).thenReturn(true);
        when(friendshipRequestRepo.existsPending(userId, 11L)).thenReturn(false);

        PageableDto<UserFriendCandidateCardDto> result = friendService.search(userId, null, pageable);

        assertNotNull(result);
        assertEquals(2, result.getPage().size());
        assertTrue(result.getPage().get(0).getRequestSent());
        assertFalse(result.getPage().get(1).getRequestSent());
        verify(userRepo, times(1)).searchCandidates(userId, "", pageable);
    }

    @Test
    void search_Successful_WithQuery_Truncated() {
        String longQuery =
            "This is a very long query string that is way over thirty characters long and should be truncated.";
        String truncatedQuery = longQuery.substring(0, 30);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = createMockUserPage(pageable);

        when(userRepo.searchCandidates(userId, truncatedQuery, pageable)).thenReturn(userPage);
        when(friendshipRequestRepo.existsPending(eq(userId), anyLong())).thenReturn(false);

        PageableDto<UserFriendCandidateCardDto> result = friendService.search(userId, longQuery, pageable);

        assertNotNull(result);
        verify(userRepo, times(1)).searchCandidates(userId, truncatedQuery, pageable);
    }

    // --- SEND FRIEND REQUEST TESTS ---

    @Test
    void sendFriendRequest_Successful() {
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.findById(friendId)).thenReturn(Optional.of(friend));
        when(friendshipRequestRepo.areAlreadyFriends(userId, friendId)).thenReturn(false);
        when(friendshipRequestRepo.existsPending(userId, friendId)).thenReturn(false);

        assertDoesNotThrow(() -> friendService.sendFriendRequest(userId, friendId));

        verify(friendshipRequestRepo, times(1)).insertPending(userId, friendId);
    }

    @Test
    void sendFriendRequest_ThrowsFriendExistsException_AlreadyFriends() {
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.findById(friendId)).thenReturn(Optional.of(friend));
        when(friendshipRequestRepo.areAlreadyFriends(userId, friendId)).thenReturn(true);

        FriendExistsException thrown = assertThrows(FriendExistsException.class,
                () -> friendService.sendFriendRequest(userId, friendId));

        assertEquals(ErrorMessage.FRIENDSHIP_ALREADY_EXISTS, thrown.getMessage());
        verify(friendshipRequestRepo, never()).insertPending(anyLong(), anyLong());
    }

    @Test
    void sendFriendRequest_ThrowsFriendExistsException_RequestAlreadyPending() {
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.findById(friendId)).thenReturn(Optional.of(friend));
        when(friendshipRequestRepo.areAlreadyFriends(userId, friendId)).thenReturn(false);
        when(friendshipRequestRepo.existsPending(userId, friendId)).thenReturn(true);

        FriendExistsException thrown = assertThrows(FriendExistsException.class,
                () -> friendService.sendFriendRequest(userId, friendId));

        assertEquals(ErrorMessage.FRIENDSHIP_REQUEST_ALREADY_EXISTS, thrown.getMessage());
        verify(friendshipRequestRepo, never()).insertPending(anyLong(), anyLong());
    }

    // --- CANCEL FRIEND REQUEST TESTS ---

    @Test
    void cancelFriendRequest_Successful() {
        when(friendshipRequestRepo.deletePending(userId, friendId)).thenReturn(1);

        assertDoesNotThrow(() -> friendService.cancelFriendRequest(userId, friendId));

        verify(friendshipRequestRepo, times(1)).deletePending(userId, friendId);
    }

    // --- UNFRIEND USER TESTS ---

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
        assertTrue(thrown.getMessage().contains(expectedMessage));
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
        assertTrue(thrown.getMessage().contains(expectedMessage));
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
        assertTrue(thrown.getMessage().contains(expectedMessage));
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
        assertTrue(thrown.getMessage().contains(expectedMessage));
    }

    // --- ACCEPT FRIEND REQUEST TESTS ---

    @Test
    void acceptFriendRequest_Successful() {
        Long me = userId;
        Long requesterId = friendId;
        when(friendshipRequestRepo.existsPending(requesterId, me)).thenReturn(true);
        when(friendshipRequestRepo.areAlreadyFriends(me, requesterId)).thenReturn(false);

        when(friendshipRequestRepo.deletePendingOneDirection(me, requesterId)).thenReturn(1);
        when(friendshipRequestRepo.deletePendingOneDirection(requesterId, me)).thenReturn(1);

        assertDoesNotThrow(() -> friendService.acceptFriendRequest(me, requesterId));

        verify(friendshipRepo, times(2)).save(any(Friendship.class));
        verify(friendshipRequestRepo, times(1)).deletePendingOneDirection(requesterId, me);
        verify(friendshipRequestRepo, times(1)).deletePendingOneDirection(me, requesterId);
    }

    @Test
    void acceptFriendRequest_ThrowsSelfFriendException() {
        SelfFriendException thrown = assertThrows(SelfFriendException.class,
            () -> friendService.acceptFriendRequest(userId, userId));

        assertEquals(ErrorMessage.USER_CANT_SELF_FRIEND, thrown.getMessage());
        verify(friendshipRequestRepo, never()).existsPending(anyLong(), anyLong());
        verify(friendshipRepo, never()).save(any(Friendship.class));
    }

    @Test
    void acceptFriendRequest_ThrowsNotFoundException_RequestNotFound() {
        Long me = userId;
        Long requesterId = friendId;

        when(friendshipRequestRepo.existsPending(requesterId, me)).thenReturn(false);

        NotFoundException thrown = assertThrows(NotFoundException.class,
            () -> friendService.acceptFriendRequest(me, requesterId));

        assertEquals(ErrorMessage.FRIENDSHIP_REQUEST_NOT_FOUND, thrown.getMessage());
        verify(friendshipRequestRepo, times(1)).existsPending(requesterId, me);
        verify(friendshipRepo, never()).save(any(Friendship.class));
    }

    @Test
    void acceptFriendRequest_AlreadyFriends_DeletesRequestAndExits() {
        Long me = userId;
        Long requesterId = friendId;

        when(friendshipRequestRepo.existsPending(requesterId, me)).thenReturn(true);
        when(friendshipRequestRepo.areAlreadyFriends(me, requesterId)).thenReturn(true);

        when(friendshipRequestRepo.deletePending(requesterId, me)).thenReturn(1);

        assertDoesNotThrow(() -> friendService.acceptFriendRequest(me, requesterId));

        verify(friendshipRequestRepo, times(1)).deletePending(requesterId, me);
        verify(friendshipRepo, never()).save(any(Friendship.class));
        verify(friendshipRequestRepo, never()).deletePendingOneDirection(anyLong(), anyLong());
    }

    // --- REJECT FRIEND REQUEST TESTS ---

    @Test
    void rejectFriendRequest_Successful() {
        Long me = userId;
        Long requesterId = friendId;

        when(friendshipRequestRepo.deletePending(requesterId, me)).thenReturn(1);

        assertDoesNotThrow(() -> friendService.rejectFriendRequest(me, requesterId));

        verify(friendshipRequestRepo, times(1)).deletePending(requesterId, me);
    }
}

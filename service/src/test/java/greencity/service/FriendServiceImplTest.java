package greencity.service;

import greencity.constant.ErrorMessage;
import greencity.dto.PageableDto;
import greencity.dto.user.FriendProfileDto;
import greencity.dto.user.FriendShortDto;
import greencity.dto.user.UserFriendCandidateCardDto;
import greencity.dto.user.UserFriendCardDto;
import greencity.entity.FriendRequest;
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

    @Mock
    private UserService userService;

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
        friend.setName("Test Friend");
        friend.setProfilePicturePath("friend/pic.jpg");
        friend.setCity("Kyiv");
        friend.setRating(700.0);
        friend.setUserCredo("My Personal Credo");
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

    // --- LIST FRIENDS TEST ---

    @Test
    void listFriends_Successful() {
        Pageable pageable = PageRequest.of(0, 10);

        User friend1 = new User();
        friend1.setId(10L);
        friend1.setName("Friend One");
        friend1.setProfilePicturePath("pic1.jpg");
        friend1.setCity("City One");
        friend1.setRating(100.0);

        User friend2 = new User();
        friend2.setId(11L);
        friend2.setName("Friend Two");
        friend2.setProfilePicturePath("pic2.jpg");
        friend2.setCity("City Two");
        friend2.setRating(200.0);

        List<User> friendsList = List.of(friend1, friend2);
        Page<User> friendsPage = new PageImpl<>(friendsList, pageable, friendsList.size());

        when(userRepo.findFriendsPage(userId, pageable)).thenReturn(friendsPage);
        when(userRepo.countMutualFriends(userId, 10L)).thenReturn(5L);
        when(userRepo.countMutualFriends(userId, 11L)).thenReturn(0L);

        PageableDto<UserFriendCardDto> result = friendService.listFriends(userId, pageable);

        assertNotNull(result);
        assertEquals(2, result.getPage().size());
        assertEquals(10L, result.getPage().get(0).getId());
        assertEquals(5, result.getPage().get(0).getMutualFriends());
        assertEquals(0, result.getPage().get(1).getMutualFriends());

        verify(userRepo, times(1)).findFriendsPage(userId, pageable);
        verify(userRepo, times(2)).countMutualFriends(eq(userId), anyLong());
    }

    // --- TOP FRIENDS TEST ---

    @Test
    void topFriends_Successful() {
        User topFriend1 = new User();
        topFriend1.setId(10L);
        topFriend1.setName("Top A");
        topFriend1.setProfilePicturePath("topA.jpg");

        User topFriend2 = new User();
        topFriend2.setId(11L);
        topFriend2.setName("Top B");
        topFriend2.setProfilePicturePath("topB.jpg");

        List<User> mockList = List.of(topFriend1, topFriend2);

        when(userRepo.findTopFriendsForBlock(userId)).thenReturn(mockList);
        when(userService.checkIfTheUserIsOnline(10L)).thenReturn(true);
        when(userService.checkIfTheUserIsOnline(11L)).thenReturn(false);

        List<FriendShortDto> result = friendService.topFriends(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getId());
        assertTrue(result.get(0).getOnline());
        assertFalse(result.get(1).getOnline());

        verify(userRepo, times(1)).findTopFriendsForBlock(userId);
        verify(userService, times(2)).checkIfTheUserIsOnline(anyLong());
    }

    // --- FRIEND PROFILE TESTS ---

    @Test
    void friendProfile_Successful() {
        Long me = userId;
        Long profileId = friendId;

        when(friendshipRepo.existsByUserIdAndFriendId(me, profileId)).thenReturn(false);
        when(friendshipRepo.existsByUserIdAndFriendId(profileId, me)).thenReturn(true);

        when(userRepo.findById(profileId)).thenReturn(Optional.of(friend));

        when(userService.checkIfTheUserIsOnline(profileId)).thenReturn(true);

        FriendProfileDto result = friendService.friendProfile(me, profileId);

        assertNotNull(result);
        assertEquals(profileId, result.getId());
        assertEquals("Test Friend", result.getName());
        assertEquals("friend/pic.jpg", result.getProfilePicture());
        assertTrue(result.getOnline());
        assertEquals(700.0, result.getPersonalRate());
        assertEquals("My Personal Credo", result.getCredo());

        verify(friendshipRepo, times(1)).existsByUserIdAndFriendId(me, profileId);
        verify(friendshipRepo, times(1)).existsByUserIdAndFriendId(profileId, me);
        verify(userRepo, times(1)).findById(profileId);
        verify(userService, times(1)).checkIfTheUserIsOnline(profileId);
    }

    @Test
    void friendProfile_ThrowsNotFoundException_NotFriends() {
        Long me = userId;
        Long profileId = friendId;

        // Mock friendship non-existence
        when(friendshipRepo.existsByUserIdAndFriendId(me, profileId)).thenReturn(false);
        when(friendshipRepo.existsByUserIdAndFriendId(profileId, me)).thenReturn(false);

        NotFoundException thrown = assertThrows(NotFoundException.class,
            () -> friendService.friendProfile(me, profileId));

        assertEquals(ErrorMessage.FRIENDSHIP_NOT_FOUND, thrown.getMessage());

        verify(friendshipRepo, times(1)).existsByUserIdAndFriendId(me, profileId);
        verify(friendshipRepo, times(1)).existsByUserIdAndFriendId(profileId, me);
        verify(userRepo, never()).findById(anyLong());
    }

    // --- SEARCH FRIENDS TESTS ---

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

        when(friendshipRepo.existsByUserIdAndFriendId(userId, friendId)).thenReturn(false);
        when(friendshipRepo.existsByUserIdAndFriendId(friendId, userId)).thenReturn(true);

        assertDoesNotThrow(() -> friendService.unfriendUser(userId, friendId));

        verify(friendshipRepo, times(1)).existsByUserIdAndFriendId(userId, friendId);
        verify(friendshipRepo, times(1)).existsByUserIdAndFriendId(friendId, userId);

        verify(friendshipRepo, times(1)).deleteBothDirections(userId, friendId);
        verify(friendshipRepo, never()).deleteByUserIdAndFriendId(anyLong(), anyLong());
    }

    @Test
    void unfriendUser_ThrowsSelfFriendException() {
        SelfFriendException thrown = assertThrows(SelfFriendException.class,
            () -> friendService.unfriendUser(userId, userId));

        verify(userRepo, never()).findById(anyLong());
        verify(friendshipRepo, never()).existsByUserIdAndFriendId(anyLong(), anyLong());
        verify(friendshipRepo, never()).deleteBothDirections(anyLong(), anyLong());

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
        when(friendshipRepo.existsByUserIdAndFriendId(friendId, userId)).thenReturn(false);

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> friendService.unfriendUser(userId, friendId));

        verify(friendshipRepo, times(1)).existsByUserIdAndFriendId(userId, friendId);
        verify(friendshipRepo, times(1)).existsByUserIdAndFriendId(friendId, userId);

        verify(friendshipRepo, never()).deleteBothDirections(anyLong(), anyLong());

        String expectedMessage = ErrorMessage.FRIENDSHIP_NOT_FOUND;
        assertTrue(thrown.getMessage().contains(expectedMessage));
    }

    // --- ACCEPT FRIEND REQUEST TESTS ---

    @Test
    void acceptFriendRequest_Successful() {
        Long me = userId;
        Long requesterId = friendId;
        when(friendshipRequestRepo.existsPending(requesterId, me)).thenReturn(true);

        when(friendshipRepo.existsByUserIdAndFriendId(me, requesterId)).thenReturn(false);
        when(friendshipRepo.existsByUserIdAndFriendId(requesterId, me)).thenReturn(false);

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
        when(friendshipRepo.existsByUserIdAndFriendId(me, requesterId)).thenReturn(true);

        assertDoesNotThrow(() -> friendService.acceptFriendRequest(me, requesterId));

        verify(friendshipRequestRepo, times(1)).deletePendingOneDirection(requesterId, me);
        verify(friendshipRequestRepo, never()).deletePendingOneDirection(me, requesterId);
        verify(friendshipRepo, never()).save(any(Friendship.class));
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

    // --- FRIEND REQUESTS TESTS (NEW COVERAGE) ---

    @Test
    void friendRequests_Successful_NoMutuals() {
        Long receiverId = userId;
        Long requesterAId = 10L;
        Long requesterBId = 11L;

        FriendRequest reqA = new FriendRequest();
        reqA.setRequesterId(requesterAId);
        reqA.setReceiverId(receiverId);

        FriendRequest reqB = new FriendRequest();
        reqB.setRequesterId(requesterBId);
        reqB.setReceiverId(receiverId);

        List<FriendRequest> requestList = List.of(reqA, reqB);
        Pageable pageable = PageRequest.of(0, 10);
        Page<FriendRequest> requestPage = new PageImpl<>(requestList, pageable, requestList.size());

        User requesterA = new User();
        requesterA.setId(requesterAId);
        requesterA.setName("Alice");
        requesterA.setCity("Paris");
        requesterA.setProfilePicturePath("alice.jpg");
        requesterA.setRating(900.0);

        User requesterB = new User();
        requesterB.setId(requesterBId);
        requesterB.setName("Bob");
        requesterB.setCity("Berlin");
        requesterB.setProfilePicturePath("bob.jpg");
        requesterB.setRating(850.0);

        List<User> requesters = List.of(requesterA, requesterB);

        when(friendshipRequestRepo.findAllPendingRequestsByReceiverId(receiverId, pageable))
            .thenReturn(requestPage);
        when(userRepo.findAllById(List.of(requesterAId, requesterBId)))
            .thenReturn(requesters);

        PageableDto<UserFriendCandidateCardDto> result = friendService.friendRequests(receiverId, pageable);

        assertNotNull(result);
        assertEquals(2, result.getPage().size());
        assertEquals(2L, result.getTotalElements());

        UserFriendCandidateCardDto dtoA = result.getPage().getFirst();
        assertEquals(requesterAId, dtoA.getId());
        assertEquals("Alice", dtoA.getName());
        assertEquals(0L, dtoA.getMutualFriends(), "Mutual friends should be 0");
        assertFalse(dtoA.getRequestSent(), "RequestSent should be false for incoming requests");

        verify(friendshipRequestRepo, times(1)).findAllPendingRequestsByReceiverId(receiverId, pageable);
        verify(userRepo, times(1)).findAllById(List.of(requesterAId, requesterBId));
    }

    @Test
    void friendRequests_NoRequests_ReturnsEmptyPage() {
        Long receiverId = userId;
        Pageable pageable = PageRequest.of(0, 10);
        Page<FriendRequest> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(friendshipRequestRepo.findAllPendingRequestsByReceiverId(receiverId, pageable))
            .thenReturn(emptyPage);

        PageableDto<UserFriendCandidateCardDto> result = friendService.friendRequests(receiverId, pageable);

        assertNotNull(result);
        assertTrue(result.getPage().isEmpty());
        assertEquals(0L, result.getTotalElements());

        verify(friendshipRequestRepo, times(1)).findAllPendingRequestsByReceiverId(receiverId, pageable);
        verify(userRepo, never()).findAllById(anyList());
    }
}

package greencity.service;


import greencity.entity.Friendship;
import greencity.enums.FriendshipStatus;
import greencity.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {
    private final FriendshipRepository repo;

    @Transactional(readOnly = true)
    @Override
    public Page<Long> findFriendIds(Long me, Pageable pageable) {
        return repo.findFriends(me, pageable)
                .map(f -> f.getUserId().equals(me) ? f.getFriendId() : f.getUserId());
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Long> findIncomingRequestUserIds(Long me, Pageable pageable) {
        return repo.findIncomingRequests(me, pageable).map(Friendship::getUserId);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Long> findNotFriendsYetUserIds(Long me, String name, Pageable pageable) {
        String q = (name == null || name.isBlank()) ? null : name;
        return repo.findNotFriendsYet(me, q, pageable);
    }

    @Transactional
    @Override
    public void addFriendRequest(Long me, Long friendId) {
        if (me.equals(friendId)) {
            throw new IllegalArgumentException("Cannot add yourself as a friend");
        }
        if (repo.findAnyLink(me, friendId).isPresent()) {
            return; // вже є заявка/дружба — ідемпотентність
        }
        repo.save(Friendship.builder()
                .userId(me)
                .friendId(friendId)
                .status(FriendshipStatus.PENDING)
                .build());
    }

    @Transactional
    @Override
    public void acceptFriendRequest(Long me, Long friendId) {
        var f = repo.findAnyLink(friendId, me)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));
        if (!f.getFriendId().equals(me) || f.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Not an incoming pending request");
        }
        f.setStatus(FriendshipStatus.ACCEPTED);
        repo.save(f);
    }

    @Transactional
    @Override
    public void declineFriendRequest(Long me, Long friendId) {
        var f = repo.findAnyLink(friendId, me)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));
        if (!f.getFriendId().equals(me) || f.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Not an incoming pending request");
        }
        // або помічати DECLINED, але простіше — видалити
        repo.delete(f);
    }

    @Transactional
    @Override
    public void deleteFriend(Long me, Long friendId) {
        var f = repo.findAnyLink(me, friendId)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));
        if (f.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new IllegalStateException("Not an accepted friendship");
        }
        repo.delete(f);
    }
}

package greencity.repository;

import greencity.entity.Friendship;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    @Query("""
        select f from Friendship f
        where (f.userId=:a and f.friendId=:b) or (f.userId=:b and f.friendId=:a)
    """)
    Optional<Friendship> findAnyLink(@Param("a") Long a, @Param("b") Long b);

    @Query("""
        select f from Friendship f
        where f.status=greencity.enums.FriendshipStatus.ACCEPTED
          and (f.userId=:me or f.friendId=:me)
        order by f.createdAt desc
    """)
    Page<Friendship> findFriends(@Param("me") Long me, Pageable pageable);

    @Query("""
        select f from Friendship f
        where f.status=greencity.enums.FriendshipStatus.PENDING
          and f.friendId=:me
        order by f.createdAt desc
    """)
    Page<Friendship> findIncomingRequests(@Param("me") Long me, Pageable pageable);

    @Query(value = """
        select u.id
        from users u
        where u.id <> :me
          and (:name is null or lower(u.name) like lower(concat('%',:name,'%')))
          and not exists (
            select 1 from friendship f
            where (f.user_id=:me and f.friend_id=u.id) or (f.user_id=u.id and f.friend_id=:me)
          )
        order by u.id desc
        """,
            countQuery = """
        select count(*)
        from users u
        where u.id <> :me
          and (:name is null or lower(u.name) like lower(concat('%',:name,'%')))
          and not exists (
            select 1 from friendship f
            where (f.user_id=:me and f.friend_id=u.id) or (f.user_id=u.id and f.friend_id=:me)
          )
        """,
            nativeQuery = true)
    Page<Long> findNotFriendsYet(@Param("me") Long me, @Param("name") String name, Pageable pageable);
}

package greencity.repository;

import greencity.dto.habit.HabitVO;
import greencity.dto.user.UserManagementVO;
import greencity.dto.user.UserVO;
import greencity.entity.User;
import greencity.repository.options.UserFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    /**
     * Find {@link User} by email.
     *
     * @param email user email.
     * @return {@link User}
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all {@link UserManagementVO}.
     *
     * @param filter   filter parameters
     * @param pageable pagination
     * @return list of all {@link UserManagementVO}
     */
    @Query(" SELECT new greencity.dto.user.UserManagementVO(u.id, u.name, u.email, u.userCredo, u.role, u.userStatus) "
        + " FROM User u ")
    Page<UserManagementVO> findAllManagementVo(UserFilter filter, Pageable pageable);

    /**
     * Find not 'DEACTIVATED' {@link User} by email.
     *
     * @param email - {@link User}'s email
     * @return found {@link User}
     * @author Vasyl Zhovnir
     */
    @Query("FROM User WHERE email=:email AND userStatus <> 1")
    Optional<User> findNotDeactivatedByEmail(String email);

    /**
     * Find id by email.
     *
     * @param email - User email
     * @return User id
     * @author Zakhar Skaletskyi
     */
    @Query("SELECT id FROM User WHERE email=:email")
    Optional<Long> findIdByEmail(String email);

    /**
     * Updates last activity time for a given user.
     *
     * @param userId               - {@link User}'s id
     * @param userLastActivityTime - new {@link User}'s last activity time
     * @author Yurii Zhurakovskyi
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastActivityTime = :userLastActivityTime WHERE u.id = :userId")
    void updateUserLastActivityTime(Long userId, Date userLastActivityTime);

    /**
     * Updates user status for a given user.
     *
     * @param userId     - {@link User}'s id
     * @param userStatus {@link String} - string value of user status to set
     */
    @Modifying
    @Transactional
    @Query("UPDATE User SET userStatus = CASE "
        + "WHEN (:userStatus = 'DEACTIVATED') THEN 1 "
        + "WHEN (:userStatus = 'ACTIVATED') THEN 2 "
        + "WHEN (:userStatus = 'CREATED') THEN 3 "
        + "WHEN (:userStatus = 'BLOCKED') THEN 4 "
        + "ELSE 0 END "
        + "WHERE id = :userId")
    void updateUserStatus(Long userId, String userStatus);

    /**
     * Find the last activity time by {@link User}'s id.
     *
     * @param userId - {@link User}'s id
     * @return {@link Date}
     */
    @Query(nativeQuery = true,
        value = "SELECT last_activity_time FROM users WHERE id=:userId")
    Optional<Timestamp> findLastActivityTimeById(Long userId);

    /**
     * Updates user rating as event organizer.
     *
     * @param userId {@link User}'s id
     * @param rate   new {@link User}'s rating as event organizer
     * @author Danylo Hlynskyi
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE User SET eventOrganizerRating=:rate WHERE id=:userId")
    void updateUserEventOrganizerRating(Long userId, Double rate);

    /**
     * Retrieves the list of the user's friends (which have INPROGRESS assign to the
     * habit).
     *
     * @param habitId {@link HabitVO} id.
     * @param userId  {@link UserVO} id.
     * @return List of friends.
     */
    @Query(nativeQuery = true, value = "SELECT * FROM ((SELECT user_id FROM users_friends AS uf "
        + "WHERE uf.friend_id = :userId AND uf.status = 'FRIEND' AND "
        + "(SELECT count(*) FROM habit_assign ha WHERE ha.habit_id = :habitId AND ha.user_id = uf.user_id "
        + "AND ha.status = 'INPROGRESS') = 1) "
        + "UNION "
        + "(SELECT friend_id FROM users_friends AS uf "
        + "WHERE uf.user_id = :userId AND uf.status = 'FRIEND' AND "
        + "(SELECT count(*) FROM habit_assign ha WHERE ha.habit_id = :habitId AND ha.user_id = uf.friend_id "
        + "AND ha.status = 'INPROGRESS') = 1)) as ui JOIN users as u ON user_id = u.id")
    List<User> getFriendsAssignedToHabit(Long userId, Long habitId);

    /**
     * Get all user friends.
     *
     * @param userId The ID of the user.
     *
     * @return list of {@link User}.
     */
    @Query(nativeQuery = true, value = "SELECT * FROM users WHERE id IN ( "
        + "(SELECT user_id FROM users_friends WHERE friend_id = :userId and status = 'FRIEND')"
        + "UNION (SELECT friend_id FROM users_friends WHERE user_id = :userId and status = 'FRIEND'));")
    List<User> getAllUserFriends(Long userId);

    /**
     * Search for friend candidates: excludes the current user and existing friends
     * (in both directions). Searches by name, case-insensitive, matching any part
     * of the string. The query parameter q may be NULL or empty. Tables: users,
     * friendships.
     *
     * @param userId   {@link UserVO} id.
     * @param q        search query string (optional).
     * @param pageable pagination parameters.
     * @return page of {@link User}.
     * @author Misha Moroz
     */
    @Query(
        value = """
            SELECT u.*
            FROM users u
            WHERE u.id <> :userId
              AND (
                    :q IS NULL OR :q = '' OR
                    LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
              AND NOT EXISTS (
                  SELECT 1
                  FROM friendships f
                  WHERE (f.user_id = :userId AND f.friend_id = u.id)
                     OR (f.user_id = u.id AND f.friend_id = :userId)
              )
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM users u
            WHERE u.id <> :userId
              AND (
                    :q IS NULL OR :q = '' OR
                    LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
              AND NOT EXISTS (
                  SELECT 1
                  FROM friendships f
                  WHERE (f.user_id = :userId AND f.friend_id = u.id)
                     OR (f.user_id = u.id AND f.friend_id = :userId)
              )
            """,
        nativeQuery = true)
    Page<User> searchCandidates(@Param("userId") Long userId,
        @Param("q") String q,
        Pageable pageable);

    @Query(
        value = """
                SELECT u.* FROM users u
                WHERE u.id IN (
                    SELECT f.friend_id FROM friendships f WHERE f.user_id = :userId
                    UNION -- UNION is correct to get distinct IDs
                    SELECT f.user_id FROM friendships f WHERE f.friend_id = :userId
                )
                ORDER BY u.id
            """,
        countQuery = """
                SELECT COUNT(*) FROM users u
                WHERE u.id IN (
                    SELECT f.friend_id FROM friendships f WHERE f.user_id = :userId
                    UNION
                    SELECT f.user_id FROM friendships f WHERE f.friend_id = :userId
                )
            """,
        nativeQuery = true)
    Page<User> findFriendsPage(@Param("userId") Long userId, Pageable pageable);

    @Query(value = """
        WITH my_habits AS (
            SELECT ha.habit_id
            FROM habit_assign ha
            WHERE ha.user_id = :userId
        ),
        my_city AS (
            SELECT COALESCE(u.city, '') AS city FROM users u WHERE u.id = :userId
        )
        SELECT u.*,
               (SELECT COUNT(*) FROM habit_assign ha
                 WHERE ha.user_id = u.id
                   AND ha.habit_id IN (SELECT habit_id FROM my_habits)
               ) AS common_habits,
               (CASE WHEN LOWER(u.city) = LOWER((SELECT city FROM my_city)) THEN 1 ELSE 0 END) AS same_city
        FROM users u
        WHERE u.id IN (
            SELECT f.friend_id FROM friendships f WHERE f.user_id = :userId
            UNION
            SELECT f.user_id   FROM friendships f WHERE f.friend_id = :userId
        )
        ORDER BY common_habits DESC,
                 same_city DESC,
                 u.rating DESC NULLS LAST,
                 u.name
        LIMIT 6
        """, nativeQuery = true)
    List<User> findTopFriendsForBlock(@Param("userId") Long userId);

    @Query(value = """
        SELECT COUNT(*) FROM (
          SELECT DISTINCT CASE WHEN f.user_id = :me THEN f.friend_id ELSE f.user_id END AS id
          FROM friendships f
          WHERE f.user_id = :me OR f.friend_id = :me
        ) my
        JOIN (
          SELECT DISTINCT CASE WHEN f.user_id = :friend THEN f.friend_id ELSE f.user_id END AS id
          FROM friendships f
          WHERE f.user_id = :friend OR f.friend_id = :friend
        ) fr ON fr.id = my.id
        """, nativeQuery = true)
    long countMutualFriends(@Param("me") Long me, @Param("friend") Long friend);
}
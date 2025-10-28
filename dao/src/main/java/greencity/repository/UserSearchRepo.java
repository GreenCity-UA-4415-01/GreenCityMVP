package greencity.repository;

import greencity.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSearchRepo extends JpaRepository<User, Long> {

    /**
     * Пошук кандидатів у друзі: виключає поточного користувача та тих, хто вже у friends
     * (у будь-якому напрямі). Пошук по name, case-insensitive, у будь-якому місці рядка.
     * За замовчуванням q може бути NULL/порожній.
     *
     * Таблиці: users, friendships (з нашої нової міграції).
     */
    @Query(
            value = """
                SELECT u.*
                FROM users u
                WHERE u.id <> :me
                  AND (
                        :q IS NULL OR :q = '' OR
                        LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%'))
                      )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM friendships f
                      WHERE (f.user_id = :me AND f.friend_id = u.id)
                         OR (f.user_id = u.id AND f.friend_id = :me)
                  )
                """,
            countQuery = """
                SELECT COUNT(*)
                FROM users u
                WHERE u.id <> :me
                  AND (
                        :q IS NULL OR :q = '' OR
                        LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%'))
                      )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM friendships f
                      WHERE (f.user_id = :me AND f.friend_id = u.id)
                         OR (f.user_id = u.id AND f.friend_id = :me)
                  )
                """,
            nativeQuery = true
    )
    Page<User> searchCandidates(@Param("me") Long me,
                                @Param("q") String q,
                                Pageable pageable);
}

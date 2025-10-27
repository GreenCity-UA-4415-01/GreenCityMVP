package greencity.repository;


import greencity.entity.User;
import greencity.entity.Friendship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSearchRepo extends JpaRepository<User, Long> {
    /**
     * Базовий пошук користувачів за name або username.
     * Повертає будь-яких користувачів без відсікання друзів/себе.
     */
    @Query("""
        select u
        from User u
        where (:q is null or :q = ''
               or lower(u.name) like lower(concat('%', :q, '%'))
               or lower(u.username) like lower(concat('%', :q, '%')))
    """)
    Page<User> search(@Param("q") String q, Pageable pageable);

    /**
     * Пошук "кандидатів у друзі":
     *  - виключає поточного користувача (:me)
     *  - виключає вже існуючих друзів (обидва напрямки у таблиці friendships)
     *  - фільтрує за name/username (нечутливо до регістру)
     */
    @Query("""
        select u
        from User u
        where u.id <> :me
          and (:q is null or :q = ''
               or lower(u.name) like lower(concat('%', :q, '%'))
               or lower(u.username) like lower(concat('%', :q, '%')))
          and u.id not in (
                select f.friend.id from Friendship f where f.user.id = :me
          )
          and u.id not in (
                select f.user.id from Friendship f where f.friend.id = :me
          )
    """)
    Page<User> searchCandidates(@Param("me") Long me,
                                @Param("q") String q,
                                Pageable pageable);
}


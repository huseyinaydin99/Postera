package tr.com.huseyinaydin.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tr.com.huseyinaydin.auth.domain.AppUser;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByActiveTrue();

    @Query("""
        SELECT u FROM AppUser u
        WHERE u.active = true
          AND u.id <> :excludeUserId
          AND (
            LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY u.firstName ASC, u.lastName ASC
    """)
    List<AppUser> searchActiveUsers(@Param("query") String query, @Param("excludeUserId") Long excludeUserId);

    @Query("SELECT u FROM AppUser u WHERE u.active = true AND u.id <> :excludeUserId ORDER BY u.firstName ASC, u.lastName ASC")
    List<AppUser> findAllActiveExcept(@Param("excludeUserId") Long excludeUserId);
}


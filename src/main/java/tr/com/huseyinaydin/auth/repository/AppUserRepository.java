package tr.com.huseyinaydin.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.huseyinaydin.auth.domain.AppUser;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}

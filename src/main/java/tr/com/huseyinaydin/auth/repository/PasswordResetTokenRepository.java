package tr.com.huseyinaydin.auth.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.huseyinaydin.auth.domain.PasswordResetToken;
import java.util.Optional;
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    void deleteByUserId(Long userId);
}

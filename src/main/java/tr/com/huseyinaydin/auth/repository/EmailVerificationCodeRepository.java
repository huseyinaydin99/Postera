package tr.com.huseyinaydin.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.huseyinaydin.auth.domain.EmailVerificationCode;

import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {
    Optional<EmailVerificationCode> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}

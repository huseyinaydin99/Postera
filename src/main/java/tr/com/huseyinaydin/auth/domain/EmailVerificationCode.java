package tr.com.huseyinaydin.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_verification_codes")
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    public static EmailVerificationCode create(AppUser user, String codeHash, Instant expiresAt) {
        var verification = new EmailVerificationCode();
        verification.user = user;
        verification.codeHash = codeHash;
        verification.expiresAt = expiresAt;
        return verification;
    }

    public boolean matches(Instant instant, String candidateHash) {
        return expiresAt.isAfter(instant) && codeHash.equals(candidateHash);
    }
}

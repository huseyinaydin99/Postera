package tr.com.huseyinaydin.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private AppUser user;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(nullable = false) private Instant expiresAt;
    private Instant usedAt;
    public static PasswordResetToken create(AppUser user, String tokenHash, Instant expiresAt) {
        var token = new PasswordResetToken(); token.user = user; token.tokenHash = tokenHash; token.expiresAt = expiresAt; return token;
    }
    public boolean isUsableAt(Instant instant) { return usedAt == null && expiresAt.isAfter(instant); }
    public void markUsed() { usedAt = Instant.now(); }
}

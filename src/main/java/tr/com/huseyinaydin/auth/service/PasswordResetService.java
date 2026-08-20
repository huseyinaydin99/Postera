package tr.com.huseyinaydin.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.domain.PasswordResetToken;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.auth.repository.PasswordResetTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AppUserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    @Value("${postera.app.base-url}") private String applicationBaseUrl;
    @Value("${spring.mail.username:}") private String senderAddress;

    @Transactional
    public void requestReset(String email) {
        var user = userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null) return;
        tokenRepository.deleteByUserId(user.getId());
        var rawToken = createRawToken();
        tokenRepository.save(PasswordResetToken.create(user, hash(rawToken), Instant.now().plus(Duration.ofMinutes(30))));
        var message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        if (!senderAddress.isBlank()) message.setFrom(senderAddress);
        message.setSubject("Postera parola sıfırlama bağlantısı");
        message.setText("Parolanızı yenilemek için 30 dakika içinde bu bağlantıyı açın:\n" + applicationBaseUrl + "/auth/reset-password?token=" + rawToken);
        mailSender.send(message);
    }

    @Transactional
    public void resetPassword(String rawToken, String password) {
        var token = tokenRepository.findByTokenHash(hash(rawToken)).orElseThrow(() -> new IllegalArgumentException("Parola sıfırlama bağlantısı geçersiz."));
        if (!token.isUsableAt(Instant.now())) throw new IllegalArgumentException("Parola sıfırlama bağlantısının süresi dolmuş veya daha önce kullanılmış.");
        token.getUser().changePassword(passwordEncoder.encode(password));
        token.markUsed();
    }

    private String createRawToken() { var bytes = new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String hash(String token) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("Token güvenlik algoritması bulunamadı.", exception); }
    }
}

package tr.com.huseyinaydin.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.domain.AppUser;
import tr.com.huseyinaydin.auth.domain.RoleName;
import tr.com.huseyinaydin.auth.repository.AppRoleRepository;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.auth.repository.EmailVerificationCodeRepository;
import tr.com.huseyinaydin.auth.web.RegistrationRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository userRepository;
    private final AppRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String senderAddress;

    @Transactional
    public String register(RegistrationRequest request) {
        var email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Bu e-posta adresiyle zaten bir hesap oluşturulmuş.");
        }

        var userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("Varsayılan kullanıcı rolü bulunamadı."));

        var user = AppUser.createPending(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordEncoder.encode(request.password())
        );
        user.assignRole(userRole);
        userRepository.save(user);

        var code = createCode();
        verificationCodeRepository.save(tr.com.huseyinaydin.auth.domain.EmailVerificationCode.create(
                user, hash(code), Instant.now().plus(Duration.ofMinutes(15))
        ));
        sendVerificationEmail(email, user.getFirstName(), code);
        return email;
    }

    @Transactional
    public void verifyEmail(String email, String code) {
        var normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        var user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Doğrulama oturumu geçersiz."));
        if (user.isActive()) return;

        var verification = verificationCodeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Doğrulama kodu bulunamadı. Yeni kayıt oluşturun."));
        if (!verification.matches(Instant.now(), hash(code))) {
            throw new IllegalArgumentException("Doğrulama kodu hatalı veya süresi dolmuş.");
        }
        user.activate();
        verificationCodeRepository.delete(verification);
    }

    private void sendVerificationEmail(String email, String name, String code) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(email);
            if (!senderAddress.isBlank()) helper.setFrom(senderAddress);
            helper.setSubject("Postera e-posta doğrulama kodunuz");

            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("code", code);
            String htmlContent = templateEngine.process("email/verification-code", context);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("Doğrulama e-postası gönderilirken bir hata oluştu.", e);
        }
    }

    private String createCode() {
        return String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
    }

    private String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Doğrulama kodu güvenlik algoritması bulunamadı.", exception);
        }
    }
}

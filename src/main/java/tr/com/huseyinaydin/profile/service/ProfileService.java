package tr.com.huseyinaydin.profile.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.profile.web.ProfileUpdateRequest;
import tr.com.huseyinaydin.profile.web.PasswordChangeRequest;
import tr.com.huseyinaydin.profile.web.EmailChangeRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public ProfileData getProfile(String email) {
        var user = findUser(email);
        return new ProfileData(user.getFirstName(), user.getLastName(), user.getEmail());
    }

    @Transactional
    public void updateProfile(String email, ProfileUpdateRequest request) {
        findUser(email).updateProfile(request.firstName().trim(), request.lastName().trim());
    }

    @Transactional
    public void changePassword(String email, PasswordChangeRequest request) {
        var user = findUser(email);
        verifyCurrentPassword(user, request.currentPassword());
        if (!request.newPassword().equals(request.newPasswordConfirmation())) {
            throw new IllegalArgumentException("Yeni şifre ve tekrarı birbiriyle eşleşmiyor.");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void changeEmail(String email, EmailChangeRequest request) {
        var user = findUser(email);
        verifyCurrentPassword(user, request.currentPassword());
        var newEmail = request.newEmail().trim().toLowerCase(Locale.ROOT);
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new IllegalArgumentException("Bu e-posta adresi başka bir hesap tarafından kullanılıyor.");
        }
        user.changeEmail(newEmail);
    }

    private tr.com.huseyinaydin.auth.domain.AppUser findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalStateException("Oturumu açık kullanıcı bulunamadı."));
    }

    private void verifyCurrentPassword(tr.com.huseyinaydin.auth.domain.AppUser user, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mevcut şifreniz doğru değil.");
        }
    }
}

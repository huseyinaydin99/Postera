package tr.com.huseyinaydin.profile.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.profile.web.ProfileUpdateRequest;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AppUserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfileData getProfile(String email) {
        var user = findUser(email);
        return new ProfileData(user.getFirstName(), user.getLastName(), user.getEmail());
    }

    @Transactional
    public void updateProfile(String email, ProfileUpdateRequest request) {
        findUser(email).updateProfile(request.firstName().trim(), request.lastName().trim());
    }

    private tr.com.huseyinaydin.auth.domain.AppUser findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalStateException("Oturumu açık kullanıcı bulunamadı."));
    }
}

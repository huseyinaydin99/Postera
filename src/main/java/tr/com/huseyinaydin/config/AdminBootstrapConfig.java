package tr.com.huseyinaydin.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.domain.RoleName;
import tr.com.huseyinaydin.auth.repository.AppRoleRepository;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;

import java.util.Locale;

@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class AdminBootstrapConfig {
    private final AppUserRepository userRepository;
    private final AppRoleRepository roleRepository;

    @Bean
    ApplicationRunner initialAdminRunner(@Value("${postera.admin.email:}") String adminEmail) {
        return arguments -> assignInitialAdmin(adminEmail);
    }

    @Transactional
    void assignInitialAdmin(String adminEmail) {
        if (adminEmail == null || adminEmail.isBlank()) {
            return;
        }
        var user = userRepository.findByEmailIgnoreCase(adminEmail.trim().toLowerCase(Locale.ROOT))
                .orElse(null);
        if (user == null || user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN)) {
            return;
        }
        var adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN rolü bulunamadı."));
        user.assignRole(adminRole);
    }
}

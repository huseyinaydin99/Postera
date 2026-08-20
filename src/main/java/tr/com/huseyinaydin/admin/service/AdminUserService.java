package tr.com.huseyinaydin.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.domain.RoleName;
import tr.com.huseyinaydin.auth.repository.AppRoleRepository;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final AppUserRepository userRepository;
    private final AppRoleRepository roleRepository;

    @Transactional(readOnly = true)
    public java.util.List<AdminUserData> listUsers() {
        return userRepository.findAll().stream().map(user -> new AdminUserData(
                user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.isActive(),
                user.getRoles().stream().map(role -> role.getName().name()).collect(java.util.stream.Collectors.toSet())
        )).toList();
    }

    @Transactional
    public void toggleActive(Long userId) {
        findUser(userId).toggleActive();
    }

    @Transactional
    public void updateRoles(Long userId, Set<RoleName> roleNames) {
        var selectedRoles = roleNames == null || roleNames.isEmpty() ? EnumSet.of(RoleName.USER) : EnumSet.copyOf(roleNames);
        var roles = selectedRoles.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalStateException("Sistem rolü bulunamadı.")))
                .collect(java.util.stream.Collectors.toSet());
        findUser(userId).replaceRoles(roles);
    }

    private tr.com.huseyinaydin.auth.domain.AppUser findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
    }
}

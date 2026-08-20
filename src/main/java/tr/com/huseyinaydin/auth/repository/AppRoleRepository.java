package tr.com.huseyinaydin.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.huseyinaydin.auth.domain.AppRole;
import tr.com.huseyinaydin.auth.domain.RoleName;

import java.util.Optional;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {
    Optional<AppRole> findByName(RoleName name);
}

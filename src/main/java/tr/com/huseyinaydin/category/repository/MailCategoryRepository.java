package tr.com.huseyinaydin.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.huseyinaydin.category.domain.MailCategory;

import java.util.List;
import java.util.Optional;

public interface MailCategoryRepository extends JpaRepository<MailCategory, Long> {
    List<MailCategory> findByOwnerIdOrderByNameAsc(Long ownerId);
    Optional<MailCategory> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);
}

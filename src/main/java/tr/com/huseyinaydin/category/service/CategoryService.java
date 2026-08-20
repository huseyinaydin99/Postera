package tr.com.huseyinaydin.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.category.domain.MailCategory;
import tr.com.huseyinaydin.category.repository.MailCategoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final MailCategoryRepository categoryRepository;
    private final AppUserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CategoryData> list(String currentUserEmail) {
        var user = findUser(currentUserEmail);
        return categoryRepository.findByOwnerIdOrderByNameAsc(user.getId()).stream()
                .map(category -> new CategoryData(category.getId(), category.getName()))
                .toList();
    }

    @Transactional
    public void create(String currentUserEmail, String name) {
        var user = findUser(currentUserEmail);
        var normalizedName = validateName(name);
        if (categoryRepository.existsByOwnerIdAndNameIgnoreCase(user.getId(), normalizedName)) {
            throw new IllegalArgumentException("Bu isimde bir kategoriniz zaten var.");
        }
        categoryRepository.save(MailCategory.create(user, normalizedName));
    }

    @Transactional
    public void rename(String currentUserEmail, Long categoryId, String name) {
        var user = findUser(currentUserEmail);
        var normalizedName = validateName(name);
        var category = findOwnedCategory(user.getId(), categoryId);
        if (!category.getName().equalsIgnoreCase(normalizedName)
                && categoryRepository.existsByOwnerIdAndNameIgnoreCase(user.getId(), normalizedName)) {
            throw new IllegalArgumentException("Bu isimde bir kategoriniz zaten var.");
        }
        category.rename(normalizedName);
    }

    @Transactional
    public void delete(String currentUserEmail, Long categoryId) {
        var user = findUser(currentUserEmail);
        categoryRepository.delete(findOwnedCategory(user.getId(), categoryId));
    }

    private tr.com.huseyinaydin.auth.domain.AppUser findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Oturumu açık kullanıcı bulunamadı."));
    }

    private MailCategory findOwnedCategory(Long ownerId, Long categoryId) {
        return categoryRepository.findByIdAndOwnerId(categoryId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Kategori bulunamadı."));
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Kategori adı zorunludur.");
        }
        var normalizedName = name.trim();
        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException("Kategori adı en fazla 100 karakter olabilir.");
        }
        return normalizedName;
    }
}

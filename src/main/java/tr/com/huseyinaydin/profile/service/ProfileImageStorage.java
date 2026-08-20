package tr.com.huseyinaydin.profile.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tr.com.huseyinaydin.config.UploadProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileImageStorage {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp"
    );
    private final UploadProperties uploadProperties;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > MAX_FILE_SIZE || !CONTENT_TYPE_EXTENSIONS.containsKey(file.getContentType())) {
            throw new IllegalArgumentException("Profil görseli JPG, PNG veya WEBP formatında ve en fazla 5 MB olmalıdır.");
        }
        var fileName = UUID.randomUUID() + CONTENT_TYPE_EXTENSIONS.get(file.getContentType());
        var directory = Path.of(uploadProperties.profileDirectory()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
            file.transferTo(directory.resolve(fileName));
        } catch (IOException exception) {
            throw new IllegalStateException("Profil görseli kaydedilemedi.", exception);
        }
        return "/uploads/profile/" + fileName;
    }
}

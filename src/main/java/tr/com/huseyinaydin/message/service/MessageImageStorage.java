package tr.com.huseyinaydin.message.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tr.com.huseyinaydin.config.UploadProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageImageStorage {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of("image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp", "image/gif", ".gif");
    private final UploadProperties uploadProperties;

    public List<String> storeAll(List<MultipartFile> files) {
        var selected = files == null ? List.<MultipartFile>of() : files.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (selected.size() > 2) throw new IllegalArgumentException("Bir mesaja en fazla 2 görsel ekleyebilirsiniz.");
        return selected.stream().map(this::store).toList();
    }
    private String store(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE || !EXTENSIONS.containsKey(file.getContentType())) throw new IllegalArgumentException("Her görsel JPG, PNG, WEBP veya GIF formatında ve en fazla 5 MB olmalıdır.");
        var name = UUID.randomUUID() + EXTENSIONS.get(file.getContentType());
        try {
            var directory = Path.of(uploadProperties.messageDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(directory); file.transferTo(directory.resolve(name));
        } catch (IOException exception) { throw new IllegalStateException("Görsel kaydedilemedi.", exception); }
        return "/uploads/messages/" + name;
    }
}

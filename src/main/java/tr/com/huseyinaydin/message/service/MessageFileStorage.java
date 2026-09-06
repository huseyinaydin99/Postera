package tr.com.huseyinaydin.message.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tr.com.huseyinaydin.config.UploadProperties;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageFileStorage {

    public static final long MAX_FILE_SIZE = 25L * 1024 * 1024; // 25 MB
    private final UploadProperties uploadProperties;

    public record StoredMessageFile(
            String fileName,
            String originalName,
            String alias,
            long fileSize,
            String contentType
    ) {}

    public StoredMessageFile store(MultipartFile file, String alias) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Yüklenecek dosya boş olamaz.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dosya boyutu en fazla 25 MB olabilir.");
        }

        var originalFilename = file.getOriginalFilename();
        var safeOriginalName = (originalFilename != null && !originalFilename.isBlank())
                ? Path.of(originalFilename).getFileName().toString()
                : "dosya";

        var extension = "";
        var dotIndex = safeOriginalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = safeOriginalName.substring(dotIndex);
        }

        var uniqueFileName = UUID.randomUUID().toString().replace("-", "") + extension;

        var userAlias = (alias != null && !alias.isBlank()) ? alias.trim() : safeOriginalName;
        // Eğer takma ad uzantı içermiyorsa orijinal uzantıyı koru
        if (!extension.isBlank() && !userAlias.toLowerCase().endsWith(extension.toLowerCase())) {
            userAlias = userAlias + extension;
        }

        try {
            var directory = getStorageDirectory();
            Files.createDirectories(directory);
            var targetPath = directory.resolve(uniqueFileName).normalize();
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Dosya sunucuya kaydedilemedi.", exception);
        }

        return new StoredMessageFile(
                uniqueFileName,
                safeOriginalName,
                userAlias,
                file.getSize(),
                file.getContentType() != null ? file.getContentType() : "application/octet-stream"
        );
    }

    public Resource loadAsResource(String fileName) {
        try {
            var filePath = getStorageDirectory().resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("Dosya bulunamadı veya okunamıyor: " + fileName);
            }
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Geçersiz dosya yolu: " + fileName, exception);
        }
    }

    private Path getStorageDirectory() {
        return Path.of(uploadProperties.messageDirectory(), "files").toAbsolutePath().normalize();
    }
}

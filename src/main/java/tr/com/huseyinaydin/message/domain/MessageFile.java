package tr.com.huseyinaydin.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "message_files")
public class MessageFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private MailMessage message;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 255)
    private String originalName;

    @Column(nullable = false, length = 255)
    private String alias;

    @Column(nullable = false)
    private long fileSize;

    @Column(length = 100)
    private String contentType;

    @Column(nullable = false)
    private Instant createdAt;

    public static MessageFile create(MailMessage message, String fileName, String originalName, String alias, long fileSize, String contentType) {
        var file = new MessageFile();
        file.message = message;
        file.fileName = fileName;
        file.originalName = originalName;
        file.alias = (alias != null && !alias.isBlank()) ? alias.trim() : originalName;
        file.fileSize = fileSize;
        file.contentType = contentType;
        file.createdAt = Instant.now();
        return file;
    }
}

package tr.com.huseyinaydin.message.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "message_images")
public class MessageImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "message_id", nullable = false) private MailMessage message;
    @Column(nullable = false, length = 500) private String imageUrl;
    @Column(nullable = false) private int displayOrder;

    static MessageImage create(MailMessage message, String imageUrl, int displayOrder) {
        var image = new MessageImage(); image.message = message; image.imageUrl = imageUrl; image.displayOrder = displayOrder; return image;
    }
}

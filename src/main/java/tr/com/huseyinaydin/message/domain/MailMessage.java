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
import tr.com.huseyinaydin.auth.domain.AppUser;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mail_messages")
public class MailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private Instant sentAt;

    @Column(nullable = false)
    private boolean read;

    @Column(nullable = false)
    private boolean important;

    @Column(nullable = false)
    private boolean trash;

    @Column(nullable = false)
    private boolean draft;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private AppUser sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private AppUser receiver;

    public static MailMessage send(AppUser sender, AppUser receiver, String subject, String body) {
        var message = new MailMessage();
        message.sender = sender;
        message.receiver = receiver;
        message.subject = subject;
        message.body = body;
        message.sentAt = Instant.now();
        return message;
    }

    public void markAsRead() {
        read = true;
    }
}

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
import tr.com.huseyinaydin.category.domain.MailCategory;

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
    private boolean senderTrash;

    @Column(nullable = false)
    private boolean receiverDeleted;

    @Column(nullable = false)
    private boolean senderDeleted;

    @Column(nullable = false)
    private boolean draft;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private AppUser sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private AppUser receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private MailCategory category;

    public static MailMessage send(AppUser sender, AppUser receiver, String subject, String body) {
        var message = new MailMessage();
        message.sender = sender;
        message.receiver = receiver;
        message.subject = subject;
        message.body = body;
        message.sentAt = Instant.now();
        return message;
    }

    public static MailMessage draft(AppUser sender, AppUser receiver, String subject, String body) {
        var message = send(sender, receiver, subject, body);
        message.draft = true;
        return message;
    }

    public void markAsRead() {
        read = true;
    }

    public void toggleImportant() {
        important = !important;
    }

    public void moveToTrash() {
        trash = true;
    }

    public void restoreFromTrash() {
        trash = false;
    }

    public void moveSenderCopyToTrash() {
        senderTrash = true;
    }

    public void restoreSenderCopyFromTrash() {
        senderTrash = false;
    }

    public void permanentlyDeleteReceiverCopy() {
        receiverDeleted = true;
    }

    public void permanentlyDeleteSenderCopy() {
        senderDeleted = true;
    }

    public boolean canBePurged() {
        return senderDeleted && (receiver == null || receiverDeleted);
    }

    public void updateDraft(AppUser receiver, String subject, String body) {
        this.receiver = receiver;
        this.subject = subject;
        this.body = body;
    }

    public void publish(AppUser receiver, String subject, String body) {
        updateDraft(receiver, subject, body);
        draft = false;
        sentAt = Instant.now();
    }

    public void assignCategory(MailCategory category) {
        this.category = category;
    }
}

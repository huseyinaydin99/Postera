package tr.com.huseyinaydin.report.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tr.com.huseyinaydin.auth.domain.AppUser;
import tr.com.huseyinaydin.message.domain.MailMessage;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "message_reports", uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "reported_by_user_id"}))
public class MessageReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private MailMessage message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by_user_id", nullable = false)
    private AppUser reportedByUser;

    private Instant reportedAt;

    public static MessageReport create(MailMessage message, AppUser reportedByUser) {
        var report = new MessageReport();
        report.message = message;
        report.reportedByUser = reportedByUser;
        report.reportedAt = Instant.now();
        return report;
    }
}

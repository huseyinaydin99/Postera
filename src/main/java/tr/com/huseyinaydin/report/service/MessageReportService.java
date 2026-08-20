package tr.com.huseyinaydin.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.message.repository.MailMessageRepository;
import tr.com.huseyinaydin.report.domain.MessageReport;
import tr.com.huseyinaydin.report.repository.MessageReportRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageReportService {
    private final MessageReportRepository reportRepository;
    private final MailMessageRepository messageRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public void report(String currentUserEmail, Long messageId) {
        var user = userRepository.findByEmailIgnoreCase(currentUserEmail)
                .orElseThrow(() -> new IllegalStateException("Oturumu açık kullanıcı bulunamadı."));
        var message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Mesaj bulunamadı."));
        var userIsSender = message.getSender().getId().equals(user.getId());
        var userIsReceiver = message.getReceiver() != null && message.getReceiver().getId().equals(user.getId());
        if (!userIsSender && !userIsReceiver) {
            throw new IllegalArgumentException("Bu mesajı şikayet etme izniniz yok.");
        }
        if (reportRepository.existsByMessageIdAndReportedByUserId(messageId, user.getId())) {
            throw new IllegalArgumentException("Bu mesajı daha önce şikayet ettiniz.");
        }
        reportRepository.save(MessageReport.create(message, user));
    }

    @Transactional(readOnly = true)
    public boolean isReportedBy(String currentUserEmail, Long messageId) {
        var user = userRepository.findByEmailIgnoreCase(currentUserEmail)
                .orElseThrow(() -> new IllegalStateException("Oturumu açık kullanıcı bulunamadı."));
        return reportRepository.existsByMessageIdAndReportedByUserId(messageId, user.getId());
    }

    @Transactional(readOnly = true)
    public List<ReportedMessageData> listReports() {
        return reportRepository.findAllByOrderByReportedAtDesc().stream().map(report -> new ReportedMessageData(
                report.getId(), report.getMessage().getId(), report.getMessage().getSubject(),
                fullName(report.getMessage().getSender()),
                report.getMessage().getReceiver() == null ? "Alıcı belirtilmedi" : fullName(report.getMessage().getReceiver()),
                fullName(report.getReportedByUser()), report.getReportedAt()
        )).toList();
    }

    private String fullName(tr.com.huseyinaydin.auth.domain.AppUser user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}

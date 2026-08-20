package tr.com.huseyinaydin.report.service;

import java.time.Instant;

public record ReportedMessageData(Long reportId, Long messageId, String subject, String senderName, String receiverName,
                                  String reportedByName, Instant reportedAt) {
}

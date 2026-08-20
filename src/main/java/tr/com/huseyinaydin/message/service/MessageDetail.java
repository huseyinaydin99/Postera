package tr.com.huseyinaydin.message.service;

import java.time.Instant;

public record MessageDetail(Long id, String subject, String body, String senderName, String senderEmail,
                            String receiverName, String receiverEmail, Instant sentAt, boolean receivedByCurrentUser,
                            boolean important, boolean trash, boolean draft, Long categoryId, String categoryName) {
}

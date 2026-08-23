package tr.com.huseyinaydin.message.service;

import java.time.Instant;

public record ConversationMessage(Long id, String senderName, String senderEmail, String senderProfileImageUrl,
                                  String body, Instant sentAt, boolean sentByCurrentUser) {
}

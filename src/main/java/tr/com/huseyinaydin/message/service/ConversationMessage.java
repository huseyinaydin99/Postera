package tr.com.huseyinaydin.message.service;

import java.time.Instant;
import java.util.List;

public record ConversationMessage(Long id, String senderName, String senderEmail, String senderProfileImageUrl,
                                  String body, List<String> imageUrls, Instant sentAt, boolean sentByCurrentUser) {
}

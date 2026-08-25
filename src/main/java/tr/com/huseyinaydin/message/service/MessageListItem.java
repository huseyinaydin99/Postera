package tr.com.huseyinaydin.message.service;

import java.time.Instant;

public record MessageListItem(Long id, String counterpartName, String counterpartEmail, String counterpartProfileImageUrl,
                              String subject, String preview, Instant sentAt, boolean read, boolean important) {
}

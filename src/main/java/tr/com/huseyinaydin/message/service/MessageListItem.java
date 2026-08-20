package tr.com.huseyinaydin.message.service;

import java.time.Instant;

public record MessageListItem(Long id, String counterpartName, String counterpartEmail, String subject, Instant sentAt,
                              boolean read, boolean important) {
}

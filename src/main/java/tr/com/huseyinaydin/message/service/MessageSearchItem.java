package tr.com.huseyinaydin.message.service;

import java.time.Instant;

public record MessageSearchItem(Long id, String counterpartName, String counterpartEmail, String subject,
                                String bodyPreview, Instant sentAt) {
}

package tr.com.huseyinaydin.notification.service;

import java.time.OffsetDateTime;

public record NotificationItem(
        Long id,
        String type,
        String title,
        String message,
        String actorName,
        String actorProfileImageUrl,
        String targetUrl,
        boolean isRead,
        OffsetDateTime createdAt
) {
}

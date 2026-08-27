package tr.com.huseyinaydin.notification.service;

import java.util.List;

public record NotificationListResponse(
        List<NotificationItem> notifications,
        long unreadCount,
        long totalCount,
        boolean hasMore,
        int nextOffset
) {
}

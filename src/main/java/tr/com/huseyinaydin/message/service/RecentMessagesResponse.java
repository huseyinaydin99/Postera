package tr.com.huseyinaydin.message.service;

import java.util.List;

public record RecentMessagesResponse(
        List<MessageListItem> messages,
        long total,
        long unreadCount,
        boolean hasMore,
        int nextOffset
) {
}

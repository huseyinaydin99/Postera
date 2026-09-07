package tr.com.huseyinaydin.message.service;

import java.time.OffsetDateTime;
import java.util.List;

public record ChatHistoryResponse(
        Long friendId,
        String friendName,
        String friendEmail,
        String friendAvatarUrl,
        boolean isOnline,
        String presenceStatusLabel,
        OffsetDateTime lastSeenAt,
        List<ConversationMessage> messages
) {}

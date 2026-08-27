package tr.com.huseyinaydin.friend.service;

import java.time.OffsetDateTime;

public record FriendRequestItem(
        Long friendshipId,
        Long senderId,
        String senderName,
        String senderEmail,
        String senderProfileImageUrl,
        OffsetDateTime sentAt
) {
}

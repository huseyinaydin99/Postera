package tr.com.huseyinaydin.friend.service;

import java.time.OffsetDateTime;

public record SidebarFriendItem(
        Long id,
        String fullName,
        String profileImageUrl,
        boolean isOnline,
        String presenceStatusLabel,
        OffsetDateTime lastSeenAt
) {}

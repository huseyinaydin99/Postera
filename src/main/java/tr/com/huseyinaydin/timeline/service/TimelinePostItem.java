package tr.com.huseyinaydin.timeline.service;

import java.time.Instant;
import java.util.List;

public record TimelinePostItem(
        Long id,
        Long userId,
        String authorName,
        String authorEmail,
        String authorProfileImageUrl,
        String content,
        List<String> imageUrls,
        Instant createdAt,
        Instant updatedAt,
        boolean ownedByCurrentUser,
        List<TimelineReactionSummary> reactions,
        String currentUserReaction
) {
}

package tr.com.huseyinaydin.timeline.service;

import java.time.Instant;
import java.util.List;

public record CommentItem(
        Long id,
        Long postId,
        Long authorId,
        String authorName,
        String authorProfileImageUrl,
        Long parentId,
        String content,
        Instant createdAt,
        boolean ownedByCurrentUser,
        List<CommentItem> replies
) {}

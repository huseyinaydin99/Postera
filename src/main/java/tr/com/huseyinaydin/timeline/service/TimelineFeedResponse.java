package tr.com.huseyinaydin.timeline.service;

import java.util.List;

public record TimelineFeedResponse(
        List<TimelinePostItem> posts,
        long totalCount,
        boolean hasMore,
        int nextOffset
) {
}

package tr.com.huseyinaydin.friend.service;

import java.util.List;

public record FriendRequestsResponse(
        List<FriendRequestItem> requests,
        long totalCount,
        boolean hasMore,
        int nextOffset
) {
}

package tr.com.huseyinaydin.friend.service;

public record DiscoverUserItem(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String profileImageUrl,
        String friendshipStatus
) {
}

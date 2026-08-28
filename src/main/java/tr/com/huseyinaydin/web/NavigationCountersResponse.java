package tr.com.huseyinaydin.web;

/**
 * Üst bardaki bekleyen kullanıcı etkileşimlerinin yalın görünümü.
 */
public record NavigationCountersResponse(
        long friendRequests,
        long unreadMessages,
        long unreadNotifications
) {
}

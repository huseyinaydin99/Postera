package tr.com.huseyinaydin.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import tr.com.huseyinaydin.friend.service.FriendService;
import tr.com.huseyinaydin.message.service.MessageService;
import tr.com.huseyinaydin.notification.service.NotificationService;
import tr.com.huseyinaydin.profile.service.ProfileService;

@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class NavigationModelAdvice {

    private final ProfileService profileService;
    private final MessageService messageService;
    private final FriendService friendService;
    private final NotificationService notificationService;

    @ModelAttribute("navigationProfile")
    Object navigationProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return profileService.getProfile(authentication.getName());
    }

    @ModelAttribute("mailboxCounts")
    Object mailboxCounts(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return messageService.mailboxCounts(authentication.getName());
    }

    @ModelAttribute("pendingFriendRequestsCount")
    long pendingFriendRequestsCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return 0L;
        }
        return friendService.countPendingRequests(authentication.getName());
    }

    @ModelAttribute("unreadNotificationsCount")
    long unreadNotificationsCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return 0L;
        }
        return notificationService.countUnread(authentication.getName());
    }
}

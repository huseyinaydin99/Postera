package tr.com.huseyinaydin.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tr.com.huseyinaydin.friend.service.FriendService;
import tr.com.huseyinaydin.message.service.MessageService;
import tr.com.huseyinaydin.notification.service.NotificationService;

@RestController
@RequestMapping("/api/navigation")
@RequiredArgsConstructor
public class NavigationCountersController {

    private final FriendService friendService;
    private final MessageService messageService;
    private final NotificationService notificationService;

    @GetMapping("/counters")
    public NavigationCountersResponse counters(Authentication authentication) {
        var email = authentication.getName();
        return new NavigationCountersResponse(
                friendService.countPendingRequests(email),
                messageService.mailboxCounts(email).inboxUnread(),
                notificationService.countUnread(email)
        );
    }
}

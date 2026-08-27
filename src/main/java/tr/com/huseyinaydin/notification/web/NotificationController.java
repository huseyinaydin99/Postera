package tr.com.huseyinaydin.notification.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import tr.com.huseyinaydin.notification.service.NotificationListResponse;
import tr.com.huseyinaydin.notification.service.NotificationService;

import java.util.Map;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/recent")
    @ResponseBody
    public NotificationListResponse getRecentNotifications(
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "6") int limit,
            Authentication authentication) {
        return notificationService.getNotifications(authentication.getName(), offset, limit);
    }

    @PostMapping("/api/{id}/read")
    @ResponseBody
    public ResponseEntity<?> markAsRead(@PathVariable("id") Long id, Authentication authentication) {
        try {
            notificationService.markAsRead(authentication.getName(), id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/api/mark-all-read")
    @ResponseBody
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        try {
            notificationService.markAllAsRead(authentication.getName());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}

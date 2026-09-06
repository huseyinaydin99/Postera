package tr.com.huseyinaydin.friend.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tr.com.huseyinaydin.auth.domain.PresenceStatus;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.friend.service.FriendService;
import tr.com.huseyinaydin.friend.service.SidebarFriendItem;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PresenceController {

    private final FriendService friendService;
    private final AppUserRepository userRepository;

    @GetMapping("/friends/sidebar")
    public List<SidebarFriendItem> getSidebarFriends(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return List.of();
        return friendService.getSidebarFriends(authentication.getName());
    }

    @PostMapping("/presence/status")
    @Transactional
    public ResponseEntity<?> updateStatus(@RequestParam("status") String statusStr, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(401).build();
        
        try {
            var status = PresenceStatus.valueOf(statusStr);
            var user = userRepository.findByEmailIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            user.updatePresenceStatus(status);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "status", status.label()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Geçersiz durum."));
        }
    }

    @GetMapping("/presence/status")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(401).build();
        var user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(Map.of("status", user.getPresenceStatus() != null ? user.getPresenceStatus().name() : "AVAILABLE"));
    }
}

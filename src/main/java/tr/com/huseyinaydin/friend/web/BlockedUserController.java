package tr.com.huseyinaydin.friend.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tr.com.huseyinaydin.friend.service.BlockedUserItem;
import tr.com.huseyinaydin.friend.service.FriendService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blocked-users")
@RequiredArgsConstructor
public class BlockedUserController {

    private final FriendService friendService;

    @GetMapping
    public List<BlockedUserItem> getBlockedUsers(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return List.of();
        return friendService.getBlockedUsers(authentication.getName());
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<?> unblockUser(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(401).build();
        try {
            friendService.unblockUser(authentication.getName(), id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }
    }
}

package tr.com.huseyinaydin.friend.web;

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
import tr.com.huseyinaydin.friend.service.FriendRequestsResponse;
import tr.com.huseyinaydin.friend.service.FriendService;

import java.util.Map;

@Controller
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendsController {

    private final FriendService friendService;

    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<?> getFriendsList(Authentication authentication) {
        return ResponseEntity.ok(friendService.getFriendsList(authentication.getName()));
    }

    @GetMapping("/api/requests")
    @ResponseBody
    public FriendRequestsResponse getRequests(
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "6") int limit,
            Authentication authentication) {
        return friendService.getPendingRequests(authentication.getName(), offset, limit);
    }

    @PostMapping("/api/requests/{friendshipId}/accept")
    @ResponseBody
    public ResponseEntity<?> acceptRequest(
            @PathVariable("friendshipId") Long friendshipId,
            Authentication authentication) {
        try {
            friendService.acceptFriendRequest(authentication.getName(), friendshipId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Arkadaşlık isteği kabul edildi."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}

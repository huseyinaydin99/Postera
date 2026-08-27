package tr.com.huseyinaydin.friend.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import tr.com.huseyinaydin.friend.service.DiscoverUserItem;
import tr.com.huseyinaydin.friend.service.FriendService;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/discover")
@RequiredArgsConstructor
public class DiscoverController {

    private final FriendService friendService;

    @GetMapping
    public String index(@RequestParam(name = "q", required = false) String query,
                        Authentication authentication,
                        Model model) {
        var users = friendService.listDiscoverUsers(authentication.getName(), query);
        model.addAttribute("users", users);
        model.addAttribute("query", query == null ? "" : query);
        return "discover/index";
    }

    @GetMapping("/api/search")
    @ResponseBody
    public List<DiscoverUserItem> apiSearch(@RequestParam(name = "q", required = false) String query,
                                            Authentication authentication) {
        return friendService.listDiscoverUsers(authentication.getName(), query);
    }

    @PostMapping("/api/request/{userId}")
    @ResponseBody
    public ResponseEntity<?> sendRequest(@PathVariable("userId") Long userId,
                                         Authentication authentication) {
        try {
            var status = friendService.sendFriendRequest(authentication.getName(), userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "status", status,
                    "message", "Arkadaşlık isteği başarıyla gönderildi."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}

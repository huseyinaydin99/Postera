package tr.com.huseyinaydin.profile.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.timeline.service.TimelineService;

import tr.com.huseyinaydin.friend.service.FriendService;

@Controller
@RequestMapping("/u")
@RequiredArgsConstructor
public class UserProfileController {

    private final AppUserRepository userRepository;
    private final TimelineService timelineService;
    private final FriendService friendService;

    @GetMapping("/{id}")
    public String viewProfile(@PathVariable Long id, Authentication authentication, Model model) {
        var targetUser = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));
        var currentUser = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Oturum bulunamadı"));
        
        if (friendService.isBlocked(currentUser.getId(), id)) {
            throw new IllegalArgumentException("Kullanıcı bulunamadı.");
        }
        
        var posts = timelineService.getUserProfilePosts(authentication.getName(), id);
        
        model.addAttribute("targetUser", targetUser);
        model.addAttribute("posts", posts);
        
        return "profile/public-profile";
    }

    @PostMapping("/{id}/block")
    public String blockUser(@PathVariable Long id, Authentication authentication) {
        friendService.blockUser(authentication.getName(), id);
        return "redirect:/discover";
    }
}

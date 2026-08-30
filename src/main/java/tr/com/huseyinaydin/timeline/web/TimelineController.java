package tr.com.huseyinaydin.timeline.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tr.com.huseyinaydin.timeline.service.TimelineService;

import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;
import tr.com.huseyinaydin.timeline.domain.TimelineReactionType;

@Controller
@RequestMapping("/timeline")
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping
    public String index(Authentication authentication, Model model) {
        model.addAttribute("posts", timelineService.listPosts(authentication.getName()));
        return "timeline/index";
    }

    @PostMapping("/{postId}/delete")
    public String deletePost(
            @PathVariable Long postId,
            @RequestParam(name = "redirectUrl", required = false) String redirectUrl,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            timelineService.deletePost(authentication.getName(), postId);
            redirectAttributes.addAttribute("deleted", "true");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("postError", exception.getMessage());
        }
        if (redirectUrl != null && !redirectUrl.isBlank()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/timeline";
    }

    @PostMapping
    public String sharePost(
            @RequestParam(name = "content", required = false) String content,
            @RequestParam(name = "images", required = false) List<MultipartFile> images,
            @RequestParam(name = "redirectUrl", required = false) String redirectUrl,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            timelineService.createPost(authentication.getName(), content, images);
            redirectAttributes.addAttribute("shared", "true");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("postError", exception.getMessage());
        }
        if (redirectUrl != null && !redirectUrl.isBlank()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/timeline";
    }

    @PostMapping("/api/posts")
    @ResponseBody
    public ResponseEntity<?> sharePostApi(
            @RequestParam(name = "content", required = false) String content,
            @RequestParam(name = "images", required = false) List<MultipartFile> images,
            Authentication authentication) {
        try {
            var postId = timelineService.createPost(authentication.getName(), content, images);
            return ResponseEntity.ok(Map.of("success", true, "postId", postId, "message", "Paylaşımınız yayınlandı."));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", exception.getMessage()));
        }
    }

    @GetMapping("/api/posts/{postId}/reactions/users")
    @ResponseBody
    public ResponseEntity<?> getReactionUsers(@PathVariable Long postId) {
        return ResponseEntity.ok(timelineService.getReactionUsers(postId));
    }

    @PostMapping("/api/posts/{postId}/reactions")
    @ResponseBody
    public ResponseEntity<?> reactToPost(@PathVariable Long postId,
                                         @RequestParam TimelineReactionType reaction,
                                         Authentication authentication) {
        try {
            return ResponseEntity.ok(timelineService.react(authentication.getName(), postId, reaction));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }
}

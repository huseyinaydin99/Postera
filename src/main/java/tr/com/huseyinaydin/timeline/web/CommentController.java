package tr.com.huseyinaydin.timeline.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tr.com.huseyinaydin.timeline.service.CommentService;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/{postId}/comments")
    public ResponseEntity<?> list(@PathVariable Long postId, Authentication auth) {
        return ResponseEntity.ok(commentService.getComments(postId, auth.getName()));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> add(
            @PathVariable Long postId,
            @RequestParam String content,
            @RequestParam(required = false) Long parentId,
            Authentication auth) {
        try {
            var item = commentService.addComment(postId, parentId, content, auth.getName());
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> delete(@PathVariable Long commentId, Authentication auth) {
        try {
            commentService.deleteComment(commentId, auth.getName());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

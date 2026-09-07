package tr.com.huseyinaydin.message.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tr.com.huseyinaydin.message.service.ChatHistoryResponse;
import tr.com.huseyinaydin.message.service.ConversationMessage;
import tr.com.huseyinaydin.message.service.MessageService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatApiController {

    private final MessageService messageService;

    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(@RequestParam("friendId") Long friendId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Oturum açmanız gerekiyor."));
        }
        try {
            ChatHistoryResponse history = messageService.getConversationWithFriend(authentication.getName(), friendId);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @RequestParam("friendId") Long friendId,
            @RequestParam(value = "body", required = false, defaultValue = "") String body,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "fileAlias", required = false) String fileAlias,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Oturum açmanız gerekiyor."));
        }

        try {
            List<MultipartFile> safeImages = images != null ? images : Collections.emptyList();
            ConversationMessage message = messageService.sendMessageToFriend(
                    authentication.getName(),
                    friendId,
                    body,
                    safeImages,
                    file,
                    fileAlias
            );
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Mesaj gönderilirken bir hata oluştu."));
        }
    }
}

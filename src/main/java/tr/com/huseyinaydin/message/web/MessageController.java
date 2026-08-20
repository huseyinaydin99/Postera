package tr.com.huseyinaydin.message.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tr.com.huseyinaydin.message.service.MessageService;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/inbox")
    String inbox(@RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        model.addAttribute("messages", messageService.inbox(authentication.getName(), page));
        model.addAttribute("boxTitle", "Gelen kutusu");
        model.addAttribute("boxType", "inbox");
        return "messages/list";
    }

    @GetMapping("/sent")
    String sent(@RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        model.addAttribute("messages", messageService.sent(authentication.getName(), page));
        model.addAttribute("boxTitle", "Gönderilenler");
        model.addAttribute("boxType", "sent");
        return "messages/list";
    }

    @GetMapping("/important")
    String important(@RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        model.addAttribute("messages", messageService.important(authentication.getName(), page));
        model.addAttribute("boxTitle", "Önemli mesajlar");
        model.addAttribute("boxType", "important");
        return "messages/list";
    }

    @GetMapping("/trash")
    String trash(@RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        model.addAttribute("messages", messageService.trash(authentication.getName(), page));
        model.addAttribute("boxTitle", "Çöp kutusu");
        model.addAttribute("boxType", "trash");
        return "messages/list";
    }

    @GetMapping("/compose")
    String compose(Model model) {
        model.addAttribute("sendMessageRequest", new SendMessageRequest(null, null, null));
        return "messages/compose";
    }

    @PostMapping
    String send(@Valid @ModelAttribute SendMessageRequest sendMessageRequest,
                BindingResult bindingResult,
                Authentication authentication) {
        if (bindingResult.hasErrors()) {
            return "messages/compose";
        }
        try {
            messageService.send(authentication.getName(), sendMessageRequest);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("message.send.failed", exception.getMessage());
            return "messages/compose";
        }
        return "redirect:/messages/sent?sent";
    }

    @GetMapping("/{messageId}")
    String detail(@PathVariable Long messageId, Authentication authentication, Model model) {
        try {
            model.addAttribute("message", messageService.getDetail(authentication.getName(), messageId));
            return "messages/detail";
        } catch (IllegalArgumentException exception) {
            return "redirect:/messages/inbox?notFound";
        }
    }

    @PostMapping("/{messageId}/important")
    String toggleImportant(@PathVariable Long messageId, Authentication authentication) {
        try {
            messageService.toggleImportant(authentication.getName(), messageId);
            return "redirect:/messages/" + messageId;
        } catch (IllegalArgumentException exception) {
            return "redirect:/messages/inbox?notFound";
        }
    }

    @PostMapping("/{messageId}/trash")
    String moveToTrash(@PathVariable Long messageId, Authentication authentication) {
        try {
            messageService.moveToTrash(authentication.getName(), messageId);
        } catch (IllegalArgumentException ignored) {
            // Geçersiz veya yetkisiz işlemlerde listeye dönülür.
        }
        return "redirect:/messages/inbox";
    }

    @PostMapping("/{messageId}/restore")
    String restoreFromTrash(@PathVariable Long messageId, Authentication authentication) {
        try {
            messageService.restoreFromTrash(authentication.getName(), messageId);
        } catch (IllegalArgumentException ignored) {
            // Geçersiz veya yetkisiz işlemlerde listeye dönülür.
        }
        return "redirect:/messages/trash";
    }
}

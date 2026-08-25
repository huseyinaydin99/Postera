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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ResponseBody;
import tr.com.huseyinaydin.message.service.MessageService;
import tr.com.huseyinaydin.category.service.CategoryService;
import tr.com.huseyinaydin.report.service.MessageReportService;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final CategoryService categoryService;
    private final MessageReportService reportService;

    @GetMapping("/inbox")
    String inbox(@ModelAttribute InboxFilter filter, BindingResult bindingResult,
                 Authentication authentication, Model model) {
        if (bindingResult.hasErrors()) {
            filter = InboxFilter.empty();
            model.addAttribute("filterError", "Filtre değerleri geçersiz olduğu için varsayılan liste gösterildi.");
        }
        model.addAttribute("messages", messageService.inbox(authentication.getName(), filter));
        model.addAttribute("filter", filter);
        model.addAttribute("categories", categoryService.list(authentication.getName()));
        model.addAttribute("boxTitle", "Gelen kutusu");
        model.addAttribute("boxType", "inbox");
        model.addAttribute("paginationPath", "/messages/inbox");
        return "messages/list";
    }

    @GetMapping("/sent")
    String sent(@RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        model.addAttribute("messages", messageService.sent(authentication.getName(), page));
        model.addAttribute("boxTitle", "Gönderilenler");
        model.addAttribute("boxType", "sent");
        model.addAttribute("paginationPath", "/messages/sent");
        return "messages/list";
    }

    @GetMapping("/important")
    String important(@RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        model.addAttribute("messages", messageService.important(authentication.getName(), page));
        model.addAttribute("boxTitle", "Önemli mesajlar");
        model.addAttribute("boxType", "important");
        model.addAttribute("paginationPath", "/messages/important");
        return "messages/list";
    }

    @GetMapping("/trash")
    String trash(@RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        model.addAttribute("messages", messageService.trash(authentication.getName(), page));
        model.addAttribute("boxTitle", "Çöp kutusu");
        model.addAttribute("boxType", "trash");
        model.addAttribute("paginationPath", "/messages/trash");
        return "messages/list";
    }

    @GetMapping("/drafts")
    String drafts(@RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        model.addAttribute("messages", messageService.drafts(authentication.getName(), page));
        return "messages/drafts";
    }

    @GetMapping("/search")
    String search(@RequestParam(name = "q", defaultValue = "") String query,
                  @RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        model.addAttribute("query", query.trim());
        model.addAttribute("results", messageService.search(authentication.getName(), query, page));
        return "messages/search";
    }

    @GetMapping(value = "/search/suggestions", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    java.util.List<tr.com.huseyinaydin.message.service.MessageSearchItem> searchSuggestions(
            @RequestParam(name = "q", defaultValue = "") String query, Authentication authentication) {
        if (query.trim().length() < 2) return java.util.List.of();
        return messageService.search(authentication.getName(), query, 0).getContent().stream().limit(8).toList();
    }

    @GetMapping("/compose")
    String compose(Model model) {
        model.addAttribute("sendMessageRequest", new SendMessageRequest(null, null, null));
        return "messages/compose";
    }

    @PostMapping("/drafts")
    String createDraft(@Valid @ModelAttribute DraftMessageRequest draftMessageRequest, BindingResult bindingResult,
                       Authentication authentication, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("sendMessageRequest", new SendMessageRequest(
                    draftMessageRequest.receiverEmail(), draftMessageRequest.subject(), draftMessageRequest.body()));
            return "messages/compose";
        }
        try {
            messageService.createDraft(authentication.getName(), draftMessageRequest);
            return "redirect:/messages/drafts?saved";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("draft.save.failed", exception.getMessage());
            model.addAttribute("sendMessageRequest", new SendMessageRequest(
                    draftMessageRequest.receiverEmail(), draftMessageRequest.subject(), draftMessageRequest.body()));
            return "messages/compose";
        }
    }

    @GetMapping("/drafts/{messageId}/edit")
    String editDraft(@PathVariable Long messageId, Authentication authentication, Model model) {
        try {
            model.addAttribute("draftMessageRequest", messageService.getDraft(authentication.getName(), messageId));
            model.addAttribute("messageId", messageId);
            return "messages/draft-form";
        } catch (IllegalArgumentException exception) {
            return "redirect:/messages/drafts";
        }
    }

    @PostMapping("/drafts/{messageId}")
    String updateDraft(@PathVariable Long messageId, @Valid @ModelAttribute DraftMessageRequest draftMessageRequest,
                       BindingResult bindingResult, Authentication authentication, Model model,
                       @RequestParam String action) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("messageId", messageId);
            return "messages/draft-form";
        }
        try {
            if ("send".equals(action)) {
                messageService.publishDraft(authentication.getName(), messageId, draftMessageRequest);
                return "redirect:/messages/sent?sent";
            }
            messageService.updateDraft(authentication.getName(), messageId, draftMessageRequest);
            return "redirect:/messages/drafts?saved";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("draft.update.failed", exception.getMessage());
            model.addAttribute("messageId", messageId);
            return "messages/draft-form";
        }
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
            model.addAttribute("conversation", messageService.conversation(authentication.getName(), messageId));
            model.addAttribute("replyMessageRequest", new ReplyMessageRequest(null, null));
            model.addAttribute("categories", categoryService.list(authentication.getName()));
            model.addAttribute("alreadyReported", reportService.isReportedBy(authentication.getName(), messageId));
            return "messages/detail";
        } catch (IllegalArgumentException exception) {
            return "redirect:/messages/inbox?notFound";
        }
    }

    @PostMapping("/{messageId}/category")
    String assignCategory(@PathVariable Long messageId, @RequestParam(required = false) Long categoryId,
                          Authentication authentication) {
        try {
            messageService.assignCategory(authentication.getName(), messageId, categoryId);
            return "redirect:/messages/" + messageId;
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
    String moveToTrash(@PathVariable Long messageId,
                       @RequestParam(defaultValue = "inbox") String returnTo,
                       Authentication authentication) {
        try {
            messageService.moveToTrash(authentication.getName(), messageId);
        } catch (IllegalArgumentException ignored) {
            // Geçersiz veya yetkisiz işlemlerde listeye dönülür.
        }
        var safeReturnTo = java.util.Set.of("inbox", "drafts", "sent", "important").contains(returnTo) ? returnTo : "inbox";
        return "redirect:/messages/" + safeReturnTo;
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

    @PostMapping("/{messageId}/reply")
    String reply(@PathVariable Long messageId, @Valid @ModelAttribute ReplyMessageRequest replyMessageRequest,
                 BindingResult bindingResult, Authentication authentication, Model model) {
        if (bindingResult.hasErrors()) {
            try {
                model.addAttribute("message", messageService.getDetail(authentication.getName(), messageId));
                model.addAttribute("conversation", messageService.conversation(authentication.getName(), messageId));
                model.addAttribute("categories", categoryService.list(authentication.getName()));
                model.addAttribute("alreadyReported", reportService.isReportedBy(authentication.getName(), messageId));
                return "messages/detail";
            } catch (IllegalArgumentException exception) {
                return "redirect:/messages/inbox?notFound";
            }
        }
        try {
            messageService.reply(authentication.getName(), messageId, replyMessageRequest.body(), replyMessageRequest.images());
            return "redirect:/messages/" + messageId + "?replied";
        } catch (IllegalArgumentException exception) {
            return "redirect:/messages/" + messageId + "?replyError";
        }
    }

    @PostMapping("/trash/selected/delete")
    String permanentlyDeleteSelectedTrash(@RequestParam(required = false) java.util.List<Long> messageIds,
                                          Authentication authentication, RedirectAttributes redirectAttributes) {
        var deletedCount = messageService.permanentlyDeleteSelectedTrash(authentication.getName(), messageIds);
        if (deletedCount == 0) {
            redirectAttributes.addFlashAttribute("trashActionError", "Silmek için çöp kutusundan en az bir mesaj seçin.");
        } else {
            redirectAttributes.addFlashAttribute("trashActionSuccess", deletedCount + " mesaj kalıcı olarak silindi.");
        }
        return "redirect:/messages/trash";
    }

    @PostMapping("/trash/empty")
    String permanentlyDeleteAllTrash(Authentication authentication, RedirectAttributes redirectAttributes) {
        var deletedCount = messageService.permanentlyDeleteAllTrash(authentication.getName());
        if (deletedCount == 0) {
            redirectAttributes.addFlashAttribute("trashActionError", "Çöp kutusu zaten boş.");
        } else {
            redirectAttributes.addFlashAttribute("trashActionSuccess", "Çöp kutusundaki " + deletedCount + " mesaj kalıcı olarak silindi.");
        }
        return "redirect:/messages/trash";
    }

    @PostMapping("/selected/trash")
    String moveSelectedToTrash(@RequestParam String boxType, @RequestParam(required = false) java.util.List<Long> messageIds,
                               Authentication authentication, RedirectAttributes redirectAttributes) {
        var movedCount = messageService.moveSelectedToTrash(authentication.getName(), messageIds, boxType);
        if (movedCount == 0) {
            redirectAttributes.addFlashAttribute("bulkActionError", "Çöp kutusuna taşımak için en az bir geçerli mesaj seçin.");
        } else {
            redirectAttributes.addFlashAttribute("bulkActionSuccess", movedCount + " mesaj çöp kutusuna taşındı.");
        }
        return "redirect:/messages/" + boxType;
    }
}

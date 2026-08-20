package tr.com.huseyinaydin.report.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import tr.com.huseyinaydin.report.service.MessageReportService;

@Controller
@RequiredArgsConstructor
public class ReportController {
    private final MessageReportService reportService;

    @PostMapping("/messages/{messageId}/report")
    String report(@PathVariable Long messageId, Authentication authentication) {
        try {
            reportService.report(authentication.getName(), messageId);
            return "redirect:/messages/" + messageId + "?reported";
        } catch (IllegalArgumentException exception) {
            return "redirect:/messages/" + messageId + "?reportError";
        }
    }
}

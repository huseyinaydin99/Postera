package tr.com.huseyinaydin.message.service;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class RichTextSanitizer {

    private static final String ALLOWED_TAGS = "b|strong|i|em|p|br|ul|ol|li";

    public String sanitize(String value) {
        if (value == null) return "";
        var escaped = HtmlUtils.htmlEscape(value.trim());
        return escaped.replaceAll("(?i)&lt;(/?(?:" + ALLOWED_TAGS + "))&gt;", "<$1>");
    }

    public boolean hasText(String value) {
        return sanitize(value).replaceAll("<[^>]+>", "").replace("&nbsp;", " ").trim().length() > 0;
    }
}

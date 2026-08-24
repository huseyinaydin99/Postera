package tr.com.huseyinaydin.message.service;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RichTextSanitizer {

    private static final String ALLOWED_TAGS = "b|strong|i|em|p|br|ul|ol|li";
    private static final Pattern LINK_TAG = Pattern.compile("(?i)&lt;a\\s+href=&quot;(https?://[^&quot;\\s]+)&quot;\\s*&gt;");
    private static final Pattern GIF_TAG = Pattern.compile("(?i)&lt;img\\s+(?:class=&quot;rich-gif&quot;\\s+)?src=&quot;(https://media\\.giphy\\.com/media/[A-Za-z0-9]+/giphy\\.gif)&quot;(?:\\s+alt=&quot;GIF&quot;)?\\s*&gt;");

    public String sanitize(String value) {
        if (value == null) return "";
        var escaped = HtmlUtils.htmlEscape(value.trim());
        var formatted = escaped.replaceAll("(?i)&lt;(/?(?:" + ALLOWED_TAGS + "))&gt;", "<$1>");
        formatted = restoreLinks(formatted);
        return restoreGifs(formatted);
    }

    public boolean hasText(String value) {
        var sanitized = sanitize(value);
        return sanitized.contains("<img class=\"rich-gif\"")
                || sanitized.replaceAll("<[^>]+>", "").replace("&nbsp;", " ").trim().length() > 0;
    }

    private String restoreLinks(String value) {
        var matcher = LINK_TAG.matcher(value);
        var output = new StringBuffer();
        while (matcher.find()) {
            var href = matcher.group(1);
            matcher.appendReplacement(output, Matcher.quoteReplacement("<a href=\"" + href + "\" target=\"_blank\" rel=\"noopener noreferrer\">"));
        }
        matcher.appendTail(output);
        return output.toString().replaceAll("(?i)&lt;/a&gt;", "</a>");
    }

    private String restoreGifs(String value) {
        var matcher = GIF_TAG.matcher(value);
        var output = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(output, Matcher.quoteReplacement("<img class=\"rich-gif\" src=\"" + matcher.group(1) + "\" alt=\"GIF\">"));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}

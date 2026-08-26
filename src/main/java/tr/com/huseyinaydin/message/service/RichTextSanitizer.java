package tr.com.huseyinaydin.message.service;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RichTextSanitizer {

    private static final String ALLOWED_TAGS = "b|strong|i|em|p|br|ul|ol|li";
    private static final Pattern LINK_TAG = Pattern.compile("(?i)&lt;a\\s+[^&gt;]*href=&quot;(https?://[^&quot;\\s]+)&quot;[^&gt;]*&gt;");
    private static final Pattern GIF_TAG = Pattern.compile("(?i)&lt;img\\s+[^&gt;]*src=&quot;(https://media[0-9]*\\.giphy\\.com/media/[A-Za-z0-9]+/giphy\\.gif)&quot;[^&gt;]*&gt;");

    public String sanitize(String value) {
        if (value == null) return "";
        var trimmed = value.trim();
        if (trimmed.isEmpty()) return "";

        var rawText = HtmlUtils.htmlUnescape(trimmed);
        var escaped = escapeHtmlBasic(rawText);
        var formatted = escaped.replaceAll("(?i)&lt;(/?(?:" + ALLOWED_TAGS + "))&gt;", "<$1>");
        formatted = restoreLinks(formatted);
        return restoreGifs(formatted);
    }

    public boolean hasText(String value) {
        if (value == null) return false;
        var sanitized = sanitize(value);
        return sanitized.contains("<img class=\"rich-gif\"")
                || sanitized.replaceAll("<[^>]+>", "").replace("&nbsp;", " ").trim().length() > 0;
    }

    private String escapeHtmlBasic(String input) {
        if (input == null) return "";
        var sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                case '&' -> sb.append("&amp;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private String restoreLinks(String value) {
        var matcher = LINK_TAG.matcher(value);
        var output = new StringBuffer();
        while (matcher.find()) {
            var href = matcher.group(1).replace("&amp;", "&");
            matcher.appendReplacement(output, Matcher.quoteReplacement("<a href=\"" + href + "\" target=\"_blank\" rel=\"noopener noreferrer\">"));
        }
        matcher.appendTail(output);
        return output.toString().replaceAll("(?i)&lt;/a&gt;", "</a>");
    }

    private String restoreGifs(String value) {
        var matcher = GIF_TAG.matcher(value);
        var output = new StringBuffer();
        while (matcher.find()) {
            var src = matcher.group(1).replace("&amp;", "&");
            matcher.appendReplacement(output, Matcher.quoteReplacement("<img class=\"rich-gif\" src=\"" + src + "\" alt=\"GIF\">"));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}

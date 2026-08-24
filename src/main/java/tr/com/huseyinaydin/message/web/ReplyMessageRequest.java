package tr.com.huseyinaydin.message.web;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public record ReplyMessageRequest(
        @Size(max = 10000, message = "Yanıt en fazla 10.000 karakter olabilir.")
        String body,
        List<MultipartFile> images
) {
}

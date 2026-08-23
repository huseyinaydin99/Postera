package tr.com.huseyinaydin.message.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplyMessageRequest(
        @NotBlank(message = "Yanıt metni zorunludur.")
        @Size(max = 10000, message = "Yanıt en fazla 10.000 karakter olabilir.")
        String body
) {
}

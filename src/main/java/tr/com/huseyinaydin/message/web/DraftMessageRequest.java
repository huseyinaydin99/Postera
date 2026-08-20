package tr.com.huseyinaydin.message.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record DraftMessageRequest(
        @Email(message = "Geçerli bir alıcı e-posta adresi girin.")
        String receiverEmail,

        @Size(max = 200, message = "Konu en fazla 200 karakter olabilir.")
        String subject,

        @Size(max = 10000, message = "Mesaj içeriği en fazla 10.000 karakter olabilir.")
        String body
) {
}

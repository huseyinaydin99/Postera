package tr.com.huseyinaydin.message.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record SendMessageRequest(
        @NotBlank(message = "Alıcı e-posta adresi zorunludur.")
        @Email(message = "Geçerli bir alıcı e-posta adresi girin.")
        String receiverEmail,

        @NotBlank(message = "Konu alanı zorunludur.")
        @Size(max = 200, message = "Konu en fazla 200 karakter olabilir.")
        String subject,

        @NotBlank(message = "Mesaj içeriği zorunludur.")
        @Size(max = 10000, message = "Mesaj içeriği en fazla 10.000 karakter olabilir.")
        String body,

        MultipartFile file,

        @Size(max = 100, message = "Dosya takma adı en fazla 100 karakter olabilir.")
        String fileAlias
) {
    public SendMessageRequest(String receiverEmail, String subject, String body) {
        this(receiverEmail, subject, body, null, null);
    }
}

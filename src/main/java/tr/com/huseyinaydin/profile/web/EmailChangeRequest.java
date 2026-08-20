package tr.com.huseyinaydin.profile.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailChangeRequest(
        @NotBlank(message = "Mevcut şifrenizi girin.") String currentPassword,
        @NotBlank(message = "Yeni e-posta adresi zorunludur.")
        @Email(message = "Geçerli bir e-posta adresi girin.")
        @Size(max = 255, message = "E-posta adresi en fazla 255 karakter olabilir.") String newEmail
) {
}

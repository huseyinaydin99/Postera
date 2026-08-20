package tr.com.huseyinaydin.profile.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank(message = "Mevcut şifrenizi girin.") String currentPassword,
        @NotBlank(message = "Yeni şifre alanı zorunludur.")
        @Size(min = 8, max = 72, message = "Yeni şifre 8 ile 72 karakter arasında olmalıdır.") String newPassword,
        @NotBlank(message = "Yeni şifre tekrarı zorunludur.") String newPasswordConfirmation
) {
}

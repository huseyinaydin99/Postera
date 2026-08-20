package tr.com.huseyinaydin.auth.web;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ResetPasswordRequest(@NotBlank(message = "Yeni şifre alanı zorunludur.") @Size(min = 8, max = 72, message = "Şifre 8 ile 72 karakter arasında olmalıdır.") String password, @NotBlank(message = "Şifre tekrarı zorunludur.") String passwordConfirmation) {}

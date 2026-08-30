package tr.com.huseyinaydin.auth.web;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record ResetPasswordRequest(
        @NotBlank(message = "Yeni şifre alanı zorunludur.")
        @Size(min = 8, max = 72, message = "Şifre 8 ile 72 karakter arasında olmalıdır.")
        @Pattern(
                regexp = "^(?=(?:.*[A-Z]){3,})(?=(?:.*[a-z]){3,})(?=(?:.*\\d){3,})(?=(?:.*[^A-Za-z0-9\\s]){3,}).{8,72}$",
                message = "Şifre en az 8 karakter; en az 3 büyük harf, 3 küçük harf, 3 rakam ve 3 özel karakter içermelidir."
        )
        String password,
        @NotBlank(message = "Şifre tekrarı zorunludur.")
        String passwordConfirmation
) {}

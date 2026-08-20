package tr.com.huseyinaydin.auth.web;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
public record ForgotPasswordRequest(@NotBlank(message = "E-posta adresi zorunludur.") @Email(message = "Geçerli bir e-posta adresi girin.") String email) {}

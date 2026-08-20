package tr.com.huseyinaydin.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank(message = "Ad alanı zorunludur.")
        @Size(max = 100, message = "Ad en fazla 100 karakter olabilir.")
        String firstName,

        @NotBlank(message = "Soyad alanı zorunludur.")
        @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir.")
        String lastName,

        @NotBlank(message = "E-posta adresi zorunludur.")
        @Email(message = "Geçerli bir e-posta adresi girin.")
        @Size(max = 255, message = "E-posta adresi en fazla 255 karakter olabilir.")
        String email,

        @NotBlank(message = "Şifre alanı zorunludur.")
        @Size(min = 8, max = 72, message = "Şifre 8 ile 72 karakter arasında olmalıdır.")
        String password,

        @NotBlank(message = "Şifre tekrarı zorunludur.")
        String passwordConfirmation
) {
}

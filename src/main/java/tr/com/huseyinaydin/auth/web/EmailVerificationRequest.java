package tr.com.huseyinaydin.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerificationRequest(
        @NotBlank(message = "Doğrulama kodu zorunludur.")
        @Pattern(regexp = "\\d{6}", message = "Doğrulama kodu 6 haneli olmalıdır.")
        String code
) { }

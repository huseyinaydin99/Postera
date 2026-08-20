package tr.com.huseyinaydin.profile.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = "Ad alanı zorunludur.")
        @Size(max = 100, message = "Ad en fazla 100 karakter olabilir.")
        String firstName,

        @NotBlank(message = "Soyad alanı zorunludur.")
        @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir.")
        String lastName
) {
}

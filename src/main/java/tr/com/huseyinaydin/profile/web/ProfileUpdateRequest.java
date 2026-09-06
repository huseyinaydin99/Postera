package tr.com.huseyinaydin.profile.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record ProfileUpdateRequest(
        @NotBlank(message = "Ad alanı zorunludur.")
        @Size(max = 100, message = "Ad en fazla 100 karakter olabilir.")
        String firstName,

        @NotBlank(message = "Soyad alanı zorunludur.")
        @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir.")
        String lastName,
        MultipartFile profileImage,
        MultipartFile coverImage
) {
}

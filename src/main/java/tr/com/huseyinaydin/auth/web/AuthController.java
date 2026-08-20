package tr.com.huseyinaydin.auth.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import tr.com.huseyinaydin.auth.service.RegistrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;

    @GetMapping("/login")
    String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    String registerForm(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest(null, null, null, null, null));
        return "auth/register";
    }

    @PostMapping("/register")
    String register(@Valid @ModelAttribute RegistrationRequest registrationRequest,
                    BindingResult bindingResult) {
        if (!Objects.equals(registrationRequest.password(), registrationRequest.passwordConfirmation())) {
            bindingResult.rejectValue("passwordConfirmation", "password.mismatch", "Şifreler birbiriyle eşleşmiyor.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            registrationService.register(registrationRequest);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("registration.failed", exception.getMessage());
            return "auth/register";
        }

        return "redirect:/auth/login?registered";
    }
}

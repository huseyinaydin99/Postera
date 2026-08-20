package tr.com.huseyinaydin.auth.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tr.com.huseyinaydin.auth.service.RegistrationService;
import tr.com.huseyinaydin.auth.service.PasswordResetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;

    @GetMapping("/login")
    String login() {
        return "auth/login";
    }

    @GetMapping("/forgot-password")
    String forgotPasswordForm(Model model) { model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest(null)); return "auth/forgot-password"; }

    @PostMapping("/forgot-password")
    String forgotPassword(@Valid @ModelAttribute ForgotPasswordRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) return "auth/forgot-password";
        passwordResetService.requestReset(request.email());
        return "redirect:/auth/login?resetRequested";
    }

    @GetMapping("/reset-password")
    String resetPasswordForm(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.isBlank()) return "redirect:/auth/login?resetInvalid";
        model.addAttribute("token", token); model.addAttribute("resetPasswordRequest", new ResetPasswordRequest(null, null));
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    String resetPassword(@RequestParam String token, @Valid @ModelAttribute ResetPasswordRequest request, BindingResult bindingResult, Model model) {
        if (!Objects.equals(request.password(), request.passwordConfirmation())) bindingResult.rejectValue("passwordConfirmation", "password.mismatch", "Şifreler eşleşmiyor.");
        if (bindingResult.hasErrors()) { model.addAttribute("token", token); return "auth/reset-password"; }
        try { passwordResetService.resetPassword(token, request.password()); return "redirect:/auth/login?passwordReset"; }
        catch (IllegalArgumentException exception) { bindingResult.reject("password.reset.failed", exception.getMessage()); model.addAttribute("token", token); return "auth/reset-password"; }
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

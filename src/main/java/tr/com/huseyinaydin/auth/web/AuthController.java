package tr.com.huseyinaydin.auth.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
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
                    BindingResult bindingResult,
                    HttpSession session) {
        if (!Objects.equals(registrationRequest.password(), registrationRequest.passwordConfirmation())) {
            bindingResult.rejectValue("passwordConfirmation", "password.mismatch", "Şifreler birbiriyle eşleşmiyor.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        final String email;
        try {
            email = registrationService.register(registrationRequest);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("registration.failed", exception.getMessage());
            return "auth/register";
        }

        session.setAttribute("pendingVerificationEmail", email);
        return "redirect:/auth/verify-email";
    }

    @GetMapping("/verify-email")
    String verifyEmailForm(@RequestParam(required = false) String email, HttpSession session, Model model) {
        if (email == null || email.isBlank()) email = (String) session.getAttribute("pendingVerificationEmail");
        if (email == null || email.isBlank()) return "redirect:/auth/register";
        model.addAttribute("email", email.trim());
        model.addAttribute("emailVerificationRequest", new EmailVerificationRequest(null));
        return "auth/verify-email";
    }

    @PostMapping("/verify-email")
    String verifyEmail(@RequestParam String email,
                       @Valid @ModelAttribute EmailVerificationRequest emailVerificationRequest,
                       BindingResult bindingResult,
                       Model model,
                       HttpSession session) {
        model.addAttribute("email", email.trim());
        if (bindingResult.hasErrors()) return "auth/verify-email";
        try {
            registrationService.verifyEmail(email, emailVerificationRequest.code());
            session.removeAttribute("pendingVerificationEmail");
            return "redirect:/auth/login?verified";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("verification.failed", exception.getMessage());
            return "auth/verify-email";
        }
    }
}

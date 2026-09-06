package tr.com.huseyinaydin.profile.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import tr.com.huseyinaydin.profile.service.ProfileService;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    String profile(Authentication authentication, Model model) {
        var profile = profileService.getProfile(authentication.getName());
        model.addAttribute("profileUpdateRequest", new ProfileUpdateRequest(profile.firstName(), profile.lastName(), null, null));
        model.addAttribute("email", profile.email());
        model.addAttribute("profileImageUrl", profile.profileImageUrl());
        model.addAttribute("coverImageUrl", profile.coverImageUrl());
        return "profile/index";
    }

    @PostMapping
    String updateProfile(@Valid ProfileUpdateRequest profileUpdateRequest,
                         BindingResult bindingResult,
                         Authentication authentication,
                         Model model) {
        if (bindingResult.hasErrors()) {
            var profile = profileService.getProfile(authentication.getName());
            model.addAttribute("email", profile.email());
            model.addAttribute("profileImageUrl", profile.profileImageUrl());
            model.addAttribute("coverImageUrl", profile.coverImageUrl());
            return "profile/index";
        }
        try {
            profileService.updateProfile(authentication.getName(), profileUpdateRequest);
            return "redirect:/profile?updated";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("profile.update.failed", exception.getMessage());
            var profile = profileService.getProfile(authentication.getName());
            model.addAttribute("email", profile.email());
            model.addAttribute("profileImageUrl", profile.profileImageUrl());
            model.addAttribute("coverImageUrl", profile.coverImageUrl());
            return "profile/index";
        }
    }

    @GetMapping("/password")
    String passwordForm(Model model) {
        model.addAttribute("passwordChangeRequest", new PasswordChangeRequest(null, null, null));
        return "profile/password";
    }

    @PostMapping("/password")
    String changePassword(@Valid PasswordChangeRequest passwordChangeRequest, BindingResult bindingResult,
                          Authentication authentication) {
        if (!Objects.equals(passwordChangeRequest.newPassword(), passwordChangeRequest.newPasswordConfirmation())) {
            bindingResult.rejectValue("newPasswordConfirmation", "password.mismatch", "Yeni şifreler eşleşmiyor.");
        }
        if (bindingResult.hasErrors()) return "profile/password";
        try {
            profileService.changePassword(authentication.getName(), passwordChangeRequest);
            return "redirect:/profile?passwordChanged";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("password.change.failed", exception.getMessage());
            return "profile/password";
        }
    }

    @GetMapping("/email")
    String emailForm(Model model) {
        model.addAttribute("emailChangeRequest", new EmailChangeRequest(null, null));
        return "profile/email";
    }

    @PostMapping("/email")
    String changeEmail(@Valid EmailChangeRequest emailChangeRequest, BindingResult bindingResult,
                       Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        if (bindingResult.hasErrors()) return "profile/email";
        try {
            profileService.changeEmail(authentication.getName(), emailChangeRequest);
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            return "redirect:/auth/login?emailChanged";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("email.change.failed", exception.getMessage());
            return "profile/email";
        }
    }
}

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
import tr.com.huseyinaydin.profile.service.ProfileService;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    String profile(Authentication authentication, Model model) {
        var profile = profileService.getProfile(authentication.getName());
        model.addAttribute("profileUpdateRequest", new ProfileUpdateRequest(profile.firstName(), profile.lastName()));
        model.addAttribute("email", profile.email());
        return "profile/index";
    }

    @PostMapping
    String updateProfile(@Valid ProfileUpdateRequest profileUpdateRequest,
                         BindingResult bindingResult,
                         Authentication authentication,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("email", profileService.getProfile(authentication.getName()).email());
            return "profile/index";
        }

        profileService.updateProfile(authentication.getName(), profileUpdateRequest);
        return "redirect:/profile?updated";
    }
}

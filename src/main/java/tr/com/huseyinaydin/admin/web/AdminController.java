package tr.com.huseyinaydin.admin.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tr.com.huseyinaydin.admin.service.AdminUserService;
import tr.com.huseyinaydin.auth.domain.RoleName;

import java.util.Set;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminController {
    private final AdminUserService adminUserService;

    @GetMapping
    String users(Model model) {
        model.addAttribute("users", adminUserService.listUsers());
        model.addAttribute("availableRoles", RoleName.values());
        return "admin/users";
    }

    @PostMapping("/{userId}/active")
    String toggleActive(@PathVariable Long userId) {
        try {
            adminUserService.toggleActive(userId);
        } catch (IllegalArgumentException ignored) {
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{userId}/roles")
    String updateRoles(@PathVariable Long userId, @RequestParam(required = false) Set<RoleName> roles) {
        try {
            adminUserService.updateRoles(userId, roles);
        } catch (IllegalArgumentException ignored) {
        }
        return "redirect:/admin/users";
    }
}

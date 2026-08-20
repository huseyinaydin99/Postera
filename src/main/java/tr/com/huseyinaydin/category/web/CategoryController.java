package tr.com.huseyinaydin.category.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tr.com.huseyinaydin.category.service.CategoryService;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    String index(Authentication authentication, Model model) {
        model.addAttribute("categories", categoryService.list(authentication.getName()));
        return "categories/index";
    }

    @PostMapping
    String create(@RequestParam String name, Authentication authentication) {
        try {
            categoryService.create(authentication.getName(), name);
            return "redirect:/categories?created";
        } catch (IllegalArgumentException exception) {
            return "redirect:/categories?error";
        }
    }

    @PostMapping("/{categoryId}")
    String rename(@PathVariable Long categoryId, @RequestParam String name, Authentication authentication) {
        try {
            categoryService.rename(authentication.getName(), categoryId, name);
            return "redirect:/categories?updated";
        } catch (IllegalArgumentException exception) {
            return "redirect:/categories?error";
        }
    }

    @PostMapping("/{categoryId}/delete")
    String delete(@PathVariable Long categoryId, Authentication authentication) {
        try {
            categoryService.delete(authentication.getName(), categoryId);
        } catch (IllegalArgumentException ignored) {
        }
        return "redirect:/categories";
    }
}

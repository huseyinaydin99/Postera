package tr.com.huseyinaydin.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    String index() {
        return "redirect:/messages/inbox";
    }
}

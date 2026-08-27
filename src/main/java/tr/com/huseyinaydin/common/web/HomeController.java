package tr.com.huseyinaydin.common.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import tr.com.huseyinaydin.timeline.service.TimelineFeedResponse;
import tr.com.huseyinaydin.timeline.service.TimelineService;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final TimelineService timelineService;

    @GetMapping({"/", "/home", "/feed"})
    public String index(Authentication authentication, Model model) {
        var feed = timelineService.getFriendsFeed(authentication.getName(), 0, 6);
        model.addAttribute("posts", feed.posts());
        model.addAttribute("hasMore", feed.hasMore());
        model.addAttribute("nextOffset", feed.nextOffset());
        return "home/index";
    }

    @GetMapping("/api/feed")
    @ResponseBody
    public TimelineFeedResponse getFeedPosts(
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "3") int limit,
            Authentication authentication) {
        return timelineService.getFriendsFeed(authentication.getName(), offset, limit);
    }
}

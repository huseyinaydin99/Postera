package tr.com.huseyinaydin.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class UserPresenceInterceptor implements HandlerInterceptor {

    private final AppUserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            var email = auth.getName();
            // Fire and forget or update synchronously. Since it's an interceptor, synchronous might be slow if we hit DB every request.
            // But we filter by checking the lastSeenAt. We can do a quick check and update if > 2 minutes.
            userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                var now = OffsetDateTime.now();
                if (user.getLastSeenAt() == null || ChronoUnit.MINUTES.between(user.getLastSeenAt(), now) >= 2) {
                    user.updateLastSeenAt(now);
                    userRepository.save(user); // Wait, this is outside a transaction, but JpaRepository.save handles it.
                }
            });
        }
        return true;
    }
}

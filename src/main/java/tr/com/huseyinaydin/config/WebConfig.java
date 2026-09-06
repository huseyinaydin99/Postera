package tr.com.huseyinaydin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties(UploadProperties.class)
public class WebConfig implements WebMvcConfigurer {
    private final UploadProperties uploadProperties;
    private final tr.com.huseyinaydin.common.web.UserPresenceInterceptor presenceInterceptor;

    public WebConfig(UploadProperties uploadProperties, tr.com.huseyinaydin.common.web.UserPresenceInterceptor presenceInterceptor) {
        this.uploadProperties = uploadProperties;
        this.presenceInterceptor = presenceInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        var location = Path.of(uploadProperties.profileDirectory()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/profile/**").addResourceLocations(location.endsWith("/") ? location : location + "/");
        var messageLocation = Path.of(uploadProperties.messageDirectory()).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/messages/**").addResourceLocations(messageLocation.endsWith("/") ? messageLocation : messageLocation + "/");
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(presenceInterceptor).excludePathPatterns("/css/**", "/js/**", "/images/**", "/uploads/**");
    }
}

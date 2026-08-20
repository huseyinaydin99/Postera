package tr.com.huseyinaydin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "postera.uploads")
public record UploadProperties(String profileDirectory) {
}

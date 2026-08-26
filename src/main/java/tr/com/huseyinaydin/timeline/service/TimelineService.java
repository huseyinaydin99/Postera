package tr.com.huseyinaydin.timeline.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tr.com.huseyinaydin.auth.domain.AppUser;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.message.service.MessageImageStorage;
import tr.com.huseyinaydin.message.service.RichTextSanitizer;
import tr.com.huseyinaydin.timeline.domain.TimelinePost;
import tr.com.huseyinaydin.timeline.repository.TimelinePostRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private final TimelinePostRepository timelinePostRepository;
    private final AppUserRepository userRepository;
    private final RichTextSanitizer richTextSanitizer;
    private final MessageImageStorage messageImageStorage;

    @Transactional
    public Long createPost(String currentUserEmail, String content, List<MultipartFile> images) {
        var user = userRepository.findByEmailIgnoreCase(currentUserEmail)
                .orElseThrow(() -> new IllegalStateException("Kullanıcı bulunamadı."));

        var sanitizedContent = richTextSanitizer.sanitize(content);
        var imageUrls = messageImageStorage.storeAll(images);

        if (!richTextSanitizer.hasText(content) && imageUrls.isEmpty()) {
            throw new IllegalArgumentException("Paylaşım için metin, GIF veya en az bir görsel eklemelisiniz.");
        }

        var post = TimelinePost.create(user, sanitizedContent);
        imageUrls.forEach(post::addImage);

        return timelinePostRepository.save(post).getId();
    }
}

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

import tr.com.huseyinaydin.timeline.domain.TimelinePostImage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private final TimelinePostRepository timelinePostRepository;
    private final AppUserRepository userRepository;
    private final RichTextSanitizer richTextSanitizer;
    private final MessageImageStorage messageImageStorage;

    @Transactional(readOnly = true)
    public List<TimelinePostItem> listPosts(String currentUserEmail) {
        var user = findUser(currentUserEmail);
        var isAdmin = user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()) || "ADMIN".equals(role.getName()));

        return timelinePostRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(post -> {
                    var author = post.getUser();
                    var authorName = author.getFirstName() + " " + author.getLastName();
                    var isOwned = author.getId().equals(user.getId()) || isAdmin;
                    var imageUrls = post.getImages().stream().map(TimelinePostImage::getImageUrl).toList();
                    var sanitizedContent = richTextSanitizer.sanitize(post.getContent());

                    return new TimelinePostItem(
                            post.getId(),
                            author.getId(),
                            authorName,
                            author.getEmail(),
                            author.getProfileImageUrl(),
                            sanitizedContent,
                            imageUrls,
                            post.getCreatedAt(),
                            post.getUpdatedAt(),
                            isOwned
                    );
                })
                .toList();
    }

    @Transactional
    public Long createPost(String currentUserEmail, String content, List<MultipartFile> images) {
        var user = findUser(currentUserEmail);

        var sanitizedContent = richTextSanitizer.sanitize(content);
        var imageUrls = messageImageStorage.storeAll(images);

        if (!richTextSanitizer.hasText(content) && imageUrls.isEmpty()) {
            throw new IllegalArgumentException("Paylaşım için metin, GIF veya en az bir görsel eklemelisiniz.");
        }

        var post = TimelinePost.create(user, sanitizedContent);
        imageUrls.forEach(post::addImage);

        return timelinePostRepository.save(post).getId();
    }

    @Transactional
    public void deletePost(String currentUserEmail, Long postId) {
        var user = findUser(currentUserEmail);
        var post = timelinePostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Paylaşım bulunamadı."));

        var isAdmin = user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()) || "ADMIN".equals(role.getName()));
        if (!post.getUser().getId().equals(user.getId()) && !isAdmin) {
            throw new IllegalArgumentException("Bu paylaşımı silme yetkiniz bulunmamaktadır.");
        }

        timelinePostRepository.delete(post);
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Kullanıcı bulunamadı."));
    }
}

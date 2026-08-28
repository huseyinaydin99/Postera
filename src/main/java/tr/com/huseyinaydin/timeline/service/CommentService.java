package tr.com.huseyinaydin.timeline.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.domain.AppUser;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.notification.domain.NotificationType;
import tr.com.huseyinaydin.notification.service.NotificationService;
import tr.com.huseyinaydin.timeline.domain.PostComment;
import tr.com.huseyinaydin.timeline.repository.PostCommentRepository;
import tr.com.huseyinaydin.timeline.repository.TimelinePostRepository;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w.\\-]+(?:\\s[\\w.\\-]+)?)");
    private static final int MAX_CONTENT_LENGTH = 1000;

    private final PostCommentRepository commentRepository;
    private final TimelinePostRepository postRepository;
    private final AppUserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<CommentItem> getComments(Long postId, String currentUserEmail) {
        var currentUser = findUser(currentUserEmail);
        var flat = commentRepository.findByPostId(postId);

        // Build tree: top-level comments with their replies
        Map<Long, CommentItem> itemMap = new LinkedHashMap<>();
        Map<Long, List<CommentItem>> replyMap = new LinkedHashMap<>();

        for (var c : flat) {
            var item = toItem(c, currentUser.getId(), new ArrayList<>());
            itemMap.put(c.getId(), item);
            if (c.getParent() != null) {
                replyMap.computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>()).add(item);
            }
        }

        List<CommentItem> roots = new ArrayList<>();
        for (var c : flat) {
            if (c.getParent() == null) {
                var replies = replyMap.getOrDefault(c.getId(), List.of());
                roots.add(toItem(c, currentUser.getId(), replies));
            }
        }
        return roots;
    }

    @Transactional
    public CommentItem addComment(Long postId, Long parentId, String content, String currentUserEmail) {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Yorum boş olamaz.");
        if (content.length() > MAX_CONTENT_LENGTH) throw new IllegalArgumentException("Yorum çok uzun.");

        var author = findUser(currentUserEmail);
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Paylaşım bulunamadı."));

        PostComment parent = null;
        if (parentId != null) {
            parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Üst yorum bulunamadı."));
            if (!parent.getPost().getId().equals(postId)) throw new IllegalArgumentException("Geçersiz üst yorum.");
        }

        var sanitized = sanitize(content);
        var comment = PostComment.create(post, author, parent, sanitized);
        commentRepository.save(comment);

        var actorName = author.getFirstName() + " " + author.getLastName();

        // Notify post owner (if not self)
        if (parent == null) {
            notificationService.createNotification(
                    post.getUser(), author, NotificationType.POST_COMMENT,
                    "Paylaşımınıza yorum yapıldı",
                    actorName + " paylaşımınıza yorum yaptı.",
                    "/?comment=" + comment.getId() + "#post-" + postId
            );
        } else {
            // Notify parent comment author
            notificationService.createNotification(
                    parent.getAuthor(), author, NotificationType.COMMENT_REPLY,
                    "Yorumunuza cevap verildi",
                    actorName + " yorumunuza cevap verdi.",
                    "/?comment=" + comment.getId() + "#post-" + postId
            );
        }

        // Notify mentioned users
        notifyMentions(sanitized, author, post.getId(), comment.getId());

        return toItem(comment, author.getId(), List.of());
    }

    @Transactional
    public void deleteComment(Long commentId, String currentUserEmail) {
        var user = findUser(currentUserEmail);
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Yorum bulunamadı."));
        var isAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()) || "ADMIN".equals(r.getName()));
        if (!comment.getAuthor().getId().equals(user.getId()) && !isAdmin) {
            throw new IllegalStateException("Bu yorumu silme yetkiniz yok.");
        }
        commentRepository.delete(comment);
    }

    public Map<Long, Long> countsByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return Map.of();
        var rows = commentRepository.countGroupedByPostIds(postIds);
        return rows.stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void notifyMentions(String content, AppUser actor, Long postId, Long commentId) {
        var matcher = MENTION_PATTERN.matcher(content);
        Set<String> seen = new HashSet<>();
        while (matcher.find()) {
            var raw = matcher.group(1).trim();
            if (!seen.add(raw.toLowerCase())) continue;
            // Try full name match first, then email prefix
            userRepository.findAll().stream()
                    .filter(u -> !u.getId().equals(actor.getId()))
                    .filter(u -> {
                        var fullName = (u.getFirstName() + " " + u.getLastName()).toLowerCase();
                        return fullName.equals(raw.toLowerCase()) || u.getEmail().toLowerCase().startsWith(raw.toLowerCase());
                    })
                    .findFirst()
                    .ifPresent(mentioned -> notificationService.createNotification(
                            mentioned, actor, NotificationType.COMMENT_MENTION,
                            "Bir yorumda etiketlendiniz",
                            actor.getFirstName() + " " + actor.getLastName() + " sizi bir yorumda etiketledi.",
                            "/?comment=" + commentId + "#post-" + postId
                    ));
        }
    }

    private CommentItem toItem(PostComment c, Long currentUserId, List<CommentItem> replies) {
        var a = c.getAuthor();
        return new CommentItem(
                c.getId(),
                c.getPost().getId(),
                a.getId(),
                a.getFirstName() + " " + a.getLastName(),
                a.getProfileImageUrl(),
                c.getParent() != null ? c.getParent().getId() : null,
                c.getContent(),
                c.getCreatedAt(),
                a.getId().equals(currentUserId),
                replies
        );
    }

    private String sanitize(String raw) {
        if (raw == null) return "";
        return raw.replace("<", "&lt;").replace(">", "&gt;").trim();
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Kullanıcı bulunamadı."));
    }
}

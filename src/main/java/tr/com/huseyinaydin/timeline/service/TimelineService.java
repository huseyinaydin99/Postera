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
import tr.com.huseyinaydin.timeline.domain.TimelineReactionType;
import tr.com.huseyinaydin.timeline.repository.TimelinePostReactionRepository;
import tr.com.huseyinaydin.notification.domain.NotificationType;
import tr.com.huseyinaydin.notification.service.NotificationService;
import tr.com.huseyinaydin.friend.service.FriendService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private final TimelinePostRepository timelinePostRepository;
    private final AppUserRepository userRepository;
    private final RichTextSanitizer richTextSanitizer;
    private final MessageImageStorage messageImageStorage;
    private final TimelinePostReactionRepository reactionRepository;
    private final NotificationService notificationService;
    private final FriendService friendService;

    @Transactional(readOnly = true)
    public List<TimelinePostItem> listPosts(String currentUserEmail) {
        var user = findUser(currentUserEmail);
        var isAdmin = user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()) || "ADMIN".equals(role.getName()));

        return timelinePostRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(post -> toPostItem(post, user, isAdmin))
                .toList();
    }

    @Transactional(readOnly = true)
    public TimelineFeedResponse getFriendsFeed(String currentUserEmail, int offset, int limit) {
        var user = findUser(currentUserEmail);
        var isAdmin = user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()) || "ADMIN".equals(role.getName()));

        long totalCount = timelinePostRepository.countFriendsFeedPosts(user.getId());
        if (totalCount == 0) {
            return new TimelineFeedResponse(List.of(), 0, false, 0);
        }

        var pageRequest = new tr.com.huseyinaydin.message.service.OffsetPageRequest(offset, limit);
        var posts = timelinePostRepository.findFriendsFeedPosts(user.getId(), pageRequest);

        var items = posts.stream()
                .map(post -> toPostItem(post, user, isAdmin))
                .toList();

        boolean hasMore = (offset + items.size()) < totalCount;
        int nextOffset = offset + items.size();
        return new TimelineFeedResponse(items, totalCount, hasMore, nextOffset);
    }

    private TimelinePostItem toPostItem(TimelinePost post, AppUser user, boolean isAdmin) {
        var author = post.getUser();
        var authorName = author.getFirstName() + " " + author.getLastName();
        var isOwned = author.getId().equals(user.getId()) || isAdmin;
        var imageUrls = post.getImages().stream().map(TimelinePostImage::getImageUrl).toList();
        var sanitizedContent = richTextSanitizer.sanitize(post.getContent());
        var reactionSummary = summarizeReactions(post, user.getId());

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
                isOwned,
                reactionSummary.reactions(),
                reactionSummary.currentUserReaction()
        );
    }

    @Transactional
    public PostReactionResponse react(String currentUserEmail, Long postId, TimelineReactionType type) {
        var user = findUser(currentUserEmail);
        var post = timelinePostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Paylaşım bulunamadı."));
        if (!friendService.areFriends(user.getId(), post.getUser().getId())) {
            throw new IllegalArgumentException("Bu paylaşıma tepki verme izniniz yok.");
        }
        var existing = reactionRepository.findByPostIdAndUserId(postId, user.getId());
        var notifyOwner = false;

        if (existing.isPresent() && existing.get().getReactionType() == type) {
            reactionRepository.delete(existing.get());
        } else if (existing.isPresent()) {
            existing.get().changeTo(type);
            notifyOwner = true;
        } else {
            reactionRepository.save(tr.com.huseyinaydin.timeline.domain.TimelinePostReaction.create(post, user, type));
            notifyOwner = true;
        }

        if (notifyOwner) {
            var actorName = user.getFirstName() + " " + user.getLastName();
            notificationService.createNotification(
                    post.getUser(), user, NotificationType.POST_REACTION,
                    "Paylaşımınıza tepki verildi",
                    actorName + " paylaşımınıza " + type.label().toLowerCase(java.util.Locale.forLanguageTag("tr-TR")) + " tepkisi verdi " + type.emoji(),
                    "/#post-" + postId
            );
        }
        reactionRepository.flush();
        return summarizeReactions(reactionRepository.findAllByPostId(postId), user.getId());
    }

    private PostReactionResponse summarizeReactions(TimelinePost post, Long currentUserId) {
        return summarizeReactions(post.getReactions(), currentUserId);
    }

    private PostReactionResponse summarizeReactions(java.util.List<tr.com.huseyinaydin.timeline.domain.TimelinePostReaction> postReactions, Long currentUserId) {
        var grouped = new java.util.EnumMap<TimelineReactionType, Long>(TimelineReactionType.class);
        String currentUserReaction = null;
        for (var reaction : postReactions) {
            grouped.merge(reaction.getReactionType(), 1L, Long::sum);
            if (reaction.getUser().getId().equals(currentUserId)) currentUserReaction = reaction.getReactionType().name();
        }
        var summaries = java.util.Arrays.stream(TimelineReactionType.values())
                .filter(type -> grouped.containsKey(type))
                .map(type -> new TimelineReactionSummary(type.name(), type.emoji(), type.label(), grouped.get(type)))
                .toList();
        return new PostReactionResponse(summaries, currentUserReaction);
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

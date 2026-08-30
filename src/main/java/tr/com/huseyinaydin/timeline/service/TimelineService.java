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
import tr.com.huseyinaydin.timeline.repository.PostCommentRepository;

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
    private final PostCommentRepository commentRepository;

    @Transactional(readOnly = true)
    public List<TimelinePostItem> listPosts(String currentUserEmail) {
        var user = findUser(currentUserEmail);
        var isAdmin = user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()) || "ADMIN".equals(role.getName()));
        var posts = timelinePostRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        var postIds = posts.stream().map(p -> p.getId()).toList();
        var counts = commentRepository.countGroupedByPostIds(postIds).stream()
                .collect(java.util.stream.Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        return posts.stream().map(post -> toPostItem(post, user, isAdmin, counts.getOrDefault(post.getId(), 0L))).toList();
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
        var postIds = posts.stream().map(p -> p.getId()).toList();
        var counts = commentRepository.countGroupedByPostIds(postIds).stream()
                .collect(java.util.stream.Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        var items = posts.stream()
                .map(post -> toPostItem(post, user, isAdmin, counts.getOrDefault(post.getId(), 0L)))
                .toList();

        boolean hasMore = (offset + items.size()) < totalCount;
        int nextOffset = offset + items.size();
        return new TimelineFeedResponse(items, totalCount, hasMore, nextOffset);
    }

    private TimelinePostItem toPostItem(TimelinePost post, AppUser user, boolean isAdmin, long commentCount) {
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
                reactionSummary.currentUserReaction(),
                commentCount
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

    @Transactional(readOnly = true)
    public TimelinePostItem getPostForEdit(String currentUserEmail, Long postId) {
        var user = findUser(currentUserEmail);
        var post = timelinePostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Paylaşım bulunamadı."));
        var isAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()) || "ADMIN".equals(r.getName()));
        if (!post.getUser().getId().equals(user.getId()) && !isAdmin)
            throw new IllegalArgumentException("Bu paylaşımı düzenleme yetkiniz yok.");
        return toPostItem(post, user, isAdmin, 0L);
    }

    @Transactional
    public TimelinePostItem updatePost(String currentUserEmail, Long postId, String content, List<MultipartFile> newImages, List<String> keepImageUrls) {
        var user = findUser(currentUserEmail);
        var post = timelinePostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Paylaşım bulunamadı."));
        var isAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()) || "ADMIN".equals(r.getName()));
        if (!post.getUser().getId().equals(user.getId()) && !isAdmin)
            throw new IllegalArgumentException("Bu paylaşımı düzenleme yetkiniz yok.");

        var sanitized = richTextSanitizer.sanitize(content);
        var uploadedUrls = messageImageStorage.storeAll(newImages);

        var kept = keepImageUrls == null ? List.<String>of() : keepImageUrls;
        var totalImages = kept.size() + uploadedUrls.size();
        if (totalImages > 2) throw new IllegalArgumentException("Bir paylaşımda en fazla 2 görsel olabilir.");
        if (!richTextSanitizer.hasText(content) && totalImages == 0)
            throw new IllegalArgumentException("Paylaşım için metin veya en az bir görsel gereklidir.");

        post.updateContent(sanitized);
        post.clearImages();
        kept.forEach(post::addImage);
        uploadedUrls.forEach(post::addImage);

        timelinePostRepository.flush();
        var counts = commentRepository.countGroupedByPostIds(List.of(postId)).stream()
                .collect(java.util.stream.Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        return toPostItem(post, user, isAdmin, counts.getOrDefault(postId, 0L));
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, java.util.List<ReactionUserItem>> getReactionUsers(Long postId) {
        var reactions = reactionRepository.findAllByPostId(postId);
        var result = new java.util.LinkedHashMap<String, java.util.List<ReactionUserItem>>();
        for (var r : reactions) {
            var key = r.getReactionType().name();
            result.computeIfAbsent(key, k -> new java.util.ArrayList<>())
                  .add(new ReactionUserItem(
                          r.getUser().getId(),
                          r.getUser().getFirstName() + " " + r.getUser().getLastName(),
                          r.getUser().getProfileImageUrl()));
        }
        return result;
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Kullanıcı bulunamadı."));
    }
}

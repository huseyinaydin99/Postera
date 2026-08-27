package tr.com.huseyinaydin.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.domain.AppUser;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.message.service.OffsetPageRequest;
import tr.com.huseyinaydin.notification.domain.Notification;
import tr.com.huseyinaydin.notification.domain.NotificationType;
import tr.com.huseyinaydin.notification.repository.NotificationRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public Notification createNotification(AppUser recipient,
                                           AppUser actor,
                                           NotificationType type,
                                           String title,
                                           String message,
                                           String targetUrl) {
        if (recipient.getId().equals(actor != null ? actor.getId() : null)) {
            // Do not notify user of their own actions
            return null;
        }
        var notification = Notification.create(recipient, actor, type, title, message, targetUrl);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public long countUnread(String currentUserEmail) {
        var user = findUser(currentUserEmail);
        return notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(String currentUserEmail, int offset, int limit) {
        var user = findUser(currentUserEmail);
        long unreadCount = notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
        long totalCount = notificationRepository.countByRecipientId(user.getId());

        if (totalCount == 0) {
            return new NotificationListResponse(List.of(), 0, 0, false, 0);
        }

        var pageRequest = new OffsetPageRequest(offset, limit);
        var notifications = notificationRepository.findNotificationsByRecipientId(user.getId(), pageRequest);

        var items = notifications.stream().map(n -> {
            String actorName = n.getActor() != null
                    ? n.getActor().getFirstName() + " " + n.getActor().getLastName()
                    : "Postera";
            String actorImage = n.getActor() != null ? n.getActor().getProfileImageUrl() : null;

            return new NotificationItem(
                    n.getId(),
                    n.getType().name(),
                    n.getTitle(),
                    n.getMessage(),
                    actorName,
                    actorImage,
                    n.getTargetUrl(),
                    n.isRead(),
                    n.getCreatedAt()
            );
        }).toList();

        boolean hasMore = (offset + items.size()) < totalCount;
        int nextOffset = offset + items.size();
        return new NotificationListResponse(items, unreadCount, totalCount, hasMore, nextOffset);
    }

    @Transactional
    public void markAsRead(String currentUserEmail, Long notificationId) {
        var user = findUser(currentUserEmail);
        var notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Bildirim bulunamadı."));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new IllegalStateException("Bu bildirimi güncelleme yetkiniz yok.");
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String currentUserEmail) {
        var user = findUser(currentUserEmail);
        notificationRepository.markAllAsReadByRecipientId(user.getId(), OffsetDateTime.now());
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Kullanıcı bulunamadı."));
    }
}

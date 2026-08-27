package tr.com.huseyinaydin.notification.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tr.com.huseyinaydin.notification.domain.Notification;

import java.time.OffsetDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    long countByRecipientId(Long recipientId);

    @EntityGraph(attributePaths = {"actor"})
    @Query("""
        SELECT n FROM Notification n
        WHERE n.recipient.id = :recipientId
        ORDER BY CASE WHEN n.isRead = false THEN 0 ELSE 1 END ASC, n.createdAt DESC
    """)
    List<Notification> findNotificationsByRecipientId(@Param("recipientId") Long recipientId, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.recipient.id = :recipientId AND n.isRead = false")
    int markAllAsReadByRecipientId(@Param("recipientId") Long recipientId, @Param("now") OffsetDateTime now);
}

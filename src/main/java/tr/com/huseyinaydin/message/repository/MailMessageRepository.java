package tr.com.huseyinaydin.message.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tr.com.huseyinaydin.message.domain.MailMessage;

import java.util.Optional;
import java.time.Instant;
import java.util.List;

public interface MailMessageRepository extends JpaRepository<MailMessage, Long>, JpaSpecificationExecutor<MailMessage> {

    // amaç sender için sonradan ayrı bir sorgu çalıştırmak yerine, ana sorguda birlikte getirmektir; ayrı sorgu çalışırsa gereksiz ek veritabanı sorguları oluşabilir.
    @EntityGraph(attributePaths = "sender")
    Page<MailMessage> findByReceiverIdAndDraftFalseAndTrashFalseAndReceiverDeletedFalseOrderBySentAtDesc(Long receiverId, Pageable pageable);

    /**
     * Gelen kutusu için: conversationId başına yalnızca en son mesajı döndürür.
     * Bu sayede aynı kişiyle sürdürülen konuşma tek satır olarak görünür.
     */
    @EntityGraph(attributePaths = "sender")
    @Query("""
            select m from MailMessage m
            where m.receiver.id = :receiverId
              and m.draft = false
              and m.trash = false
              and m.receiverDeleted = false
              and m.sentAt = (
                  select max(m2.sentAt) from MailMessage m2
                  where m2.conversationId = m.conversationId
                    and m2.draft = false
              )
            order by m.sentAt desc
            """)
    Page<MailMessage> findLatestPerConversationForReceiver(@Param("receiverId") Long receiverId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "images"})
    @Query("""
            select message from MailMessage message
            where message.receiver.id = :receiverId and message.draft = false and message.trash = false and message.receiverDeleted = false
            order by message.sentAt desc
            """)
    List<MailMessage> findRecentInboxMessages(@Param("receiverId") Long receiverId, Pageable pageable);

    @EntityGraph(attributePaths = "receiver")
    Page<MailMessage> findBySenderIdAndDraftFalseAndSenderTrashFalseAndSenderDeletedFalseOrderBySentAtDesc(Long senderId, Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    Page<MailMessage> findByReceiverIdAndImportantTrueAndTrashFalseAndReceiverDeletedFalseAndDraftFalseOrderBySentAtDesc(Long receiverId,
                                                                                                                            Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("""
            select message from MailMessage message
            where (message.receiver.id = :userId and message.trash = true and message.receiverDeleted = false)
               or (message.sender.id = :userId and message.senderTrash = true and message.senderDeleted = false)
            order by message.sentAt desc
            """)
    Page<MailMessage> findTrashByOwnerId(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("""
            select message from MailMessage message
            where (message.receiver.id = :userId and message.trash = true and message.receiverDeleted = false)
               or (message.sender.id = :userId and message.senderTrash = true and message.senderDeleted = false)
            """)
    List<MailMessage> findAllTrashByOwnerId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = "receiver")
    Page<MailMessage> findBySenderIdAndDraftTrueAndSenderTrashFalseAndSenderDeletedFalseOrderBySentAtDesc(Long senderId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"sender", "receiver", "category", "images"})
    Optional<MailMessage> findById(Long id);

    @EntityGraph(attributePaths = {"sender", "receiver", "images"})
    List<MailMessage> findByConversationIdOrderBySentAtAsc(String conversationId);

    /**
     * İki kullanıcı arasında var olan en eski sohbet odasının ID'sini döndürür.
     * Native SQL kullanarak JPQL + Pageable uyumluluk sorununu aşar.
     */
    @Query(value = """
            SELECT conversation_id FROM mail_messages
            WHERE conversation_id IS NOT NULL
              AND draft = false
              AND (
                (sender_id = :userId1 AND receiver_id = :userId2)
                OR
                (sender_id = :userId2 AND receiver_id = :userId1)
              )
            ORDER BY sent_at ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> findExistingConversationBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Gönderilen kutusunda conversationId başına yalnızca en son mesajı döndürür.
     */
    @Query(value = """
            SELECT id FROM mail_messages m
            WHERE m.sender_id = :senderId
              AND m.draft = false
              AND m.sender_trash = false
              AND m.sender_deleted = false
              AND m.sent_at = (
                SELECT MAX(m2.sent_at) FROM mail_messages m2
                WHERE m2.conversation_id = m.conversation_id
                  AND m2.draft = false
              )
            ORDER BY m.sent_at DESC
            """, nativeQuery = true)
    List<Long> findLatestIdPerConversationForSender(@Param("senderId") Long senderId, Pageable pageable);

    @Query(value = """
            SELECT COUNT(DISTINCT m.conversation_id) FROM mail_messages m
            WHERE m.sender_id = :senderId
              AND m.draft = false
              AND m.sender_trash = false
              AND m.sender_deleted = false
              AND m.conversation_id IS NOT NULL
            """, nativeQuery = true)
    long countConversationsForSender(@Param("senderId") Long senderId);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("""
            select message from MailMessage message left join message.receiver receiver
            where ((message.sender.id = :userId and message.senderDeleted = false)
                or (message.receiver.id = :userId and message.receiverDeleted = false))
              and (lower(message.subject) like lower(concat('%', :query, '%'))
                or lower(message.body) like lower(concat('%', :query, '%'))
                or lower(message.sender.firstName) like lower(concat('%', :query, '%'))
                or lower(message.sender.lastName) like lower(concat('%', :query, '%'))
                or lower(concat(concat(message.sender.firstName, ' '), message.sender.lastName)) like lower(concat('%', :query, '%'))
                or lower(message.sender.email) like lower(concat('%', :query, '%'))
                or lower(receiver.firstName) like lower(concat('%', :query, '%'))
                or lower(receiver.lastName) like lower(concat('%', :query, '%'))
                or lower(concat(concat(receiver.firstName, ' '), receiver.lastName)) like lower(concat('%', :query, '%'))
                or lower(receiver.email) like lower(concat('%', :query, '%')))
            order by message.sentAt desc
            """)
    Page<MailMessage> searchOwnedMessages(@Param("userId") Long userId, @Param("query") String query, Pageable pageable);

    long countByReceiverIdAndDraftFalseAndTrashFalseAndReceiverDeletedFalse(Long receiverId);

    long countByReceiverIdAndDraftFalseAndTrashFalseAndReceiverDeletedFalseAndReadFalse(Long receiverId);

    long countBySenderIdAndDraftTrueAndSenderTrashFalseAndSenderDeletedFalse(Long senderId);

    long countBySenderIdAndDraftFalseAndSenderTrashFalseAndSenderDeletedFalse(Long senderId);

    long countByReceiverIdAndImportantTrueAndTrashFalseAndReceiverDeletedFalseAndDraftFalse(Long receiverId);

    long countByDraftFalse();

    long countByDraftFalseAndSentAtGreaterThanEqualAndSentAtLessThan(Instant from, Instant to);

    long countByDraftFalseAndTrashFalseAndReadFalse();

    long countByTrashTrue();

    @Query("""
            select m.sender.firstName, m.sender.lastName, count(m)
            from MailMessage m where m.draft = false
            group by m.sender.id, m.sender.firstName, m.sender.lastName
            order by count(m) desc
            """)
    List<Object[]> findTopSenders(org.springframework.data.domain.Pageable pageable);

    @Query("""
            select m.category.name, count(m)
            from MailMessage m where m.category is not null
            group by m.category.id, m.category.name
            order by count(m) desc
            """)
    List<Object[]> findTopCategories(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MailMessage m
            set m.read = true
            where m.receiver.id = :receiverId
              and m.sender.id = :senderId
              and m.read = false
            """)
    int markAllAsReadFromSender(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MailMessage m
            set m.read = true
            where m.receiver.id = :receiverId
              and m.conversationId = :conversationId
              and m.read = false
            """)
    int markAllAsReadInConversation(@Param("receiverId") Long receiverId, @Param("conversationId") String conversationId);
}

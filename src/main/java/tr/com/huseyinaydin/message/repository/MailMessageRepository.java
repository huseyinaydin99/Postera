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
}

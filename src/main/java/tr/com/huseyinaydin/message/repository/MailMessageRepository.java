package tr.com.huseyinaydin.message.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import tr.com.huseyinaydin.message.domain.MailMessage;

import java.util.Optional;
import java.time.Instant;
import java.util.List;

public interface MailMessageRepository extends JpaRepository<MailMessage, Long>, JpaSpecificationExecutor<MailMessage> {

    // amaç sender için sonradan ayrı bir sorgu çalıştırmak yerine, ana sorguda birlikte getirmektir; ayrı sorgu çalışırsa gereksiz ek veritabanı sorguları oluşabilir.
    @EntityGraph(attributePaths = "sender")
    Page<MailMessage> findByReceiverIdAndDraftFalseAndTrashFalseOrderBySentAtDesc(Long receiverId, Pageable pageable);

    @EntityGraph(attributePaths = "receiver")
    Page<MailMessage> findBySenderIdAndDraftFalseOrderBySentAtDesc(Long senderId, Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    Page<MailMessage> findByReceiverIdAndImportantTrueAndTrashFalseAndDraftFalseOrderBySentAtDesc(Long receiverId,
                                                                                                      Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    Page<MailMessage> findByReceiverIdAndTrashTrueOrderBySentAtDesc(Long receiverId, Pageable pageable);

    @EntityGraph(attributePaths = "receiver")
    Page<MailMessage> findBySenderIdAndDraftTrueAndTrashFalseOrderBySentAtDesc(Long senderId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"sender", "receiver", "category"})
    Optional<MailMessage> findById(Long id);

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

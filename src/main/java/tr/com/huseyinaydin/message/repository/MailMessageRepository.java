package tr.com.huseyinaydin.message.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.huseyinaydin.message.domain.MailMessage;

import java.util.Optional;

public interface MailMessageRepository extends JpaRepository<MailMessage, Long> {

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

    @Override
    @EntityGraph(attributePaths = {"sender", "receiver"})
    Optional<MailMessage> findById(Long id);
}

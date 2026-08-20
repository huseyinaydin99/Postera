package tr.com.huseyinaydin.report.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.huseyinaydin.report.domain.MessageReport;

import java.util.List;

public interface MessageReportRepository extends JpaRepository<MessageReport, Long> {
    boolean existsByMessageIdAndReportedByUserId(Long messageId, Long reportedByUserId);

    @EntityGraph(attributePaths = {"message", "message.sender", "message.receiver", "reportedByUser"})
    List<MessageReport> findAllByOrderByReportedAtDesc();
}

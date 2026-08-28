package tr.com.huseyinaydin.timeline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.huseyinaydin.timeline.domain.TimelinePostReaction;

import java.util.Optional;
import java.util.List;

public interface TimelinePostReactionRepository extends JpaRepository<TimelinePostReaction, Long> {
    Optional<TimelinePostReaction> findByPostIdAndUserId(Long postId, Long userId);
    List<TimelinePostReaction> findAllByPostId(Long postId);
}

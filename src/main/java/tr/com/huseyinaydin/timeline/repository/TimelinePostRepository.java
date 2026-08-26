package tr.com.huseyinaydin.timeline.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import tr.com.huseyinaydin.timeline.domain.TimelinePost;

public interface TimelinePostRepository extends JpaRepository<TimelinePost, Long> {

    @EntityGraph(attributePaths = {"user", "images"})
    Page<TimelinePost> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

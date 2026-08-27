package tr.com.huseyinaydin.timeline.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tr.com.huseyinaydin.timeline.domain.TimelinePost;

import java.util.List;

public interface TimelinePostRepository extends JpaRepository<TimelinePost, Long> {

    @EntityGraph(attributePaths = {"user", "images"})
    List<TimelinePost> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "images"})
    List<TimelinePost> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user", "images"})
    Page<TimelinePost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "images"})
    @Query("""
        SELECT p FROM TimelinePost p
        WHERE p.user.id IN (
            SELECT CASE WHEN f.sender.id = :userId THEN f.receiver.id ELSE f.sender.id END
            FROM Friendship f
            WHERE (f.sender.id = :userId OR f.receiver.id = :userId)
              AND f.status = tr.com.huseyinaydin.friend.domain.FriendshipStatus.ACCEPTED
        )
        ORDER BY p.createdAt DESC
    """)
    List<TimelinePost> findFriendsFeedPosts(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT COUNT(p) FROM TimelinePost p
        WHERE p.user.id IN (
            SELECT CASE WHEN f.sender.id = :userId THEN f.receiver.id ELSE f.sender.id END
            FROM Friendship f
            WHERE (f.sender.id = :userId OR f.receiver.id = :userId)
              AND f.status = tr.com.huseyinaydin.friend.domain.FriendshipStatus.ACCEPTED
        )
    """)
    long countFriendsFeedPosts(@Param("userId") Long userId);
}

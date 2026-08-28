package tr.com.huseyinaydin.timeline.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tr.com.huseyinaydin.timeline.domain.PostComment;

import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    @EntityGraph(attributePaths = {"author", "parent", "parent.author"})
    @Query("SELECT c FROM PostComment c WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<PostComment> findByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(c) FROM PostComment c WHERE c.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(c) FROM PostComment c WHERE c.post.id IN :postIds")
    long countByPostIdIn(@Param("postIds") List<Long> postIds);

    @Query("SELECT c.post.id, COUNT(c) FROM PostComment c WHERE c.post.id IN :postIds GROUP BY c.post.id")
    List<Object[]> countGroupedByPostIds(@Param("postIds") List<Long> postIds);
}

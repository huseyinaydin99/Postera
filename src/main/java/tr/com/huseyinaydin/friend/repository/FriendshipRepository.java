package tr.com.huseyinaydin.friend.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tr.com.huseyinaydin.friend.domain.Friendship;
import tr.com.huseyinaydin.friend.domain.FriendshipStatus;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE (f.sender.id = :u1 AND f.receiver.id = :u2) OR (f.sender.id = :u2 AND f.receiver.id = :u1)")
    Optional<Friendship> findRelationBetween(@Param("u1") Long u1, @Param("u2") Long u2);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("SELECT f FROM Friendship f WHERE f.sender.id = :userId OR f.receiver.id = :userId")
    List<Friendship> findAllByUserId(@Param("userId") Long userId);

    long countByReceiverIdAndStatus(Long receiverId, FriendshipStatus status);

    @EntityGraph(attributePaths = {"sender"})
    @Query("SELECT f FROM Friendship f WHERE f.receiver.id = :receiverId AND f.status = :status ORDER BY f.createdAt DESC")
    List<Friendship> findPendingRequestsByReceiverId(@Param("receiverId") Long receiverId,
                                                    @Param("status") FriendshipStatus status,
                                                    Pageable pageable);

    @EntityGraph(attributePaths = {"receiver"})
    List<Friendship> findBySenderIdAndStatus(Long senderId, FriendshipStatus status);
}

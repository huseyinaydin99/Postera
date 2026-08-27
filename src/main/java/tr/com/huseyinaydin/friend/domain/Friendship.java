package tr.com.huseyinaydin.friend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tr.com.huseyinaydin.auth.domain.AppUser;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "friendships",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_friendships_sender_receiver",
                columnNames = {"sender_id", "receiver_id"}
        )
)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private AppUser sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private AppUser receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FriendshipStatus status = FriendshipStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public static Friendship create(AppUser sender, AppUser receiver) {
        var friendship = new Friendship();
        friendship.sender = sender;
        friendship.receiver = receiver;
        friendship.status = FriendshipStatus.PENDING;
        friendship.createdAt = OffsetDateTime.now();
        return friendship;
    }

    public void updateStatus(FriendshipStatus status) {
        this.status = status;
        this.updatedAt = OffsetDateTime.now();
    }

    public void accept() {
        updateStatus(FriendshipStatus.ACCEPTED);
    }

    public void reject() {
        updateStatus(FriendshipStatus.REJECTED);
    }

    public void block() {
        updateStatus(FriendshipStatus.BLOCKED);
    }
}

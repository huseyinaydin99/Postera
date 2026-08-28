package tr.com.huseyinaydin.timeline.domain;

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

import java.time.Instant;

@Entity
@Table(name = "timeline_post_reactions", uniqueConstraints = @UniqueConstraint(name = "uk_timeline_reaction_post_user", columnNames = {"post_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimelinePostReaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private TimelinePost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 20)
    private TimelineReactionType reactionType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static TimelinePostReaction create(TimelinePost post, AppUser user, TimelineReactionType reactionType) {
        var reaction = new TimelinePostReaction();
        reaction.post = post;
        reaction.user = user;
        reaction.reactionType = reactionType;
        reaction.createdAt = Instant.now();
        return reaction;
    }

    public void changeTo(TimelineReactionType reactionType) { this.reactionType = reactionType; }
}

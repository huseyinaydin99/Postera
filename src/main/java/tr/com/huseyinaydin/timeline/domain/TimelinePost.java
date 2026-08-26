package tr.com.huseyinaydin.timeline.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tr.com.huseyinaydin.auth.domain.AppUser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "timeline_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimelinePost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<TimelinePostImage> images = new ArrayList<>();

    public static TimelinePost create(AppUser user, String content) {
        var post = new TimelinePost();
        post.user = user;
        post.content = content;
        post.createdAt = Instant.now();
        return post;
    }

    public void markAsUpdated() {
        this.updatedAt = Instant.now();
    }

    public void updateContent(String newContent) {
        this.content = newContent;
        this.updatedAt = Instant.now();
    }

    public void addImage(String imageUrl) {
        images.add(TimelinePostImage.create(this, imageUrl, images.size()));
    }

    public List<TimelinePostImage> getImages() {
        return Collections.unmodifiableList(images);
    }
}

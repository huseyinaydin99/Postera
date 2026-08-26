package tr.com.huseyinaydin.timeline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "timeline_post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimelinePostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private TimelinePost post;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private int displayOrder;

    public static TimelinePostImage create(TimelinePost post, String imageUrl, int displayOrder) {
        var image = new TimelinePostImage();
        image.post = post;
        image.imageUrl = imageUrl;
        image.displayOrder = displayOrder;
        return image;
    }
}

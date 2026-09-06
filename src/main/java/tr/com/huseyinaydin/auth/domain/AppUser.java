package tr.com.huseyinaydin.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "last_seen_at")
    private java.time.OffsetDateTime lastSeenAt;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "presence_status", length = 50)
    private PresenceStatus presenceStatus = PresenceStatus.AVAILABLE;

    /*
        // tam olarak mesele bu: app_user_roles bir ara tablo olduğu için hem user_id hem de role_id
        değerini tutuyor; inverseJoinColumns da bu tablodaki karşı entity'nin ID'sini gösteriyor.

        app_user_roles
        ┌─────────┬─────────┐
        │ user_id │ role_id │
        ├─────────┼─────────┤
        │    5    │    2    │
        └─────────┴─────────┘
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "app_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<AppRole> roles = new HashSet<>();

    public static AppUser create(String firstName, String lastName, String email, String passwordHash) {
        var user = new AppUser();
        user.firstName = firstName;
        user.lastName = lastName;
        user.email = email;
        user.passwordHash = passwordHash;
        return user;
    }

    public static AppUser createPending(String firstName, String lastName, String email, String passwordHash) {
        var user = create(firstName, lastName, email, passwordHash);
        user.active = false;
        return user;
    }

    public void activate() {
        active = true;
    }

    public void assignRole(AppRole role) {
        roles.add(role);
    }

    public void updateProfile(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public void toggleActive() {
        active = !active;
    }

    public void replaceRoles(Set<AppRole> roles) {
        this.roles.clear();
        this.roles.addAll(roles);
    }

    public void changeEmail(String email) {
        this.email = email;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateCoverImage(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public void updatePresenceStatus(PresenceStatus status) {
        this.presenceStatus = status;
    }

    public void updateLastSeenAt(java.time.OffsetDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}

package tr.com.huseyinaydin.friend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.domain.AppUser;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.friend.domain.Friendship;
import tr.com.huseyinaydin.friend.domain.FriendshipStatus;
import tr.com.huseyinaydin.friend.repository.FriendshipRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tr.com.huseyinaydin.notification.domain.NotificationType;
import tr.com.huseyinaydin.notification.service.NotificationService;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final AppUserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<DiscoverUserItem> listDiscoverUsers(String currentUserEmail, String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        var currentUser = findUserByEmail(currentUserEmail);
        var users = userRepository.searchActiveUsers(query.trim(), currentUser.getId());
        if (users.isEmpty()) {
            return List.of();
        }

        var userFriendships = friendshipRepository.findAllByUserId(currentUser.getId());
        Map<Long, Friendship> relationMap = new HashMap<>();
        for (var f : userFriendships) {
            var otherUserId = f.getSender().getId().equals(currentUser.getId())
                    ? f.getReceiver().getId()
                    : f.getSender().getId();
            relationMap.put(otherUserId, f);
        }

        return users.stream().map(user -> {
            var relation = relationMap.get(user.getId());
            var status = resolveStatus(currentUser.getId(), relation);
            return new DiscoverUserItem(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getFirstName() + " " + user.getLastName(),
                    user.getEmail(),
                    user.getProfileImageUrl(),
                    status
            );
        }).toList();
    }

    @Transactional
    public String sendFriendRequest(String senderEmail, Long targetUserId) {
        var sender = findUserByEmail(senderEmail);
        if (sender.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Kendinize arkadaşlık isteği gönderemezsiniz.");
        }

        var target = userRepository.findById(targetUserId)
                .filter(AppUser::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));

        var existingRelation = friendshipRepository.findRelationBetween(sender.getId(), target.getId());
        if (existingRelation.isPresent()) {
            var friendship = existingRelation.get();
            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new IllegalStateException("Bu kullanıcı ile zaten arkadaşsınız.");
            }
            if (friendship.getStatus() == FriendshipStatus.PENDING) {
                if (friendship.getSender().getId().equals(sender.getId())) {
                    return "PENDING_SENT";
                } else {
                    friendship.accept();
                    String actorName = sender.getFirstName() + " " + sender.getLastName();
                    notificationService.createNotification(
                            friendship.getSender(),
                            sender,
                            NotificationType.FRIEND_REQUEST_ACCEPTED,
                            "Arkadaşlık İsteği Kabul Edildi",
                            actorName + " arkadaşlık isteğinizi kabul etti.",
                            "/home"
                    );
                    return "FRIENDS";
                }
            }
            if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
                throw new IllegalStateException("Bu işlem gerçekleştirilemiyor.");
            }
            // If REJECTED, re-send
            friendshipRepository.delete(friendship);
            friendshipRepository.flush();
        }

        var newFriendship = Friendship.create(sender, target);
        friendshipRepository.save(newFriendship);
        return "PENDING_SENT";
    }

    @Transactional(readOnly = true)
    public long countPendingRequests(String currentUserEmail) {
        var currentUser = findUserByEmail(currentUserEmail);
        return friendshipRepository.countByReceiverIdAndStatus(currentUser.getId(), FriendshipStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public boolean areFriends(Long firstUserId, Long secondUserId) {
        if (firstUserId.equals(secondUserId)) return true;
        return friendshipRepository.findRelationBetween(firstUserId, secondUserId)
                .map(friendship -> friendship.getStatus() == FriendshipStatus.ACCEPTED)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public FriendRequestsResponse getPendingRequests(String currentUserEmail, int offset, int limit) {
        var currentUser = findUserByEmail(currentUserEmail);
        long totalCount = friendshipRepository.countByReceiverIdAndStatus(currentUser.getId(), FriendshipStatus.PENDING);
        if (totalCount == 0) {
            return new FriendRequestsResponse(List.of(), 0, false, 0);
        }

        var pageRequest = new tr.com.huseyinaydin.message.service.OffsetPageRequest(offset, limit);
        var friendships = friendshipRepository.findPendingRequestsByReceiverId(
                currentUser.getId(),
                FriendshipStatus.PENDING,
                pageRequest
        );

        var items = friendships.stream().map(f -> new FriendRequestItem(
                f.getId(),
                f.getSender().getId(),
                f.getSender().getFirstName() + " " + f.getSender().getLastName(),
                f.getSender().getEmail(),
                f.getSender().getProfileImageUrl(),
                f.getCreatedAt()
        )).toList();

        boolean hasMore = (offset + items.size()) < totalCount;
        int nextOffset = offset + items.size();
        return new FriendRequestsResponse(items, totalCount, hasMore, nextOffset);
    }

    @Transactional
    public void acceptFriendRequest(String currentUserEmail, Long friendshipId) {
        var currentUser = findUserByEmail(currentUserEmail);
        var friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Arkadaşlık isteği bulunamadı."));

        if (!friendship.getReceiver().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Bu arkadaşlık isteğini kabul etme yetkiniz yok.");
        }

        if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
            return;
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Bu istek bekleyen durumda değil.");
        }

        friendship.accept();
        friendshipRepository.save(friendship);

        // Notify the original sender that their friend request was accepted
        String actorName = currentUser.getFirstName() + " " + currentUser.getLastName();
        notificationService.createNotification(
                friendship.getSender(),
                currentUser,
                NotificationType.FRIEND_REQUEST_ACCEPTED,
                "Arkadaşlık İsteği Kabul Edildi",
                actorName + " arkadaşlık isteğinizi kabul etti.",
                "/home"
        );
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getFriendsList(String currentUserEmail) {
        var currentUser = findUserByEmail(currentUserEmail);
        var friendships = friendshipRepository.findAllByUserId(currentUser.getId());
        return friendships.stream()
                .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .map(f -> {
                    var other = f.getSender().getId().equals(currentUser.getId()) ? f.getReceiver() : f.getSender();
                    return java.util.Map.<String, Object>of(
                            "id", other.getId(),
                            "name", other.getFirstName() + " " + other.getLastName(),
                            "email", other.getEmail(),
                            "profileImageUrl", other.getProfileImageUrl() != null ? other.getProfileImageUrl() : ""
                    );
                })
                .toList();
    }

    private String resolveStatus(Long currentUserId, Friendship friendship) {
        if (friendship == null) {
            return "NONE";
        }
        if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
            return "FRIENDS";
        }
        if (friendship.getStatus() == FriendshipStatus.PENDING) {
            return friendship.getSender().getId().equals(currentUserId)
                    ? "PENDING_SENT"
                    : "PENDING_RECEIVED";
        }
        if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
            return "BLOCKED";
        }
        return "NONE";
    }

    private AppUser findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Oturumu açık kullanıcı bulunamadı."));
    }
}

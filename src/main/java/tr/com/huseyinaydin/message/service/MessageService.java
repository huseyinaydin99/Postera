package tr.com.huseyinaydin.message.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.domain.AppUser;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.category.repository.MailCategoryRepository;
import tr.com.huseyinaydin.message.domain.MailMessage;
import tr.com.huseyinaydin.message.repository.MailMessageRepository;
import tr.com.huseyinaydin.message.web.SendMessageRequest;
import tr.com.huseyinaydin.message.web.DraftMessageRequest;

import java.util.Locale;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import tr.com.huseyinaydin.message.web.InboxFilter;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int PAGE_SIZE = 5;

    // sourceUserId + "_" + targetUserId -> timestamp (epoch milli)
    private final ConcurrentMap<String, Long> typingState = new ConcurrentHashMap<>();

    private final MailMessageRepository messageRepository;
    private final AppUserRepository userRepository;
    private final MailCategoryRepository categoryRepository;
    private final RichTextSanitizer richTextSanitizer;
    private final MessageImageStorage messageImageStorage;
    private final MessageFileStorage messageFileStorage;
    private final tr.com.huseyinaydin.friend.service.FriendService friendService;

    public void setTyping(Long senderId, Long targetUserId, boolean typing) {
        String key = senderId + "_" + targetUserId;
        if (typing) {
            typingState.put(key, System.currentTimeMillis());
        } else {
            typingState.remove(key);
        }
    }

    public void setTyping(String currentUserEmail, Long targetUserId, boolean typing) {
        var user = findUser(currentUserEmail);
        setTyping(user.getId(), targetUserId, typing);
    }

    public boolean isUserTyping(Long senderId, Long targetUserId) {
        String key = senderId + "_" + targetUserId;
        Long lastTime = typingState.get(key);
        if (lastTime == null) return false;
        if (System.currentTimeMillis() - lastTime > 4000) {
            typingState.remove(key);
            return false;
        }
        return true;
    }

    @Transactional
    public String send(String senderEmail, SendMessageRequest request) {
        var sender = findUser(senderEmail);
        var receiverEmail = request.receiverEmail().trim().toLowerCase(Locale.ROOT);
        var receiver = userRepository.findByEmailIgnoreCase(receiverEmail)
                .orElseThrow(() -> new IllegalArgumentException("Bu e-posta adresiyle kayıtlı bir kullanıcı bulunamadı."));

        if (friendService.isBlocked(sender.getId(), receiver.getId())) {
            throw new IllegalArgumentException("Bu kullanıcı ile mesajlaşamazsınız.");
        }

        // İki kullanıcı arasında mevcut sohbet varsa o odayı kullan, yoksa yeni oda oluştur
        var existingConversationId = messageRepository
                .findExistingConversationBetween(sender.getId(), receiver.getId())
                .orElse(null);

        MailMessage message;
        if (existingConversationId != null) {
            message = MailMessage.reply(sender, receiver, request.subject().trim(), request.body().trim(), existingConversationId);
        } else {
            message = MailMessage.send(sender, receiver, request.subject().trim(), request.body().trim());
        }

        if (request.file() != null && !request.file().isEmpty()) {
            var stored = messageFileStorage.store(request.file(), request.fileAlias());
            message.attachFile(stored.fileName(), stored.originalName(), stored.alias(), stored.fileSize(), stored.contentType());
        }
        messageRepository.save(message);
        return fullName(receiver);
    }

    @Transactional(readOnly = true)
    public Page<MessageListItem> inbox(String currentUserEmail, int page) {
        var user = findUser(currentUserEmail);
        return messageRepository.findLatestPerConversationForReceiver(user.getId(), pageRequest(page))
                .map(message -> toListItem(message, message.getSender()));
    }

    @Transactional(readOnly = true)
    public RecentMessagesResponse recentMessages(String currentUserEmail, int offset, int limit) {
        var user = findUser(currentUserEmail);
        var safeOffset = Math.max(offset, 0);
        var safeLimit = Math.max(1, Math.min(limit, 50));
        var total = messageRepository.countByReceiverIdAndDraftFalseAndTrashFalseAndReceiverDeletedFalse(user.getId());
        var unreadCount = messageRepository.countByReceiverIdAndDraftFalseAndTrashFalseAndReceiverDeletedFalseAndReadFalse(user.getId());
        var pageable = new OffsetPageRequest(safeOffset, safeLimit, Sort.by(Sort.Direction.DESC, "sentAt"));
        var items = messageRepository.findRecentInboxMessages(user.getId(), pageable).stream()
                .map(message -> toListItem(message, message.getSender()))
                .toList();
        var hasMore = (safeOffset + items.size()) < total;
        var nextOffset = safeOffset + items.size();
        return new RecentMessagesResponse(items, total, unreadCount, hasMore, nextOffset);
    }

    @Transactional(readOnly = true)
    public Page<MessageListItem> inbox(String currentUserEmail, InboxFilter filter) {
        var user = findUser(currentUserEmail);
        Specification<MailMessage> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("receiver").get("id"), user.getId()),
                cb.isFalse(root.get("draft")), cb.isFalse(root.get("trash")), cb.isFalse(root.get("receiverDeleted")));
        if (filter.sender() != null && !filter.sender().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(cb.concat(cb.concat(root.get("sender").get("firstName"), " "), root.get("sender").get("lastName"))), "%" + filter.sender().trim().toLowerCase(Locale.ROOT) + "%"));
        }
        if (filter.subject() != null && !filter.subject().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("subject")), "%" + filter.subject().trim().toLowerCase(Locale.ROOT) + "%"));
        }
        if (filter.categoryId() != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), filter.categoryId()));
        if (filter.read() != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("read"), filter.read()));
        if (filter.important() != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("important"), filter.important()));
        if (filter.from() != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("sentAt"), filter.from().atStartOfDay().toInstant(ZoneOffset.UTC)));
        if (filter.to() != null) spec = spec.and((root, query, cb) -> cb.lessThan(root.get("sentAt"), filter.to().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
        var sort = Sort.by(filter.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC, "sentAt");
        return messageRepository.findAll(spec, PageRequest.of(filter.pageNumber(), PAGE_SIZE, sort)).map(this::toInboxListItem);
    }

    @Transactional(readOnly = true)
    public Page<MessageListItem> sent(String currentUserEmail, int page) {
        var user = findUser(currentUserEmail);
        // conversationId başına yalnızca en son mesajın id'lerini getir, sonra o mesajları çek
        var pageable = pageRequest(page);
        var ids = messageRepository.findLatestIdPerConversationForSender(user.getId(), pageable);
        if (ids.isEmpty()) {
            return org.springframework.data.domain.Page.empty(pageable);
        }
        var messages = messageRepository.findAllById(ids).stream()
                .sorted(java.util.Comparator.comparing(MailMessage::getSentAt).reversed())
                .toList();
        long total = messageRepository.countConversationsForSender(user.getId());
        return new org.springframework.data.domain.PageImpl<>(
                messages.stream().map(m -> toListItem(m, m.getReceiver())).toList(),
                pageable,
                total
        );
    }

    @Transactional(readOnly = true)
    public Page<MessageListItem> important(String currentUserEmail, int page) {
        var user = findUser(currentUserEmail);
        return messageRepository
                .findByReceiverIdAndImportantTrueAndTrashFalseAndReceiverDeletedFalseAndDraftFalseOrderBySentAtDesc(user.getId(), pageRequest(page))
                .map(this::toInboxListItem);
    }

    @Transactional(readOnly = true)
    public Page<MessageListItem> trash(String currentUserEmail, int page) {
        var user = findUser(currentUserEmail);
        return messageRepository.findTrashByOwnerId(user.getId(), pageRequest(page))
                .map(message -> toListItemForOwner(message, user.getId()));
    }

    @Transactional(readOnly = true)
    public Page<MessageListItem> drafts(String currentUserEmail, int page) {
        var user = findUser(currentUserEmail);
        return messageRepository.findBySenderIdAndDraftTrueAndSenderTrashFalseAndSenderDeletedFalseOrderBySentAtDesc(user.getId(), pageRequest(page))
                .map(message -> toListItem(message, message.getReceiver()));
    }

    @Transactional(readOnly = true)
    public Page<MessageSearchItem> search(String currentUserEmail, String query, int page) {
        var user = findUser(currentUserEmail);
        var normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) return Page.empty(pageRequest(page));
        return messageRepository.searchOwnedMessages(user.getId(), normalizedQuery, pageRequest(page))
                .map(message -> toSearchItem(message, user.getId()));
    }

    @Transactional(readOnly = true)
    public MailboxCounts mailboxCounts(String currentUserEmail) {
        var userId = findUser(currentUserEmail).getId();
        return new MailboxCounts(
                messageRepository.countByReceiverIdAndDraftFalseAndTrashFalseAndReceiverDeletedFalse(userId),
                messageRepository.countByReceiverIdAndDraftFalseAndTrashFalseAndReceiverDeletedFalseAndReadFalse(userId),
                messageRepository.countBySenderIdAndDraftTrueAndSenderTrashFalseAndSenderDeletedFalse(userId),
                messageRepository.countBySenderIdAndDraftFalseAndSenderTrashFalseAndSenderDeletedFalse(userId),
                messageRepository.countByReceiverIdAndImportantTrueAndTrashFalseAndReceiverDeletedFalseAndDraftFalse(userId)
        );
    }

    @Transactional
    public MessageDetail getDetail(String currentUserEmail, Long messageId) {
        var user = findUser(currentUserEmail);
        var message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Mesaj bulunamadı."));

        var receivedByCurrentUser = message.getReceiver() != null && message.getReceiver().getId().equals(user.getId());
        var sentByCurrentUser = message.getSender().getId().equals(user.getId());
        if (!receivedByCurrentUser && !sentByCurrentUser) {
            throw new IllegalArgumentException("Bu mesaja erişim izniniz yok.");
        }
        if (receivedByCurrentUser && !message.isRead()) {
            message.markAsRead();
        }

        return new MessageDetail(
                message.getId(), message.getSubject(), message.getBody(),
                fullName(message.getSender()), message.getSender().getEmail(), message.getSender().getProfileImageUrl(),
                message.getReceiver() == null ? "Alıcı belirtilmedi" : fullName(message.getReceiver()),
                message.getReceiver() == null ? "" : message.getReceiver().getEmail(),
                message.getReceiver() == null ? null : message.getReceiver().getProfileImageUrl(),
                message.getSentAt(), receivedByCurrentUser, message.isImportant(),
                receivedByCurrentUser ? message.isTrash() : message.isSenderTrash(), message.isDraft(),
                message.getCategory() == null ? null : message.getCategory().getId(),
                message.getCategory() == null ? null : message.getCategory().getName()
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<ConversationMessage> conversation(String currentUserEmail, Long messageId) {
        var user = findUser(currentUserEmail);
        var message = findOwnedMessage(user, messageId);
        if (message.getConversationId() == null) {
            return java.util.List.of(toConversationMessage(message, user.getId()));
        }
        return messageRepository.findByConversationIdOrderBySentAtAsc(message.getConversationId()).stream()
                .filter(item -> isReceiver(user, item) || item.getSender().getId().equals(user.getId()))
                .map(item -> toConversationMessage(item, user.getId()))
                .toList();
    }

    @Transactional
    public void reply(String currentUserEmail, Long messageId, tr.com.huseyinaydin.message.web.ReplyMessageRequest request) {
        reply(currentUserEmail, messageId, request.body(), request.images(), request.file(), request.fileAlias());
    }

    @Transactional
    public void reply(String currentUserEmail, Long messageId, String body,
                      java.util.List<org.springframework.web.multipart.MultipartFile> images,
                      org.springframework.web.multipart.MultipartFile file,
                      String fileAlias) {
        var sender = findUser(currentUserEmail);
        var message = findOwnedMessage(sender, messageId);
        if (message.isDraft() || message.getReceiver() == null) {
            throw new IllegalArgumentException("Bu mesaja yanıt verilemez.");
        }
        message.startConversationIfMissing();
        var receiver = isReceiver(sender, message) ? message.getSender() : message.getReceiver();
        
        if (friendService.isBlocked(sender.getId(), receiver.getId())) {
            throw new IllegalArgumentException("Bu kullanıcı ile mesajlaşamazsınız.");
        }
        
        var imageUrls = messageImageStorage.storeAll(images);
        var hasFile = file != null && !file.isEmpty();
        if (!richTextSanitizer.hasText(body) && imageUrls.isEmpty() && !hasFile) {
            throw new IllegalArgumentException("Yanıt metni, görsel veya dosya zorunludur.");
        }
        var reply = MailMessage.reply(sender, receiver, replySubject(message.getSubject()), richTextSanitizer.sanitize(body), message.getConversationId());
        imageUrls.forEach(reply::addImage);
        if (hasFile) {
            var stored = messageFileStorage.store(file, fileAlias);
            reply.attachFile(stored.fileName(), stored.originalName(), stored.alias(), stored.fileSize(), stored.contentType());
        }
        messageRepository.save(reply);
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getConversationWithFriend(String currentUserEmail, Long friendId) {
        var user = findUser(currentUserEmail);
        var friend = userRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));

        var now = java.time.OffsetDateTime.now();
        boolean isOnline = false;
        if (friend.getLastSeenAt() != null && friend.getPresenceStatus() != tr.com.huseyinaydin.auth.domain.PresenceStatus.INVISIBLE) {
            isOnline = java.time.temporal.ChronoUnit.MINUTES.between(friend.getLastSeenAt(), now) <= 5;
        }
        String presenceLabel = friend.getPresenceStatus() != null ? friend.getPresenceStatus().label() : "Müsait";

        var existingConversationId = messageRepository
                .findExistingConversationBetween(user.getId(), friend.getId())
                .orElse(null);

        java.util.List<ConversationMessage> messages;
        if (existingConversationId != null) {
            messages = messageRepository.findByConversationIdOrderBySentAtAsc(existingConversationId).stream()
                    .filter(item -> isReceiver(user, item) || item.getSender().getId().equals(user.getId()))
                    .map(item -> toConversationMessage(item, user.getId()))
                    .toList();
        } else {
            messages = java.util.List.of();
        }

        boolean isPeerTyping = isUserTyping(friend.getId(), user.getId());

        return new ChatHistoryResponse(
                friend.getId(),
                fullName(friend),
                friend.getEmail(),
                friend.getProfileImageUrl(),
                isOnline,
                presenceLabel,
                friend.getLastSeenAt(),
                isPeerTyping,
                messages
        );
    }

    @Transactional
    public void markConversationAsRead(String currentUserEmail, Long friendId) {
        var user = findUser(currentUserEmail);
        messageRepository.markAllAsReadFromSender(user.getId(), friendId);
    }

    @Transactional
    public void markConversationAsReadByMessageId(String currentUserEmail, Long messageId) {
        var user = findUser(currentUserEmail);
        var message = findOwnedMessage(user, messageId);
        if (message.getConversationId() != null) {
            messageRepository.markAllAsReadInConversation(user.getId(), message.getConversationId());
        } else if (isReceiver(user, message) && !message.isRead()) {
            message.markAsRead();
        }
    }

    @Transactional(readOnly = true)
    public ConversationLiveResponse getConversationLive(String currentUserEmail, Long messageId) {
        var user = findUser(currentUserEmail);
        var message = findOwnedMessage(user, messageId);
        var counterpart = message.getSender().getId().equals(user.getId()) ? message.getReceiver() : message.getSender();
        boolean peerTyping = counterpart != null && isUserTyping(counterpart.getId(), user.getId());
        java.util.List<ConversationMessage> messages;
        if (message.getConversationId() == null) {
            messages = java.util.List.of(toConversationMessage(message, user.getId()));
        } else {
            messages = messageRepository.findByConversationIdOrderBySentAtAsc(message.getConversationId()).stream()
                    .filter(item -> isReceiver(user, item) || item.getSender().getId().equals(user.getId()))
                    .map(item -> toConversationMessage(item, user.getId()))
                    .toList();
        }
        return new ConversationLiveResponse(
                counterpart != null ? counterpart.getId() : null,
                counterpart != null ? fullName(counterpart) : "",
                peerTyping,
                messages
        );
    }

    public void setTypingForMessageCounterpart(String currentUserEmail, Long messageId, boolean typing) {
        var user = findUser(currentUserEmail);
        var message = findOwnedMessage(user, messageId);
        var counterpart = message.getSender().getId().equals(user.getId()) ? message.getReceiver() : message.getSender();
        if (counterpart != null) {
            setTyping(user.getId(), counterpart.getId(), typing);
        }
    }

    @Transactional
    public ConversationMessage sendMessageToFriend(String currentUserEmail, Long friendId, String body,
                                                   java.util.List<org.springframework.web.multipart.MultipartFile> images,
                                                   org.springframework.web.multipart.MultipartFile file,
                                                   String fileAlias) {
        var sender = findUser(currentUserEmail);
        var receiver = userRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));

        if (friendService.isBlocked(sender.getId(), receiver.getId())) {
            throw new IllegalArgumentException("Bu kullanıcı ile mesajlaşamazsınız.");
        }

        var imageUrls = messageImageStorage.storeAll(images);
        var hasFile = file != null && !file.isEmpty();
        if (!richTextSanitizer.hasText(body) && imageUrls.isEmpty() && !hasFile) {
            throw new IllegalArgumentException("Mesaj metni, görsel veya dosya zorunludur.");
        }

        var existingConversationId = messageRepository
                .findExistingConversationBetween(sender.getId(), receiver.getId())
                .orElse(null);

        MailMessage message;
        if (existingConversationId != null) {
            message = MailMessage.reply(sender, receiver, "Sohbet", richTextSanitizer.sanitize(body), existingConversationId);
        } else {
            message = MailMessage.send(sender, receiver, "Sohbet", richTextSanitizer.sanitize(body));
        }

        imageUrls.forEach(message::addImage);
        if (hasFile) {
            var stored = messageFileStorage.store(file, fileAlias);
            message.attachFile(stored.fileName(), stored.originalName(), stored.alias(), stored.fileSize(), stored.contentType());
        }

        var saved = messageRepository.save(message);
        return toConversationMessage(saved, sender.getId());
    }

    @Transactional
    public void assignCategory(String currentUserEmail, Long messageId, Long categoryId) {
        var message = findReceivedMessage(currentUserEmail, messageId);
        if (categoryId == null) {
            message.assignCategory(null);
            return;
        }
        var user = findUser(currentUserEmail);
        var category = categoryRepository.findByIdAndOwnerId(categoryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Kategori bulunamadı."));
        message.assignCategory(category);
    }

    @Transactional
    public void createDraft(String currentUserEmail, DraftMessageRequest request) {
        var sender = findUser(currentUserEmail);
        messageRepository.save(MailMessage.draft(sender, findOptionalReceiver(request.receiverEmail()), safeText(request.subject()), safeText(request.body())));
    }

    @Transactional(readOnly = true)
    public DraftMessageRequest getDraft(String currentUserEmail, Long messageId) {
        var draft = findDraft(currentUserEmail, messageId);
        return new DraftMessageRequest(
                draft.getReceiver() == null ? "" : draft.getReceiver().getEmail(), draft.getSubject(), draft.getBody()
        );
    }

    @Transactional
    public void updateDraft(String currentUserEmail, Long messageId, DraftMessageRequest request) {
        findDraft(currentUserEmail, messageId)
                .updateDraft(findOptionalReceiver(request.receiverEmail()), safeText(request.subject()), safeText(request.body()));
    }

    @Transactional
    public String publishDraft(String currentUserEmail, Long messageId, DraftMessageRequest request) {
        var receiver = findRequiredReceiver(request.receiverEmail());
        var subject = requireText(request.subject(), "Konu alanı zorunludur.");
        var body = requireText(request.body(), "Mesaj içeriği zorunludur.");
        findDraft(currentUserEmail, messageId).publish(receiver, subject, body);
        return fullName(receiver);
    }

    @Transactional
    public void toggleImportant(String currentUserEmail, Long messageId) {
        var message = findReceivedMessage(currentUserEmail, messageId);
        message.toggleImportant();
    }

    @Transactional
    public void moveToTrash(String currentUserEmail, Long messageId) {
        var user = findUser(currentUserEmail);
        var message = findOwnedMessage(user, messageId);
        if (isReceiver(user, message)) {
            message.moveToTrash();
        } else {
            message.moveSenderCopyToTrash();
        }
    }

    @Transactional
    public void restoreFromTrash(String currentUserEmail, Long messageId) {
        var user = findUser(currentUserEmail);
        var message = findOwnedMessage(user, messageId);
        if (isReceiver(user, message)) {
            message.restoreFromTrash();
        } else {
            message.restoreSenderCopyFromTrash();
        }
    }

    @Transactional
    public int moveSelectedToTrash(String currentUserEmail, Collection<Long> messageIds, String boxType) {
        if (messageIds == null || messageIds.isEmpty()) return 0;
        var user = findUser(currentUserEmail);
        var ids = messageIds.stream().filter(Objects::nonNull).distinct().toList();
        var moved = 0;
        for (var message : messageRepository.findAllById(ids)) {
            var receiverOwnsMessage = isReceiver(user, message);
            var senderOwnsMessage = message.getSender().getId().equals(user.getId());
            var eligible = switch (boxType) {
                case "inbox", "important" -> receiverOwnsMessage && !message.isDraft() && !message.isTrash() && !message.isReceiverDeleted();
                case "sent" -> senderOwnsMessage && !message.isDraft() && !message.isSenderTrash() && !message.isSenderDeleted();
                case "drafts" -> senderOwnsMessage && message.isDraft() && !message.isSenderTrash() && !message.isSenderDeleted();
                default -> false;
            };
            if (eligible) {
                if (receiverOwnsMessage) message.moveToTrash(); else message.moveSenderCopyToTrash();
                moved++;
            }
        }
        return moved;
    }

    @Transactional
    public int permanentlyDeleteSelectedTrash(String currentUserEmail, Collection<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return 0;
        }
        var uniqueMessageIds = messageIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (uniqueMessageIds.isEmpty()) {
            return 0;
        }
        var user = findUser(currentUserEmail);
        return permanentlyDeleteTrashMessages(user, messageRepository.findAllById(uniqueMessageIds));
    }

    @Transactional
    public int permanentlyDeleteAllTrash(String currentUserEmail) {
        var user = findUser(currentUserEmail);
        return permanentlyDeleteTrashMessages(user, messageRepository.findAllTrashByOwnerId(user.getId()));
    }

    private PageRequest pageRequest(int page) {
        return PageRequest.of(Math.max(page, 0), PAGE_SIZE);
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Oturumu açık kullanıcı bulunamadı."));
    }

    private String fullName(AppUser user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private MessageListItem toInboxListItem(MailMessage message) {
        return toListItem(message, message.getSender());
    }

    private MessageListItem toListItemForOwner(MailMessage message, Long ownerId) {
        var counterpart = message.getSender().getId().equals(ownerId) ? message.getReceiver() : message.getSender();
        return toListItem(message, counterpart);
    }

    private MessageListItem toListItem(MailMessage message, AppUser counterpart) {
        var preview = richTextSanitizer.sanitize(message.getBody()).replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        if (preview.isBlank() && !message.getImages().isEmpty()) preview = "İçerik";
        if (preview.isBlank()) preview = "İçerik";
        if (preview.length() > 72) preview = preview.substring(0, 69) + "…";
        return new MessageListItem(message.getId(), counterpart == null ? "Alıcı belirtilmedi" : fullName(counterpart),
                counterpart == null ? "" : counterpart.getEmail(), counterpart == null ? null : counterpart.getProfileImageUrl(),
                message.getSubject().isBlank() ? "(Konu yok)" : message.getSubject(), preview, message.getSentAt(),
                message.isRead(), message.isImportant());
    }

    public record AttachmentDownload(
            org.springframework.core.io.Resource resource,
            String downloadFileName,
            String contentType
    ) {}

    @Transactional(readOnly = true)
    public AttachmentDownload getAttachmentResource(String currentUserEmail, Long messageId, Long attachmentId) {
        var user = findUser(currentUserEmail);
        var message = findOwnedMessage(user, messageId);
        var attachment = message.getAttachment();
        if (attachment == null || !attachment.getId().equals(attachmentId)) {
            throw new IllegalArgumentException("Ekli dosya bulunamadı.");
        }
        var resource = messageFileStorage.loadAsResource(attachment.getFileName());
        return new AttachmentDownload(
                resource,
                attachment.getAlias(),
                attachment.getContentType()
        );
    }

    private ConversationMessage toConversationMessage(MailMessage message, Long currentUserId) {
        var attachment = message.getAttachment() != null
                ? new MessageAttachmentData(
                        message.getAttachment().getId(),
                        message.getAttachment().getAlias(),
                        message.getAttachment().getOriginalName(),
                        message.getAttachment().getFileSize(),
                        MessageAttachmentData.formatFileSize(message.getAttachment().getFileSize()))
                : null;
        return new ConversationMessage(
                message.getId(), fullName(message.getSender()), message.getSender().getEmail(), message.getSender().getProfileImageUrl(),
                richTextSanitizer.sanitize(message.getBody()), message.getImages().stream().map(image -> image.getImageUrl()).toList(),
                message.getSentAt(), message.getSender().getId().equals(currentUserId),
                attachment,
                message.isRead()
        );
    }

    private MessageSearchItem toSearchItem(MailMessage message, Long ownerId) {
        var counterpart = message.getSender().getId().equals(ownerId) ? message.getReceiver() : message.getSender();
        var preview = richTextSanitizer.sanitize(message.getBody()).replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        if (preview.length() > 140) preview = preview.substring(0, 137) + "…";
        return new MessageSearchItem(message.getId(), counterpart == null ? "Alıcı belirtilmedi" : fullName(counterpart),
                counterpart == null ? "" : counterpart.getEmail(), message.getSubject(), preview, message.getSentAt());
    }

    private String replySubject(String subject) {
        return subject.regionMatches(true, 0, "Re:", 0, 3) ? subject : "Re: " + subject;
    }

    private int permanentlyDeleteTrashMessages(AppUser user, Collection<MailMessage> messages) {
        var deleted = 0;
        var purgeableMessages = new java.util.ArrayList<MailMessage>();
        for (var message : messages) {
            var deletedForCurrentUser = false;
            if (isReceiver(user, message) && message.isTrash() && !message.isReceiverDeleted()) {
                message.permanentlyDeleteReceiverCopy();
                deletedForCurrentUser = true;
            }
            if (message.getSender().getId().equals(user.getId()) && message.isSenderTrash() && !message.isSenderDeleted()) {
                message.permanentlyDeleteSenderCopy();
                deletedForCurrentUser = true;
            }
            if (deletedForCurrentUser) deleted++;
            if (message.canBePurged()) purgeableMessages.add(message);
        }
        messageRepository.deleteAll(purgeableMessages);
        return deleted;
    }

    private MailMessage findOwnedMessage(AppUser user, Long messageId) {
        var message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Mesaj bulunamadı."));
        if (!isReceiver(user, message) && !message.getSender().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Bu mesaj üzerinde işlem yapma izniniz yok.");
        }
        return message;
    }

    private boolean isReceiver(AppUser user, MailMessage message) {
        return message.getReceiver() != null && message.getReceiver().getId().equals(user.getId());
    }

    private MailMessage findReceivedMessage(String currentUserEmail, Long messageId) {
        var user = findUser(currentUserEmail);
        var message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Mesaj bulunamadı."));
        if (!message.getReceiver().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Bu mesaj üzerinde işlem yapma izniniz yok.");
        }
        return message;
    }

    private MailMessage findDraft(String currentUserEmail, Long messageId) {
        var user = findUser(currentUserEmail);
        var message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Taslak bulunamadı."));
        if (!message.isDraft() || !message.getSender().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Bu taslak üzerinde işlem yapma izniniz yok.");
        }
        return message;
    }

    private AppUser findOptionalReceiver(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return findRequiredReceiver(email);
    }

    private AppUser findRequiredReceiver(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Alıcı e-posta adresi zorunludur.");
        }
        return userRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Bu e-posta adresiyle kayıtlı bir kullanıcı bulunamadı."));
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}

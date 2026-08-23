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
import tr.com.huseyinaydin.message.web.InboxFilter;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int PAGE_SIZE = 20;

    private final MailMessageRepository messageRepository;
    private final AppUserRepository userRepository;
    private final MailCategoryRepository categoryRepository;

    @Transactional
    public void send(String senderEmail, SendMessageRequest request) {
        var sender = findUser(senderEmail);
        var receiverEmail = request.receiverEmail().trim().toLowerCase(Locale.ROOT);
        var receiver = userRepository.findByEmailIgnoreCase(receiverEmail)
                .orElseThrow(() -> new IllegalArgumentException("Bu e-posta adresiyle kayıtlı bir kullanıcı bulunamadı."));

        messageRepository.save(MailMessage.send(sender, receiver, request.subject().trim(), request.body().trim()));
    }

    @Transactional(readOnly = true)
    public Page<MessageListItem> inbox(String currentUserEmail, int page) {
        var user = findUser(currentUserEmail);
        return messageRepository.findByReceiverIdAndDraftFalseAndTrashFalseOrderBySentAtDesc(user.getId(), pageRequest(page))
                .map(message -> new MessageListItem(
                        message.getId(), fullName(message.getSender()), message.getSender().getEmail(), message.getSubject(),
                        message.getSentAt(), message.isRead(), message.isImportant()
                ));
    }

    @Transactional(readOnly = true)
    public Page<MessageListItem> inbox(String currentUserEmail, InboxFilter filter) {
        var user = findUser(currentUserEmail);
        Specification<MailMessage> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("receiver").get("id"), user.getId()),
                cb.isFalse(root.get("draft")), cb.isFalse(root.get("trash")));
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
        return messageRepository.findBySenderIdAndDraftFalseOrderBySentAtDesc(user.getId(), pageRequest(page))
                .map(message -> new MessageListItem(
                        message.getId(), fullName(message.getReceiver()), message.getReceiver().getEmail(), message.getSubject(),
                        message.getSentAt(), message.isRead(), message.isImportant()
                ));
    }

    @Transactional(readOnly = true)
    public Page<MessageListItem> important(String currentUserEmail, int page) {
        var user = findUser(currentUserEmail);
        return messageRepository
                .findByReceiverIdAndImportantTrueAndTrashFalseAndDraftFalseOrderBySentAtDesc(user.getId(), pageRequest(page))
                .map(this::toInboxListItem);
    }

    @Transactional(readOnly = true)
    public Page<MessageListItem> trash(String currentUserEmail, int page) {
        var user = findUser(currentUserEmail);
        return messageRepository.findByReceiverIdAndTrashTrueOrderBySentAtDesc(user.getId(), pageRequest(page))
                .map(this::toInboxListItem);
    }

    @Transactional(readOnly = true)
    public Page<MessageListItem> drafts(String currentUserEmail, int page) {
        var user = findUser(currentUserEmail);
        return messageRepository.findBySenderIdAndDraftTrueAndTrashFalseOrderBySentAtDesc(user.getId(), pageRequest(page))
                .map(message -> new MessageListItem(
                        message.getId(), message.getReceiver() == null ? "Alıcı belirtilmedi" : fullName(message.getReceiver()),
                        message.getReceiver() == null ? "" : message.getReceiver().getEmail(), message.getSubject(),
                        message.getSentAt(), message.isRead(), message.isImportant()
                ));
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
                fullName(message.getSender()), message.getSender().getEmail(),
                message.getReceiver() == null ? "Alıcı belirtilmedi" : fullName(message.getReceiver()),
                message.getReceiver() == null ? "" : message.getReceiver().getEmail(),
                message.getSentAt(), receivedByCurrentUser, message.isImportant(), message.isTrash(), message.isDraft(),
                message.getCategory() == null ? null : message.getCategory().getId(),
                message.getCategory() == null ? null : message.getCategory().getName()
        );
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
    public void publishDraft(String currentUserEmail, Long messageId, DraftMessageRequest request) {
        var receiver = findRequiredReceiver(request.receiverEmail());
        var subject = requireText(request.subject(), "Konu alanı zorunludur.");
        var body = requireText(request.body(), "Mesaj içeriği zorunludur.");
        findDraft(currentUserEmail, messageId).publish(receiver, subject, body);
    }

    @Transactional
    public void toggleImportant(String currentUserEmail, Long messageId) {
        var message = findReceivedMessage(currentUserEmail, messageId);
        message.toggleImportant();
    }

    @Transactional
    public void moveToTrash(String currentUserEmail, Long messageId) {
        findReceivedMessage(currentUserEmail, messageId).moveToTrash();
    }

    @Transactional
    public void restoreFromTrash(String currentUserEmail, Long messageId) {
        findReceivedMessage(currentUserEmail, messageId).restoreFromTrash();
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
        return new MessageListItem(
                message.getId(), fullName(message.getSender()), message.getSender().getEmail(), message.getSubject(),
                message.getSentAt(), message.isRead(), message.isImportant()
        );
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

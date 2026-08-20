package tr.com.huseyinaydin.message.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.domain.AppUser;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.message.domain.MailMessage;
import tr.com.huseyinaydin.message.repository.MailMessageRepository;
import tr.com.huseyinaydin.message.web.SendMessageRequest;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int PAGE_SIZE = 20;

    private final MailMessageRepository messageRepository;
    private final AppUserRepository userRepository;

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
                        message.getSentAt(), message.isRead()
                ));
    }

    @Transactional(readOnly = true)
    public Page<MessageListItem> sent(String currentUserEmail, int page) {
        var user = findUser(currentUserEmail);
        return messageRepository.findBySenderIdAndDraftFalseOrderBySentAtDesc(user.getId(), pageRequest(page))
                .map(message -> new MessageListItem(
                        message.getId(), fullName(message.getReceiver()), message.getReceiver().getEmail(), message.getSubject(),
                        message.getSentAt(), message.isRead()
                ));
    }

    @Transactional
    public MessageDetail getDetail(String currentUserEmail, Long messageId) {
        var user = findUser(currentUserEmail);
        var message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Mesaj bulunamadı."));

        var receivedByCurrentUser = message.getReceiver().getId().equals(user.getId());
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
                fullName(message.getReceiver()), message.getReceiver().getEmail(),
                message.getSentAt(), receivedByCurrentUser
        );
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
}

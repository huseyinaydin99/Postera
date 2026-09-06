package tr.com.huseyinaydin.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.domain.AppUser;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.message.repository.MailMessageRepository;
import tr.com.huseyinaydin.message.service.MessageService;
import tr.com.huseyinaydin.message.web.ReplyMessageRequest;
import tr.com.huseyinaydin.message.web.SendMessageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MessageAttachmentIntegrationTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private MailMessageRepository messageRepository;

    private AppUser user1;
    private AppUser user2;
    private AppUser user3;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(AppUser.create("Ahmet", "Yılmaz", "ahmet@test.com", "hash123"));
        user2 = userRepository.save(AppUser.create("Mehmet", "Demir", "mehmet@test.com", "hash123"));
        user3 = userRepository.save(AppUser.create("Ayşe", "Kaya", "ayse@test.com", "hash123"));
    }

    @Test
    void shouldSendMessageWithAttachmentAndRetrieveIt() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "proje-raporu.pdf",
                "application/pdf",
                "pdf dosya icerigi test verisi".getBytes()
        );

        SendMessageRequest sendRequest = new SendMessageRequest(
                user2.getEmail(),
                "Proje Raporu Ektedir",
                "Merhaba, raporu ekte iletiyorum.",
                file,
                "Proje Raporu"
        );

        String sentTo = messageService.send(user1.getEmail(), sendRequest);
        assertThat(sentTo).isEqualTo(user2.getFirstName() + " " + user2.getLastName());

        var messages = messageRepository.findAll();
        var sentMessage = messages.stream()
                .filter(m -> m.getSubject().equals("Proje Raporu Ektedir"))
                .findFirst()
                .orElseThrow();

        assertThat(sentMessage.getAttachment()).isNotNull();
        assertThat(sentMessage.getAttachment().getOriginalName()).isEqualTo("proje-raporu.pdf");
        assertThat(sentMessage.getAttachment().getAlias()).isEqualTo("Proje Raporu.pdf");
        assertThat(sentMessage.getAttachment().getFileSize()).isEqualTo("pdf dosya icerigi test verisi".getBytes().length);

        var conversation = messageService.conversation(user1.getEmail(), sentMessage.getId());
        assertThat(conversation).hasSize(1);
        var convMsg = conversation.get(0);
        assertThat(convMsg.attachment()).isNotNull();
        assertThat(convMsg.attachment().alias()).isEqualTo("Proje Raporu.pdf");
        assertThat(convMsg.attachment().formattedSize()).isEqualTo("29 B");

        // Sender access
        var downloadSender = messageService.getAttachmentResource(user1.getEmail(), sentMessage.getId(), convMsg.attachment().id());
        assertThat(downloadSender.resource().exists()).isTrue();
        assertThat(downloadSender.downloadFileName()).isEqualTo("Proje Raporu.pdf");

        // Receiver access
        var downloadReceiver = messageService.getAttachmentResource(user2.getEmail(), sentMessage.getId(), convMsg.attachment().id());
        assertThat(downloadReceiver.resource().exists()).isTrue();

        // Unauthorized user access
        assertThatThrownBy(() -> messageService.getAttachmentResource(user3.getEmail(), sentMessage.getId(), convMsg.attachment().id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("izniniz yok");
    }

    @Test
    void shouldReplyWithAttachment() {
        // First user1 sends normal message
        SendMessageRequest sendRequest = new SendMessageRequest(
                user2.getEmail(),
                "Toplantı Notları",
                "Notları paylaşabilir misin?"
        );
        messageService.send(user1.getEmail(), sendRequest);

        var originalMessage = messageRepository.findAll().stream()
                .filter(m -> m.getSubject().equals("Toplantı Notları"))
                .findFirst()
                .orElseThrow();

        // User2 replies with attachment
        MockMultipartFile replyFile = new MockMultipartFile(
                "file",
                "notlar.txt",
                "text/plain",
                "Toplantı kararları...".getBytes()
        );

        ReplyMessageRequest replyRequest = new ReplyMessageRequest(
                "İşte toplantı notları:",
                null,
                replyFile,
                "Toplanti-Notlari"
        );

        messageService.reply(user2.getEmail(), originalMessage.getId(), replyRequest);

        var conversation = messageService.conversation(user2.getEmail(), originalMessage.getId());
        assertThat(conversation).hasSize(2);

        var replyMsg = conversation.get(1);
        assertThat(replyMsg.attachment()).isNotNull();
        assertThat(replyMsg.attachment().alias()).isEqualTo("Toplanti-Notlari.txt");
    }
}

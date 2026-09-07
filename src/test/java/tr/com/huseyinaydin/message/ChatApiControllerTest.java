package tr.com.huseyinaydin.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tr.com.huseyinaydin.auth.domain.AppUser;
import tr.com.huseyinaydin.auth.repository.AppUserRepository;
import tr.com.huseyinaydin.message.service.ChatHistoryResponse;
import tr.com.huseyinaydin.message.service.ConversationMessage;
import tr.com.huseyinaydin.message.web.ChatApiController;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatApiControllerTest {

    @Autowired
    private ChatApiController chatApiController;

    @Autowired
    private AppUserRepository userRepository;

    private AppUser user1;
    private AppUser user2;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(AppUser.create("Ahmet", "Yılmaz", "ahmet_chat@test.com", "hash123"));
        user2 = userRepository.save(AppUser.create("Mehmet", "Demir", "mehmet_chat@test.com", "hash123"));
        var auth = new UsernamePasswordAuthenticationToken(user1.getEmail(), null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldReturnEmptyChatHistoryInitially() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var response = chatApiController.getChatHistory(user2.getId(), auth);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        var body = (ChatHistoryResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.friendId()).isEqualTo(user2.getId());
        assertThat(body.friendName()).isEqualTo("Mehmet Demir");
        assertThat(body.messages()).isEmpty();
    }

    @Test
    void shouldSendMessageAndReceiveInHistory() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var sendResponse = chatApiController.sendMessage(
                user2.getId(),
                "Merhaba nasılsın?",
                null,
                null,
                null,
                auth
        );

        assertThat(sendResponse.getStatusCode().is2xxSuccessful()).isTrue();
        var sentMsg = (ConversationMessage) sendResponse.getBody();
        assertThat(sentMsg).isNotNull();
        assertThat(sentMsg.body()).contains("Merhaba nasılsın?");
        assertThat(sentMsg.sentByCurrentUser()).isTrue();

        var historyResponse = chatApiController.getChatHistory(user2.getId(), auth);
        var history = (ChatHistoryResponse) historyResponse.getBody();
        assertThat(history.messages()).hasSize(1);
        assertThat(history.messages().get(0).body()).contains("Merhaba nasılsın?");
        assertThat(history.messages().get(0).read()).isFalse();
    }

    @Test
    void shouldTrackTypingState() {
        var authUser1 = SecurityContextHolder.getContext().getAuthentication();
        var authUser2 = new UsernamePasswordAuthenticationToken(user2.getEmail(), null, Collections.emptyList());

        // User2 types to User1
        chatApiController.setTyping(user1.getId(), true, authUser2);

        // User1 checks chat history with User2 -> peer should be typing
        var historyResponse = chatApiController.getChatHistory(user2.getId(), authUser1);
        var history = (ChatHistoryResponse) historyResponse.getBody();
        assertThat(history.isPeerTyping()).isTrue();

        // User2 stops typing
        chatApiController.setTyping(user1.getId(), false, authUser2);
        historyResponse = chatApiController.getChatHistory(user2.getId(), authUser1);
        history = (ChatHistoryResponse) historyResponse.getBody();
        assertThat(history.isPeerTyping()).isFalse();
    }

    @Test
    void shouldMarkMessagesAsRead() {
        var authUser1 = SecurityContextHolder.getContext().getAuthentication();
        var authUser2 = new UsernamePasswordAuthenticationToken(user2.getEmail(), null, Collections.emptyList());

        // User1 sends message to User2
        var sendResponse = chatApiController.sendMessage(user2.getId(), "Test mesajı", null, null, null, authUser1);
        var sentMsg = (ConversationMessage) sendResponse.getBody();
        assertThat(sentMsg.read()).isFalse();

        // User2 marks conversation with User1 as read
        chatApiController.markAsRead(user1.getId(), authUser2);

        // User1 checks history -> message is now read (Görüldü)
        var historyResponse = chatApiController.getChatHistory(user2.getId(), authUser1);
        var history = (ChatHistoryResponse) historyResponse.getBody();
        assertThat(history.messages().get(0).read()).isTrue();
    }
}

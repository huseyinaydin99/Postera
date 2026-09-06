package tr.com.huseyinaydin.message.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import tr.com.huseyinaydin.message.service.MessageService;
import tr.com.huseyinaydin.message.service.MessageService.AttachmentDownload;

import static org.assertj.core.api.Assertions.assertThat;

class MessageControllerTest {

    private static class StubMessageService extends MessageService {
        private AttachmentDownload downloadToReturn;

        public StubMessageService() {
            super(null, null, null, null, null, null);
        }

        public void setDownloadToReturn(AttachmentDownload downloadToReturn) {
            this.downloadToReturn = downloadToReturn;
        }

        @Override
        public AttachmentDownload getAttachmentResource(String currentUserEmail, Long messageId, Long attachmentId) {
            return downloadToReturn;
        }
    }

    private StubMessageService messageService;
    private MessageController messageController;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        messageService = new StubMessageService();
        messageController = new MessageController(messageService, null, null);
        authentication = new UsernamePasswordAuthenticationToken("user@example.com", "pass");
    }

    @Test
    void shouldDownloadAttachmentSuccessfully() {
        var bytes = "sample pdf content".getBytes();
        var resource = new ByteArrayResource(bytes);
        var download = new AttachmentDownload(resource, "belge.pdf", "application/pdf");
        messageService.setDownloadToReturn(download);

        ResponseEntity<?> response = messageController.downloadAttachment(10L, 5L, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment")
                .contains("belge.pdf");
        assertThat(response.getBody()).isEqualTo(resource);
    }

    @Test
    void shouldDownloadAttachmentWithFallbackContentTypeWhenNull() {
        var bytes = "raw data".getBytes();
        var resource = new ByteArrayResource(bytes);
        var download = new AttachmentDownload(resource, "data.bin", null);
        messageService.setDownloadToReturn(download);

        ResponseEntity<?> response = messageController.downloadAttachment(20L, 8L, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment")
                .contains("data.bin");
    }
}

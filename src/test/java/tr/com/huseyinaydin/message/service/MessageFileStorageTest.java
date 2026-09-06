package tr.com.huseyinaydin.message.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import tr.com.huseyinaydin.config.UploadProperties;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageFileStorageTest {

    @TempDir
    Path tempDir;

    private MessageFileStorage messageFileStorage;

    @BeforeEach
    void setUp() {
        UploadProperties uploadProperties = new UploadProperties(
                tempDir.resolve("profiles").toString(),
                tempDir.resolve("messages").toString()
        );
        messageFileStorage = new MessageFileStorage(uploadProperties);
    }

    @Test
    void shouldStoreFileWithCustomAlias() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "hello world content".getBytes()
        );

        var result = messageFileStorage.store(file, "Rapor");

        assertThat(result.fileName()).endsWith(".pdf");
        assertThat(result.fileName()).isNotEqualTo("test-document.pdf");
        assertThat(result.originalName()).isEqualTo("test-document.pdf");
        assertThat(result.alias()).isEqualTo("Rapor.pdf");
        assertThat(result.fileSize()).isEqualTo("hello world content".getBytes().length);
        assertThat(result.contentType()).isEqualTo("application/pdf");

        var resource = messageFileStorage.loadAsResource(result.fileName());
        assertThat(resource.exists()).isTrue();
        assertThat(resource.contentLength()).isEqualTo("hello world content".getBytes().length);
    }

    @Test
    void shouldStoreFileWithDefaultAliasWhenAliasEmpty() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sunum.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "word data".getBytes()
        );

        var result = messageFileStorage.store(file, "");

        assertThat(result.fileName()).endsWith(".docx");
        assertThat(result.originalName()).isEqualTo("sunum.docx");
        assertThat(result.alias()).isEqualTo("sunum.docx");
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> messageFileStorage.store(file, "bos"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boş olamaz");
    }

    @Test
    void shouldRejectFileExceeding25MB() {
        MockMultipartFile file = new MockMultipartFile("file", "huge.zip", "application/zip", new byte[10]) {
            @Override
            public long getSize() {
                return 26L * 1024 * 1024;
            }
        };

        assertThatThrownBy(() -> messageFileStorage.store(file, "buyuk"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("25 MB");
    }
}

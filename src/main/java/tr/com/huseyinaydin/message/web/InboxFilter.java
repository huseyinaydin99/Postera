package tr.com.huseyinaydin.message.web;

import java.time.LocalDate;

public record InboxFilter(String sender, String subject, LocalDate from, LocalDate to, Long categoryId,
                          Boolean read, Boolean important, String sort, int page) {
    public boolean ascending() {
        return "asc".equalsIgnoreCase(sort);
    }
}

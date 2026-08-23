package tr.com.huseyinaydin.message.web;

import java.time.LocalDate;

public record InboxFilter(String sender, String subject, LocalDate from, LocalDate to, Long categoryId,
                          Boolean read, Boolean important, String sort, Integer page) {
    public boolean ascending() {
        return "asc".equalsIgnoreCase(sort);
    }

    public int pageNumber() {
        return page == null || page < 0 ? 0 : page;
    }

    public static InboxFilter empty() {
        return new InboxFilter(null, null, null, null, null, null, null, "desc", 0);
    }
}

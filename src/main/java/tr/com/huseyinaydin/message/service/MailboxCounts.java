package tr.com.huseyinaydin.message.service;

public record MailboxCounts(long inboxTotal, long inboxUnread, long drafts, long sent, long important) {
}

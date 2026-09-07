package tr.com.huseyinaydin.message.service;

import java.util.List;

public record ConversationLiveResponse(
        Long counterpartId,
        String counterpartName,
        boolean isPeerTyping,
        List<ConversationMessage> messages
) {}

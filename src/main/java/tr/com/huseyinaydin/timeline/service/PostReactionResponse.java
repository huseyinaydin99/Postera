package tr.com.huseyinaydin.timeline.service;

import java.util.List;

public record PostReactionResponse(List<TimelineReactionSummary> reactions, String currentUserReaction) {
}

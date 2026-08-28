package tr.com.huseyinaydin.timeline.domain;

public enum TimelineReactionType {
    LIKE("👍", "Beğen"),
    LAUGH("😂", "Gülme"),
    ANGRY("😠", "Kızgınlık"),
    SURPRISED("😮", "Şaşkınlık"),
    SUPPORT("🤝", "Yanındayım"),
    HEART("❤️", "Kalp");

    private final String emoji;
    private final String label;

    TimelineReactionType(String emoji, String label) {
        this.emoji = emoji;
        this.label = label;
    }

    public String emoji() { return emoji; }
    public String label() { return label; }
}

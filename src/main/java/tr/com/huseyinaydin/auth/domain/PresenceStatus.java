package tr.com.huseyinaydin.auth.domain;

public enum PresenceStatus {
    AVAILABLE("Müsait"),
    INVISIBLE("Offline Görünür"),
    EATING("Yemekte"),
    SLEEPING("Uyuyor"),
    BUSY("İşi var"),
    RESTROOM("Çişi var"),
    UNAVAILABLE("Müsait değil");

    private final String label;

    PresenceStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

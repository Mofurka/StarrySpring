package irden.space.proxy.protocol.payload.packet.chat.consts;

public enum ChatSentMode {
    UNIVERSE(0),
    LOCAL(1),
    PARTY(2);

    private final int id;

    ChatSentMode(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

        public static ChatSentMode fromId(int id) {
            for (ChatSentMode mode : values()) {
                if (mode.id == id) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Invalid ChatSentMode id: " + id);
        }
}

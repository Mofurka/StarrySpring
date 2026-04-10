package irden.space.proxy.protocol.payload.packet.chat.consts;

public enum ChatReceiveMode {
    LOCAL(0),
    PARTY(1),
    BROADCAST(2),
    WHISPER(3),
    COMMAND_RESULT(4),
    RADIO_MESSAGE(5),
    WORLD(6);

    private final int id;

    ChatReceiveMode(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static ChatReceiveMode fromId(int id) {
        for (ChatReceiveMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown ChatReceiveMode id: " + id);
    }

}

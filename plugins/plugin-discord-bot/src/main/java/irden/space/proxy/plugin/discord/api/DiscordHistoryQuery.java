package irden.space.proxy.plugin.discord.api;


public record DiscordHistoryQuery(
        Anchor anchor,
        long anchorMessageId,
        int limit,
        boolean oldestFirst
) {


    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_LIMIT = 1000;

    public DiscordHistoryQuery {
        if (anchor == null) {
            throw new IllegalArgumentException("History anchor must not be null");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("History limit must be within 1.." + MAX_LIMIT + ": " + limit);
        }
        if (anchor.needsMessageId() && anchorMessageId <= 0) {
            throw new IllegalArgumentException("History anchor " + anchor + " requires a message id");
        }
        if (anchor == Anchor.AROUND && limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "History around a message is limited to " + MAX_PAGE_SIZE + " messages: " + limit);
        }
    }

    /**
     * Последние сообщения канала.
     */
    public static DiscordHistoryQuery latest(int limit) {
        return new DiscordHistoryQuery(Anchor.LATEST, 0, limit, false);
    }

    /**
     * Сообщения старше указанного, от него в прошлое.
     */
    public static DiscordHistoryQuery before(long messageId, int limit) {
        return new DiscordHistoryQuery(Anchor.BEFORE, messageId, limit, false);
    }

    /**
     * Сообщения новее указанного - то, что нужно, чтобы дочитать канал с места прошлого переноса.
     */
    public static DiscordHistoryQuery after(long messageId, int limit) {
        return new DiscordHistoryQuery(Anchor.AFTER, messageId, limit, true);
    }

    /**
     * Сообщения вокруг указанного, не больше {@value #MAX_PAGE_SIZE}.
     */
    public static DiscordHistoryQuery around(long messageId, int limit) {
        return new DiscordHistoryQuery(Anchor.AROUND, messageId, limit, false);
    }

    /**
     * Самые старые сообщения канала.
     */
    public static DiscordHistoryQuery fromBeginning(int limit) {
        return new DiscordHistoryQuery(Anchor.BEGINNING, 0, limit, true);
    }

    /**
     * Отдать результат в хронологическом порядке, от старых к свежим.
     */
    public DiscordHistoryQuery withOldestFirst() {
        return withOldestFirst(true);
    }

    public DiscordHistoryQuery withOldestFirst(boolean oldestFirst) {
        return new DiscordHistoryQuery(anchor, anchorMessageId, limit, oldestFirst);
    }

    public DiscordHistoryQuery withLimit(int limit) {
        return new DiscordHistoryQuery(anchor, anchorMessageId, limit, oldestFirst);
    }

    public enum Anchor {

        /**
         * От самого свежего сообщения канала назад.
         */
        LATEST,
        /**
         * От указанного сообщения назад.
         */
        BEFORE,
        /**
         * От указанного сообщения вперёд.
         */
        AFTER,
        /**
         * Вокруг указанного сообщения.
         */
        AROUND,
        /**
         * От первого сообщения канала вперёд.
         */
        BEGINNING;

        public boolean needsMessageId() {
            return this == BEFORE || this == AFTER || this == AROUND;
        }
    }
}

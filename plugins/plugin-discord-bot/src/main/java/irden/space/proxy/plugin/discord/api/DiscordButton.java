package irden.space.proxy.plugin.discord.api;

import lombok.Builder;

@Builder(toBuilder = true)
public record DiscordButton(
        String id,
        String label,
        DiscordButtonStyle style,
        String emoji,
        String url,
        boolean disabled
) {

    public static final int ID_MAX_LENGTH = 100;
    public static final int LABEL_MAX_LENGTH = 80;

    public DiscordButton {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Button label must not be blank");
        }
        if (label.length() > LABEL_MAX_LENGTH) {
            throw new IllegalArgumentException("Button label is longer than " + LABEL_MAX_LENGTH + ": " + label);
        }

        if (url != null && !url.isBlank()) {
            style = null;
            id = null;
        } else {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Button id must not be blank");
            }
            if (id.length() > ID_MAX_LENGTH) {
                throw new IllegalArgumentException("Button id is longer than " + ID_MAX_LENGTH + ": " + id);
            }
            style = style == null ? DiscordButtonStyle.SECONDARY : style;
        }
    }

    public static DiscordButton of(String id, String label) {
        return build(id, label, DiscordButtonStyle.SECONDARY);
    }

    public static DiscordButton primary(String id, String label) {
        return build(id, label, DiscordButtonStyle.PRIMARY);
    }

    public static DiscordButton success(String id, String label) {
        return build(id, label, DiscordButtonStyle.SUCCESS);
    }

    public static DiscordButton danger(String id, String label) {
        return build(id, label, DiscordButtonStyle.DANGER);
    }

    public static DiscordButton link(String url, String label) {
        return DiscordButton.builder().url(url).label(label).build();
    }

    private static DiscordButton build(String id, String label, DiscordButtonStyle style) {
        return DiscordButton.builder().id(id).label(label).style(style).build();
    }

    public boolean isLink() {
        return url != null && !url.isBlank();
    }
}

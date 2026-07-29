package irden.space.proxy.plugin.star_custom_chat.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import irden.space.proxy.plugin.command_handler.color.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ChatMode {

    PROXIMITY(
            "Proximity",
            "[" + Color.GREEN.colorString("L") + "]"
    ),

    FIGHT(
            "Fight",
            "[" + Color.RED.colorString("F") + "]"
    ),

    WHISPER(
            "Whisper",
            "[" + Color.MAGENTA.colorString("W") + "]"
    );

    private final String mode;
    private final String prefix;

    @JsonCreator
    public static ChatMode fromJson(String value) {
        return Arrays.stream(values())
                .filter(mode -> mode.mode.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Неизвестный режим чата: " + value
                        )
                );
    }

    @JsonValue
    public String toJson() {
        return mode;
    }
}
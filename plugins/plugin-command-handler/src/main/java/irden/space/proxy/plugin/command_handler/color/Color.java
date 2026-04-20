package irden.space.proxy.plugin.command_handler.color;

import irden.space.proxy.protocol.payload.common.rgba.Rgba;

import java.util.regex.Pattern;

public enum Color {
    RED("red", new Rgba(255, 73, 66, 255)),
    ORANGE("orange", new Rgba(255, 180, 47, 255)),
    YELLOW("yellow", new Rgba(255, 239, 30, 255)),
    GREEN("green", new Rgba(79, 230, 70, 255)),
    BLUE("blue", new Rgba(38, 96, 255, 255)),
    INDIGO("indigo", new Rgba(75, 0, 130, 255)),
    VIOLET("violet", new Rgba(160, 119, 255, 255)),
    BLACK("black", new Rgba(0, 0, 0, 255)),
    WHITE("white", new Rgba(255, 255, 255, 255)),
    MAGENTA("magenta", new Rgba(221, 92, 249, 255)),
    DARKMAGENTA("darkmagenta", new Rgba(142, 33, 144, 255)),
    CYAN("cyan", new Rgba(0, 220, 233, 255)),
    DARKCYAN("darkcyan", new Rgba(0, 137, 165, 255)),
    CORNFLOWERBLUE("cornflowerblue", new Rgba(100, 149, 237, 255)),
    GRAY("gray", new Rgba(160, 160, 160, 255)),
    LIGHTGRAY("lightgray", new Rgba(192, 192, 192, 255)),
    DARKGRAY("darkgray", new Rgba(128, 128, 128, 255)),
    DARKGREEN("darkgreen", new Rgba(0, 128, 0, 255)),
    PINK("pink", new Rgba(255, 162, 187, 255)),
    CLEAR("clear", new Rgba(0, 0, 0, 0));

    private final String color;
    private final Rgba rgba;

    Color(String color, Rgba rgba) {
        this.color = color;
        this.rgba = rgba;
    }

    public String color() {
        return color;
    }

    public Rgba rgba() {
        return rgba;
    }

    public static Color fromString(String color) {
        for (Color c : Color.values()) {
            if (c.color.equalsIgnoreCase(color)) {
                return c;
            }
        }
        return null;
    }

    public String colorString(String text, boolean clear) {
        String formatted = "^".concat(color).concat(";").concat(text);
        if (clear) {
            formatted = formatted.concat("^reset;");
        }
        return formatted;
    }

    public static String colorString(Color color, String text, boolean clear) {
        if (color == null) {
            return text;
        }
        return color.colorString(text, clear);
    }

    public static String stripColorCodes(String text) {
        return text.replaceAll("\\^[a-zA-Z]+;", "");
    }

}

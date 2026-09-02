package irden.space.proxy.plugin.utils.messages;

import java.util.Locale;

public interface MessageUtils {
    String get(String code, Object... args);

    String get(String code, Locale locale, Object... args);
}

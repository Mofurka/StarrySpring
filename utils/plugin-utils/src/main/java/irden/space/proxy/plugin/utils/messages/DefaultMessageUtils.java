package irden.space.proxy.plugin.utils.messages;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;

import java.util.Locale;

@RequiredArgsConstructor
public class DefaultMessageUtils implements MessageUtils {
    private final MessageSource messageSource;

    @Override
    public String get(String code, Object... args) {
        return messageSource.getMessage(code, args, Locale.getDefault());
    }

    @Override
    public String get(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, locale);
    }

}
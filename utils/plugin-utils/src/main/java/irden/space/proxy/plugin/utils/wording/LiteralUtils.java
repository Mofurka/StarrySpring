package irden.space.proxy.plugin.utils.wording;

import lombok.experimental.UtilityClass;

import java.util.Locale;

@UtilityClass
public class LiteralUtils {

    public static String declineWord(
            int count,
            Locale locale,
            String form1,
            String form2,
            String form5
    ) {
        if (locale.getLanguage().equals("ru")) {
            return declineRussian(count, form1, form2, form5);
        }

        return count == 1 ? form1 : form2;
    }

    public static String declineRussian(int count, String form1, String form2, String form5) {
        count = Math.abs(count) % 100;

        // Исключение для чисел от 11 до 19 (всегда используется форма "яблок")
        if (count > 10 && count < 20) {
            return form5;
        }

        int lastDigit = count % 10;
        if (lastDigit == 1) {
            return form1; // 1 яблоко // 1 apple
        }
        if (lastDigit > 1 && lastDigit < 5) {
            return form2; // 2 яблока // 2 apples
        }

        return form5; // 5 яблок // 5 apples
    }

}

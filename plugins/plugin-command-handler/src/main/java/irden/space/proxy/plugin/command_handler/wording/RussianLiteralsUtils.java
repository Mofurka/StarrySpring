package irden.space.proxy.plugin.command_handler.wording;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RussianLiteralsUtils {

    public static String declineWord(int count, String form1, String form2, String form5) {
        count = Math.abs(count) % 100;

        // Исключение для чисел от 11 до 19 (всегда используется форма "яблок")
        if (count > 10 && count < 20) {
            return form5;
        }

        int lastDigit = count % 10;
        if (lastDigit == 1) {
            return form1; // 1 яблоко
        }
        if (lastDigit > 1 && lastDigit < 5) {
            return form2; // 2 яблока
        }

        return form5; // 5 яблок
    }

}

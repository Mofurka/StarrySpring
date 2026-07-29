package irden.space.proxy.plugin.irden.d20;

import irden.space.proxy.plugin.irden.d20.constants.CharacterStats;
import irden.space.proxy.plugin.irden.d20.constants.ColorCode;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;

import java.util.List;
import java.util.Map;


public final class RollFormatter {

    private static final String DEFAULT_COLOR = "yellow";


    private RollFormatter() {
    }

    public static BonusResult combine(int rn,
                                      List<VariantValue> bonuses,
                                      boolean ignoreCrit,
                                      boolean onlySum,
                                      boolean weatherEffects,
                                      String color,
                                      int minCrit,
                                      Map<String, Integer> weatherStats) {
        StringBuilder res = new StringBuilder();
        boolean crit = false;

        if (!ignoreCrit) {
            if (rn >= minCrit) {
                crit = true;
                res.append(" ^green;КРИТ УСПЕХ!^reset;");
            } else if (rn == 1) {
                crit = true;
                res.append(" ^red;КРИТ ПРОВАЛ!^reset;");
            }
        }

        if (bonuses.isEmpty()) {
            return new BonusResult("", 0);
        }

        // Если показываем только сумму и был крит - оставляем лишь строку крита без разбивки бонусов.
        if (onlySum && crit) {
            return new BonusResult(res.toString(), 0);
        }

        int sum = rn;
        // В Python это сырой литерал "^clear;{{^reset;" (без .format), поэтому скобки двойные.
        res.append("^clear;{{^reset;");
        for (int i = 0; i + 1 < bonuses.size(); i += 2) {
            int value = intOf(bonuses.get(i));
            String desc = Variants.asString(bonuses.get(i + 1)).orElse("");

            if (!onlySum) {
                res.append(formatBonus(value, desc));
            }

            sum += value;

            // Погодные эффекты: если описание бонуса - характеристика, и на неё есть погодный модификатор.
            if (weatherEffects && !weatherStats.isEmpty() && isStat(desc)) {
                Integer weatherValue = weatherStats.get(desc);
                if (weatherValue != null) {
                    res.append(formatBonus(weatherValue, "Погода: " + desc));
                    sum += weatherValue;
                }
            }
        }
        res.append("^clear;}}^reset;");

        String effectiveColor = color == null ? "None" : color;
        if (onlySum) {
            return new BonusResult(" = ^" + effectiveColor + ";" + sum + "^reset;", sum);
        }

        res.append(" = ^").append(effectiveColor).append(";").append(sum).append("^reset;");
        return new BonusResult(res.toString(), sum);
    }

    /**
     * Перегрузка с параметрами по умолчанию (color=yellow, minCrit=20), как в Python.
     */
    public static BonusResult combine(int rn,
                                      List<VariantValue> bonuses,
                                      boolean ignoreCrit,
                                      boolean onlySum,
                                      boolean weatherEffects,
                                      Map<String, Integer> weatherStats) {
        return combine(rn, bonuses, ignoreCrit, onlySum, weatherEffects, DEFAULT_COLOR, 20, weatherStats);
    }

    private static String formatBonus(int value, String desc) {
        String sign = value >= 0 ? "+" : "";
        return " " + sign + value + ColorCode.GRAY + "[" + desc + "]" + ColorCode.RESET;
    }

    private static int intOf(VariantValue value) {
        return Variants.asInt(value)
                .orElseGet(() -> Variants.asDouble(value).map(Double::intValue).orElse(0));
    }

    private static boolean isStat(String desc) {
        for (CharacterStats stat : CharacterStats.values()) {
            if (stat.name().equals(desc)) {
                return true;
            }
        }
        return false;
    }

    public record BonusResult(String text, int sum) {
    }
}

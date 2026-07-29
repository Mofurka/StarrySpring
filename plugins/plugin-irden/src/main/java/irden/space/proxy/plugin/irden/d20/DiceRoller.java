package irden.space.proxy.plugin.irden.d20;

import irden.space.proxy.plugin.irden.d20.constants.ColorCode;

import java.util.concurrent.ThreadLocalRandom;


public final class DiceRoller {


    private DiceRoller() {
    }

    public static AdvantageResult calculate(Boolean advantage, int dice) {
        int rn = ThreadLocalRandom.current().nextInt(1, dice + 1);
        int rn2 = ThreadLocalRandom.current().nextInt(1, dice + 1);

        if (advantage == null) {
            // Ни преимущества, ни помехи - одиночный бросок.
            return new AdvantageResult(rn, Integer.toString(rn));
        }

        int chosen;
        int other;
        if (advantage) {
            // Преимущество: выбираем больший из двух бросков.
            chosen = Math.max(rn, rn2);
            other = Math.min(rn, rn2);
        } else {
            // Помеха: выбираем меньший из двух бросков.
            chosen = Math.min(rn, rn2);
            other = Math.max(rn, rn2);
        }

        String display = ColorCode.DICE_GRAY + other + " | " + ColorCode.RESET + chosen;
        return new AdvantageResult(chosen, display);
    }

    public record AdvantageResult(int value, String display) {
    }
}

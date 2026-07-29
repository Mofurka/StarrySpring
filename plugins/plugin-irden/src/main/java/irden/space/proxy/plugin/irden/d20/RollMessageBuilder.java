package irden.space.proxy.plugin.irden.d20;

import irden.space.proxy.plugin.irden.d20.DiceRoller.AdvantageResult;
import irden.space.proxy.plugin.irden.d20.RollFormatter.BonusResult;
import irden.space.proxy.plugin.irden.d20.constants.ColorCode;
import irden.space.proxy.plugin.irden.d20.constants.MessageTemplate;
import irden.space.proxy.plugin.irden.d20.initiative.IrdenFightHandler;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class RollMessageBuilder {

    private final ResourceRewardService resourceRewardService;
    private final WeatherStatsProvider weatherStats;
    private final IrdenFightHandler fightHandler;

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void chop2(StringBuilder sb) {
        if (sb.length() >= 2) {
            sb.setLength(sb.length() - 2);
        }
    }

    public String actionRoll(StatManagerMessage info) {
        RollContext ctx = RollContext.of(info);
        StringBuilder res = new StringBuilder(ctx.actorPrefix());
        res.append(MessageTemplate.PERFORMS_ACTION.replace("{action}", safe(info.action())));

        if (info.hasTarget()) {
            res.append(MessageTemplate.AGAINST);
            if (info.targetIsList()) {
                List<String> names = info.targetNames();
                for (int i = 0; i < names.size(); i++) {
                    if (i > 0) {
                        res.append(", ");
                    }
                    res.append(MessageTemplate.TARGET_NAME.replace("{name}", names.get(i)));
                }
            } else {
                res.append(MessageTemplate.TARGET_NAME.replace("{name}", safe(info.targetName())));
            }
        }

        if (info.defenseAction() != null && !info.defenseAction().isEmpty()) {
            res.append(MessageTemplate.DEFENSE_ACTION.replace("{defense}", info.defenseAction()));
        }

        AdvantageResult roll = DiceRoller.calculate(ctx.advantage(), info.dice());
        if (!ctx.onlySumFlag()) {
            res.append(": ").append(roll.display());
        }

        res.append(combine(roll.value(), info.bonuses(), false, ctx).text());

        List<VariantValue> damageBonuses = info.damageBonuses();
        if (!damageBonuses.isEmpty()) {
            res.append("\n").append(ctx.fightName()).append(MessageTemplate.DAMAGE);
            res.append(RollFormatter.combine(
                    0, damageBonuses, false, ctx.onlySumFlag(),
                    ctx.weatherEffects(), "red", 20, weatherStats.current()
            ).text());
        }

        if (info.richDescription() != null && !info.richDescription().isEmpty()) {
            res.append("\n").append(ctx.fightName());
            res.append(MessageTemplate.DESCRIPTION.replace("{description}", info.richDescription()));
        }

        return res.toString();
    }

    public String statRoll(StatManagerMessage info) {
        RollContext ctx = RollContext.of(info);
        StringBuilder res = new StringBuilder(ctx.actorPrefix());
        res.append(MessageTemplate.PERFORMS_CHECK.replace("{action}", safe(info.action())));

        AdvantageResult roll = DiceRoller.calculate(ctx.advantage(), info.dice());
        if (!ctx.onlySumFlag()) {
            res.append(": ").append(roll.display());
        }

        res.append(combine(roll.value(), info.bonuses(), false, ctx).text());
        return res.toString();
    }

    public String diceRoll(StatManagerMessage info) {
        RollContext ctx = RollContext.of(info);
        StringBuilder res = new StringBuilder(ctx.actorPrefix());

        if (info.action() != null && !info.action().isEmpty()) {
            res.append(MessageTemplate.CHECK_ACTION
                    .replace("{action}", info.action())
                    .replace("{dice}", Integer.toString(info.dice())));
        } else {
            res.append(MessageTemplate.THROWS_DICE.replace("{dice}", Integer.toString(info.dice())));
        }

        AdvantageResult roll = DiceRoller.calculate(ctx.advantage(), info.dice());
        res.append(": ").append(roll.display());
        return res.toString();
    }

    public String initiative(StatManagerMessage info) {
        RollContext ctx = RollContext.of(info);
        StringBuilder res = new StringBuilder(ctx.actorPrefix());
        res.append(MessageTemplate.THROWS_INITIATIVE);

        if (info.fightName() != null && !info.fightName().isEmpty()) {
            res.append(MessageTemplate.INITIATIVE_WITH_FIGHT.replace("{fight_name}", info.fightName()));
        }
        var roll = DiceRoller.calculate(ctx.advantage(), info.dice());
        res.append(": ").append(roll.value());


        res.append(combine(roll.value(), info.bonuses(), true, ctx).text());
        fightHandler.startFight(info.fightName(), info.player(), roll.value(), info.fightEntityType());
        return res.toString();
    }

    public String resourceEvent(StatManagerMessage info) {
        RollContext ctx = RollContext.of(info);
        StringBuilder res = new StringBuilder(ctx.actorPrefix());
        res.append(MessageTemplate.CONDUCTS_ACTION.replace("{action}", safe(info.action())));

        Map<String, VariantValue> data = info.data();
        String event = Variants.asString(data.get("event")).orElse(null);
        String onCrit = Variants.asString(data.get("onCrit")).orElse(null);
        int minCrit = info.minCrit();

        if (info.attempts() > 1) {
            res.append(multipleResourceAttempts(info, ctx, event, onCrit, minCrit));
        } else {
            res.append(singleResourceAttempt(info, ctx, event, onCrit, minCrit));
        }

        return res.toString();
    }

    private String multipleResourceAttempts(StatManagerMessage info, RollContext ctx,
                                            String event, String onCrit, int minCrit) {
        StringBuilder res = new StringBuilder();
        int attempts = info.attempts();
        int[] rannums = new int[attempts];
        int[] values = new int[attempts];

        for (int i = 0; i < attempts; i++) {
            AdvantageResult roll = DiceRoller.calculate(ctx.advantage(), 20);
            res.append(roll.display());

            BonusResult bonus = combine(roll.value(), info.bonuses(), false, ctx, null, minCrit);

            res.append(ColorCode.RESET).append("[").append(ColorCode.GRAY)
                    .append(bonus.sum()).append(ColorCode.RESET).append("], ");
            values[i] = bonus.sum();
            rannums[i] = roll.value();
        }

        chop2(res);
        res.append(MessageTemplate.RECEIVES);

        for (int i = 0; i < attempts; i++) {
            if (rannums[i] != 1) {
                String reward = resourceRewardService.getReward(rannums[i], values[i], event, onCrit, minCrit);
                if (reward != null) {
                    res.append(reward).append(", ");
                }
            }
        }

        chop2(res);
        res.append("!");
        return res.toString();
    }

    private String singleResourceAttempt(StatManagerMessage info, RollContext ctx,
                                         String event, String onCrit, int minCrit) {
        AdvantageResult roll = DiceRoller.calculate(ctx.advantage(), 20);
        StringBuilder res = new StringBuilder(roll.display());

        BonusResult bonus = combine(roll.value(), info.bonuses(), false, ctx, null, minCrit);
        res.append(bonus.text());

        if (roll.value() != 1) {
            String reward = resourceRewardService.getReward(roll.value(), bonus.sum(), event, onCrit, minCrit);
            if (reward != null) {
                res.append(MessageTemplate.RECEIVES).append(reward).append("!");
            }
        }

        return res.toString();
    }

    private BonusResult combine(int rn, List<VariantValue> bonuses, boolean ignoreCrit, RollContext ctx) {
        return RollFormatter.combine(
                rn, bonuses, ignoreCrit, ctx.onlySumFlag(), ctx.weatherEffects(), weatherStats.current());
    }

    private BonusResult combine(int rn, List<VariantValue> bonuses, boolean ignoreCrit, RollContext ctx,
                                String color, int minCrit) {
        return RollFormatter.combine(
                rn, bonuses, ignoreCrit, ctx.onlySumFlag(), ctx.weatherEffects(), color, minCrit,
                weatherStats.current());
    }
}

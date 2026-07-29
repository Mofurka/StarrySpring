package irden.space.proxy.plugin.irden.d20;

import irden.space.proxy.plugin.irden.d20.constants.MessageTemplate;
import irden.space.proxy.plugin.irden.d20.constants.Placeholders;


record RollContext(
        String fightName,
        Boolean onlySum,
        Boolean advantage,
        boolean weatherEffects
) {

    static RollContext of(StatManagerMessage info) {
        String fight = info.fight();
        String fightName = (fight == null || fight.isEmpty())
                ? ""
                : MessageTemplate.FIGHT_NAME.replace("{name}", fight);
        return new RollContext(
                fightName,
                oneMinusOne(info.onlySum()),
                oneMinusOne(info.advantage()),
                info.weatherEffects()
        );
    }

    private static Boolean oneMinusOne(Integer value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case 1 -> Boolean.TRUE;
            case -1 -> Boolean.FALSE;
            default -> null;
        };
    }

    String actorPrefix() {
        return fightName + Placeholders.PLAYER;
    }

    boolean onlySumFlag() {
        return Boolean.TRUE.equals(onlySum);
    }
}

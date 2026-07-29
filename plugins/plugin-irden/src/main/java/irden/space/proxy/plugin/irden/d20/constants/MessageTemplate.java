package irden.space.proxy.plugin.irden.d20.constants;


public final class MessageTemplate {

    public static final String UPDATE_VERSION = "^pink;Обнови мод до версии {version}.*!^reset;";
    public static final String PERFORMS_ACTION = " совершает ^orange;{action}^reset;";
    public static final String PERFORMS_CHECK = " совершает проверку ^orange;{action}^reset;";
    public static final String THROWS_DICE = " бросает ^magenta;d{dice}^reset;";
    public static final String CHECK_ACTION = "совершает проверку ^orange;{action}^reset; d{dice}";
    public static final String THROWS_INITIATIVE = " бросает ^cornflowerblue;Инициативу^reset;";
    public static final String RETURN_TO_FIGHT = "Вы вернулись в бой ^red;{fight_name}^reset; с инициативой {initiative}";
    public static final String CONDUCTS_ACTION = " проводит ^orange;{action}^reset;: ";
    public static final String AGAINST = " против ";
    public static final String DEFENSE_ACTION = "^gray;[{defense}]^reset;";
    public static final String DAMAGE = "^red;Урон^reset;: ";
    public static final String DESCRIPTION = "^gray;Описание^reset;: ^#C56D3E;{description}^reset;";
    public static final String RECEIVES = " и получает ";
    public static final String DEBT_MESSAGE = "^red;У вас долг! ({money} монет)^reset;";
    public static final String MONEY_MESSAGE = "^green;У вас {money} монет.^reset;";
    public static final String SELF_TRANSFER_ERROR = "^violet;Ты не можешь дать монетки самому себе, глупышка~ :P^reset;";
    public static final String INVALID_AMOUNT = "^red;Укажите корректное значение!^reset;";
    public static final String NOT_ENOUGH_MONEY = "^red;Недостаточно денег!^reset;";
    public static final String MONEY_RECEIVED = "^green;{sender} даёт вам {amount} монет!^reset;";
    public static final String MONEY_SENT = "^green;Вы отдаете {target} {amount} монет!^reset;";
    public static final String DISCORD_TRANSFER = "**{sender}** Передал **{target}** {amount} монет!";
    public static final String SILENT_PREFIX = "^#792F33;[Неслышно]^reset; ";
    public static final String LOCAL_PREFIX = "[^green;L^reset;] ";
    public static final String TARGET_NAME = "^magenta;{name}^reset;";
    public static final String FIGHT_NAME = "[^red;{name}^reset;] ";
    public static final String INITIATIVE_WITH_FIGHT = " (^red;{fight_name}^reset;)";

    private MessageTemplate() {
    }
}

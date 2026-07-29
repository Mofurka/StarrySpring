package irden.space.proxy.plugin.irden.d20;

import irden.space.proxy.plugin.irden.d20.initiative.model.FightEntityType;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.codec.variant.ListVariantValue;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StatManagerMessage {

    private final Map<String, VariantValue> raw;
    private Player player;

    private StatManagerMessage(Map<String, VariantValue> raw) {
        this.raw = raw;
    }

    /**
     * @return обёртку, если аргумент - это Map с полем {@code type}; иначе {@code null}.
     */
    public static StatManagerMessage from(VariantValue arg) {
        Map<String, VariantValue> map = Variants.asMap(arg).orElse(null);
        if (map == null || !map.containsKey("type")) {
            return null;
        }
        return new StatManagerMessage(map);
    }

    // ===== базовые поля =====

    public String type() {
        return string("type", null);
    }

    public String version() {
        return string("version", null);
    }

    public boolean silent() {
        return bool("silent", false);
    }

    /**
     * может быть {@code null} - тогда бросок уходит широковещательно.
     */
    public String rollMode() {
        return string("rollMode", null);
    }

    public String fight() {
        return string("fight", null);
    }

    /**
     * приходит как 1/-1 (реже 0). {@code null}, если поля нет.
     */
    public Integer onlySum() {
        return intOrNull("onlySum");
    }

    /**
     * приходит как 1/-1/0. {@code null}, если поля нет.
     */
    public Integer advantage() {
        return intOrNull("advantage");
    }

    public boolean weatherEffects() {
        return bool("weatherEffects", true);
    }

    public List<String> uniqueIds() {
        return stringList("uniqueIds");
    }

    public void player(Player player) {
        this.player = player;
    }

    public Player player() {
        return this.player;
    }

    public List<Integer> clientIds() {
        return intList("clientIds");
    }

    // ===== target (полиморфное поле) =====

    public VariantValue target() {
        return raw.get("target");
    }

    public boolean hasTarget() {
        VariantValue target = target();
        if (target == null) {
            return false;
        }
        if (target instanceof StringVariantValue(String value)) {
            return !value.isEmpty();
        }
        if (target instanceof ListVariantValue(VariantValue[] values)) {
            return values.length > 0;
        }
        return true;
    }

    public boolean targetIsList() {
        return target() instanceof ListVariantValue;
    }

    /**
     * имена целей из списка target ({@code [{"targetName": ...}, ...]}).
     */
    public List<String> targetNames() {
        List<String> result = new ArrayList<>();
        if (target() instanceof ListVariantValue(VariantValue[] values)) {
            for (VariantValue value : values) {
                Variants.get(value, "targetName")
                        .flatMap(Variants::asString)
                        .ifPresent(result::add);
            }
        }
        return result;
    }

    // ===== actionroll / statroll / diceroll =====

    public String action() {
        return string("action", null);
    }

    public int dice() {
        return intValue("dice", 20);
    }

    public List<VariantValue> bonuses() {
        return variantList("bonuses");
    }

    public String targetName() {
        return string("targetName", null);
    }

    public String defenseAction() {
        return string("defenseAction", null);
    }

    public List<VariantValue> damageBonuses() {
        return variantList("damageBonuses");
    }

    public String richDescription() {
        return string("richDescription", null);
    }

    // ===== initiative / return_to_fight =====

    public int initiative() {
        return intValue("initiative", 0);
    }

    public String fightName() {
        return string("fightName", null);
    }

    public FightEntityType fightEntityType() {
        return FightEntityType.valueOf(string("fightEntityType", FightEntityType.PLAYER.name()));
    }

    // ===== resourceEvent =====

    public Map<String, VariantValue> data() {
        return Variants.asMap(raw.get("data")).orElse(Map.of());
    }

    public int attempts() {
        return intValue("attempts", 1);
    }

    public int minCrit() {
        return intValue("minCrit", 20);
    }

    // ===== transferMoney =====

    public String amount() {
        return string("amount", null);
    }

    // ===== низкоуровневые аксессоры =====

    private String string(String key, String fallback) {
        return Variants.asString(raw.get(key)).orElse(fallback);
    }

    private boolean bool(String key, boolean fallback) {
        return Variants.asBoolean(raw.get(key)).orElse(fallback);
    }

    private int intValue(String key, int fallback) {
        Integer value = intOrNull(key);
        return value == null ? fallback : value;
    }

    private Integer intOrNull(String key) {
        VariantValue value = raw.get(key);
        return Variants.asInt(value)
                .orElseGet(() -> Variants.asDouble(value).map(Double::intValue).orElse(null));
    }

    private List<VariantValue> variantList(String key) {
        VariantValue[] values = Variants.asList(raw.get(key)).orElse(null);
        return values == null ? List.of() : List.of(values);
    }

    private List<String> stringList(String key) {
        List<String> result = new ArrayList<>();
        for (VariantValue value : variantList(key)) {
            Variants.asString(value).ifPresent(result::add);
        }
        return result;
    }

    private List<Integer> intList(String key) {
        List<Integer> result = new ArrayList<>();
        for (VariantValue value : variantList(key)) {
            Variants.asInt(value)
                    .or(() -> Variants.asDouble(value).map(Double::intValue))
                    .ifPresent(result::add);
        }
        return result;
    }
}

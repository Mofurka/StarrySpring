package irden.space.proxy.plugin.irden.d20;

import irden.space.proxy.plugin.irden.d20.constants.ColorCode;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Награды за ресурсные события (resourceEvent). Перенос Python {@code _get_resource_reward}
 * вместе с загрузкой {@code config/resources.json}.
 *
 * <p>Структура {@code resources.json}:
 * <ul>
 *     <li>ключи-события (например {@code "prey"}) - массив объектов {@code {range:[lo,hi], reward:[...]}};</li>
 *     <li>ключи-криты (например {@code "preyCrit"}) - массив строк-наград.</li>
 * </ul>
 */
@Service
public class ResourceRewardService {

    private static final String RESOURCES_LOCATION = "/d20/resources.json";

    private final JsonNode resources;

    public ResourceRewardService(JsonMapper objectMapper) {
        this.resources = load(objectMapper);
    }

    private static String randomFrom(JsonNode array) {
        if (array == null || !array.isArray() || array.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(array.size());
        return array.get(index).asString();
    }

    private JsonNode load(JsonMapper objectMapper) {
        try (InputStream input = getClass().getResourceAsStream(RESOURCES_LOCATION)) {
            if (input == null) {
                throw new IllegalStateException("Resources config not found on classpath: " + RESOURCES_LOCATION);
            }
            return objectMapper.readTree(input);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read resources config: " + RESOURCES_LOCATION, e);
        }
    }

    /**
     * @param rn        значение кубика (для проверки крита)
     * @param sumValue  итоговая сумма броска с бонусами (для выбора диапазона награды)
     * @param eventName ключ события из {@code data.event}
     * @param onCrit    ключ крит-таблицы из {@code data.onCrit} (может быть {@code null})
     * @param minCrit   порог крита
     * @return строку награды с цветом, либо {@code null}, если награды нет.
     */
    public String getReward(int rn, int sumValue, String eventName, String onCrit, int minCrit) {
        if (rn >= minCrit && onCrit != null) {
            String reward = randomFrom(resources.get(onCrit));
            if (reward != null) {
                return ColorCode.YELLOW + reward + ColorCode.RESET;
            }
        }

        JsonNode ranges = resources.get(eventName);
        if (ranges != null && ranges.isArray()) {
            for (JsonNode entry : ranges) {
                JsonNode range = entry.get("range");
                if (range == null || range.size() < 2) {
                    continue;
                }
                int low = range.get(0).asInt();
                int high = range.get(1).asInt();
                if (low <= sumValue && sumValue <= high) {
                    String reward = randomFrom(entry.get("reward"));
                    if (reward != null) {
                        return ColorCode.GREEN + reward + ColorCode.RESET;
                    }
                }
            }
        }

        return null;
    }
}

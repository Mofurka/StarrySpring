// Боевой конфиг подтягивается из репозитория на этапе сборки, а не хранится
// отдельной копией внутри HTML. Прежняя версия держала внутри страницы
// вручную вставленный снимок на 100 КБ, который разъехался бы с настоящим
// файлом при первой же правке конфига.
import rawDefaultConfig from "../../../../config/plugins/irden/weather/weather-system.json?raw";

export const DEFAULT_CONFIG_TEXT = rawDefaultConfig;

export function loadDefaultConfig(): unknown {
    return JSON.parse(rawDefaultConfig);
}

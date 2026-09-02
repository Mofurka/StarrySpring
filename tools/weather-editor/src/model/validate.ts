import {isValidStoredColor} from "./colors";
import {isEmptyConditions} from "./normalize";
import {hourCoverage} from "./periods";
import type {Conditions, TabId, Transition, WeatherModel} from "./types";

export interface Issue {
    tab: TabId;
    message: string;
}

export interface ValidationResult {
    errors: Issue[];
    warnings: Issue[];
}

/**
 * Ошибка = сервер либо не загрузит конфиг (WeatherConfigValidator кинет
 * исключение), либо упадёт в рантайме. Замечание = загрузится и будет
 * работать, но почти наверняка не так, как задумано.
 *
 * Набор ошибок держится в паритете с
 * plugins/plugin-irden/.../weather/WeatherConfigValidator.java — расхождения
 * этих двух списков и были источником «в редакторе зелено, на сервере падает».
 */
export function validate(model: WeatherModel): ValidationResult {
    const errors: Issue[] = [];
    const warnings: Issue[] = [];
    const err = (tab: TabId, message: string) => errors.push({tab, message});
    const warn = (tab: TabId, message: string) => warnings.push({tab, message});

    const stateIds = Object.keys(model.states);
    const periodIds = Object.keys(model.periods);
    const knownStates = new Set(stateIds);
    const knownPeriods = new Set(periodIds);
    const knownTags = new Set<string>();
    for (const state of Object.values(model.states)) {
        for (const tag of state.tags) knownTags.add(tag);
    }
    const knownAtmosphere = new Set([
        ...Object.keys(model.settings.atmosphereBounds),
        ...Object.keys(model.settings.initialAtmosphere),
    ]);

    /* ---------- настройки ---------- */

    if (stateIds.length === 0) err("states", "Не задано ни одного состояния");

    const defaultState = model.settings.defaultState;
    if (!defaultState) {
        err("settings", "Не задано состояние по умолчанию");
    } else if (!knownStates.has(defaultState)) {
        err("settings", `Состояние по умолчанию «${defaultState}» не существует`);
    }

    /* ---------- периоды ---------- */

    if (periodIds.length === 0) err("periods", "Не задано ни одного периода");

    for (const [id, period] of Object.entries(model.periods)) {
        if (!id.trim()) err("periods", "У периода пустой id");
        if (period.fromHour < 0 || period.fromHour > 23) {
            err("periods", `Период «${id}»: fromHour вне 0–23`);
        }
        if (period.toHour < 1 || period.toHour > 24) {
            err("periods", `Период «${id}»: toHour вне 1–24`);
        }
        if (period.fromHour === period.toHour) {
            err("periods", `Период «${id}»: нулевая длительность`);
        }
    }

    hourCoverage(model).forEach((n, hour) => {
        if (n === 0) err("periods", `Час ${hour}:00 не покрыт ни одним периодом`);
        else if (n > 1) err("periods", `Час ${hour}:00 покрыт ${n} периодами (нужен ровно 1)`);
    });

    /**
     * WeatherEngine.fallbackState кидает IllegalStateException, если в текущем
     * периоде не разрешено ни текущее состояние, ни состояние по умолчанию.
     * Проверки на это не было ни здесь, ни в WeatherConfigValidator — конфиг
     * выглядел валидным и ронял тик по крону.
     */
    const defaultDefinition = defaultState ? model.states[defaultState] : undefined;
    if (defaultDefinition && defaultDefinition.allowedPeriods.length) {
        const missing = periodIds.filter((p) => !defaultDefinition.allowedPeriods.includes(p));
        if (missing.length) {
            err(
                "settings",
                `Состояние по умолчанию «${defaultState}» запрещено в периодах: ${missing.join(", ")}. ` +
                `Движок падает, если в периоде недоступно ни текущее состояние, ни состояние по умолчанию`,
            );
        }
    }

    /* ---------- условия ---------- */

    const checkConditions = (conditions: Conditions | undefined, where: string) => {
        if (!conditions) return;

        for (const period of conditions.periods) {
            if (!knownPeriods.has(period)) {
                err("states", `${where}: условие ссылается на неизвестный период «${period}»`);
            }
        }
        for (const tag of [...conditions.currentAnyTags, ...conditions.currentAllTags]) {
            if (!knownTags.has(tag)) {
                warn("states", `${where}: тег «${tag}» не встречается ни у одного состояния`);
            }
        }
        for (const key of Object.keys(conditions.atmosphere)) {
            if (!knownAtmosphere.has(key)) {
                warn("states", `${where}: переменная атмосферы «${key}» нигде не объявлена`);
            }
        }

        const history = [
            ["recentWeather", conditions.recentWeather] as const,
            ["absentRecentWeather", conditions.absentRecentWeather] as const,
        ];
        for (const [name, h] of history) {
            if (!h) continue;
            for (const stateId of h.anyStates) {
                if (!knownStates.has(stateId)) {
                    err("states", `${where}: ${name}.anyStates ссылается на неизвестное состояние «${stateId}»`);
                }
            }
            for (const tag of [...h.anyTags, ...h.allTags]) {
                if (!knownTags.has(tag)) {
                    warn("states", `${where}: ${name} использует тег «${tag}», которого нет ни у одного состояния`);
                }
            }
            if (h.withinTicks > model.settings.historySize) {
                warn(
                    "states",
                    `${where}: ${name}.withinTicks (${h.withinTicks}) больше размера истории (${model.settings.historySize}) — окно обрежется`,
                );
            }
        }
    };

    /* ---------- переходы ---------- */

    const incoming = new Set<string>();

    const checkTransitions = (transitions: Transition[], source: string) => {
        for (const t of transitions) {
            const label = `Переход «${source}» → «${t.to || "?"}»`;

            if (!t.to || !knownStates.has(t.to)) {
                err("states", `${label} ведёт в неизвестное состояние`);
            } else {
                incoming.add(t.to);
            }
            if (!Number.isFinite(t.weight) || t.weight < 0) {
                err("states", `${label}: вес должен быть ≥ 0`);
            }
            if (t.weight === 0) {
                warn("states", `${label}: нулевой вес — переход никогда не выберется`);
            }

            checkConditions(t.conditions, label);

            for (const [i, modifier] of t.modifiers.entries()) {
                const modLabel = `${label}, модификатор #${i + 1}`;
                if (!Number.isFinite(modifier.multiplier) || modifier.multiplier < 0) {
                    err("states", `${modLabel}: множитель должен быть ≥ 0`);
                }
                if (isEmptyConditions(modifier.condition)) {
                    warn("states", `${modLabel}: пустое условие — множитель применяется всегда`);
                }
                // WeatherConfigValidator проверяет периоды и внутри модификаторов;
                // редактор раньше смотрел только на conditions самого перехода.
                checkConditions(modifier.condition, modLabel);
            }
        }
    };

    /* ---------- состояния ---------- */

    for (const [id, d] of Object.entries(model.states)) {
        if (!id.trim()) err("states", "У состояния пустой id");

        // WeatherConfigValidator кидает исключение на пустом serverWeather,
        // поэтому здесь ошибка, а не замечание.
        if (!d.serverWeather.trim()) {
            err("states", `«${id}»: пустой serverWeather — сервер не загрузит конфиг`);
        }

        for (const period of d.allowedPeriods) {
            if (!knownPeriods.has(period)) {
                err("states", `«${id}»: allowedPeriods ссылается на неизвестный период «${period}»`);
            }
        }

        for (const [period, variants] of Object.entries(d.presentations)) {
            if (period !== "*" && !knownPeriods.has(period)) {
                err("states", `«${id}»: презентация для неизвестного периода «${period}»`);
            }
            if (variants.length === 0) {
                warn("states", `«${id}»: у презентации «${period}» нет вариантов`);
            }
            for (const [i, p] of variants.entries()) {
                const label = `«${id}» / презентация «${period}» #${i + 1}`;
                // WeatherAnnouncer режет первые два символа без проверок:
                // пустая строка роняет анонс, "FFFF00" без 0x даёт чужой цвет.
                if (!isValidStoredColor(p.color)) {
                    err("states", `${label}: цвет «${p.color}» должен быть в формате 0xRRGGBB`);
                }
                if (p.text.length === 0 || p.text.every((line) => !line.trim())) {
                    warn("states", `${label}: нет текста, игроки увидят заглушку «Погода: ${id}»`);
                }
                if (p.weight <= 0) {
                    warn("states", `${label}: вес ≤ 0 будет поднят движком до 1`);
                }
            }
        }

        // Периоды, в которых состояние может оказаться, но описания для них нет:
        // WeatherPresentationService свалится на "*", потом на любой доступный,
        // потом на захардкоженную заглушку.
        const effectivePeriods = d.allowedPeriods.length ? d.allowedPeriods : periodIds;
        const hasWildcard = "*" in d.presentations;
        if (!hasWildcard) {
            const uncovered = effectivePeriods.filter(
                (p) => knownPeriods.has(p) && !(p in d.presentations),
            );
            if (uncovered.length && Object.keys(d.presentations).length) {
                warn(
                    "states",
                    `«${id}»: нет презентации для периодов ${uncovered.join(", ")} — движок возьмёт описание от другого периода`,
                );
            }
        }

        if (d.duration.minTicks > d.duration.maxTicks) {
            warn("states", `«${id}»: minTicks > maxTicks (движок подравняет)`);
        }
        if (!d.transitions.length && !model.globalTransitions.length) {
            warn("states", `«${id}»: нет исходящих переходов (тупик)`);
        }

        for (const key of Object.keys(d.atmosphereChanges)) {
            if (!knownAtmosphere.has(key)) {
                warn(
                    "states",
                    `«${id}»: атмосфера «${key}» не объявлена в границах — значение не клампится`,
                );
            }
        }

        const targets = new Set<string>();
        for (const t of d.transitions) {
            if (targets.has(t.to)) {
                warn("states", `«${id}»: несколько переходов в «${t.to}» — движок сложит их веса`);
            }
            targets.add(t.to);
        }

        checkTransitions(d.transitions, id);
    }

    checkTransitions(model.globalTransitions, "∀ глобальные");

    for (const id of stateIds) {
        if (id === defaultState || incoming.has(id)) continue;
        warn("states", `«${id}»: недостижимо (нет входящих переходов)`);
    }

    /* ---------- атмосфера ---------- */

    for (const [key, range] of Object.entries(model.settings.atmosphereBounds)) {
        if (range.min != null && range.max != null && range.min >= range.max) {
            err("settings", `Границы «${key}»: min должен быть меньше max`);
        }
        const initial = model.settings.initialAtmosphere[key];
        if (initial != null) {
            if (range.min != null && initial < range.min) {
                warn("settings", `Стартовое значение «${key}» ниже минимума`);
            }
            if (range.max != null && initial > range.max) {
                warn("settings", `Стартовое значение «${key}» выше максимума`);
            }
        }
    }

    return {errors, warnings};
}

/** Русское склонение для счётчиков в бейдже. */
export function plural(n: number, one: string, few: string, many: string): string {
    const abs = Math.abs(n) % 100;
    const last = abs % 10;
    if (abs > 10 && abs < 20) return many;
    if (last === 1) return one;
    if (last > 1 && last < 5) return few;
    return many;
}

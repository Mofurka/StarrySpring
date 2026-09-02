import type {
    Conditions,
    Duration,
    HistoryCondition,
    Period,
    Presentation,
    Range,
    Settings,
    StateDefinition,
    Transition,
    WeatherModel,
    WeightModifier,
} from "./types";

/**
 * Приводит произвольный JSON к полностью заполненной модели.
 *
 * Нормализация глубокая — в отличие от прежней версии, где доводились только
 * два верхних уровня, а `conditions` / `modifiers` / `presentations` оставались
 * сырыми и каждый компонент дострачивал `|| []` на месте. Значения по
 * умолчанию совпадают с компактными конструкторами Java-записей, чтобы
 * редактор и сервер видели одну и ту же конфигурацию.
 */
export function normalize(raw: unknown): WeatherModel {
    const m = obj(raw);
    return {
        settings: normalizeSettings(m.settings),
        periods: mapValues(obj(m.periods), normalizePeriod),
        states: mapValues(obj(m.states), normalizeState),
        globalTransitions: array(m.globalTransitions).map(normalizeTransition),
    };
}

function normalizeSettings(raw: unknown): Settings {
    const s = obj(raw);
    // Java: historySize <= 0 -> 24, sameStateMultiplier <= 0 -> 1.0
    const historySize = num(s.historySize, 24);
    const sameStateMultiplier = num(s.sameStateMultiplier, 1);
    return {
        defaultState: str(s.defaultState),
        historySize: historySize <= 0 ? 24 : Math.round(historySize),
        sameStateMultiplier: sameStateMultiplier <= 0 ? 1 : sameStateMultiplier,
        initialAtmosphere: mapValues(obj(s.initialAtmosphere), (v) => num(v, 0)),
        atmosphereBounds: mapValues(obj(s.atmosphereBounds), normalizeRange),
    };
}

function normalizePeriod(raw: unknown): Period {
    const p = obj(raw);
    return {
        fromHour: Math.round(num(p.fromHour, 0)),
        toHour: Math.round(num(p.toHour, 1)),
        channelName: str(p.channelName),
    };
}

function normalizeState(raw: unknown): StateDefinition {
    const d = obj(raw);
    return {
        serverWeather: str(d.serverWeather),
        tags: strings(d.tags),
        allowedPeriods: strings(d.allowedPeriods),
        cooldownTicks: Math.max(0, Math.round(num(d.cooldownTicks, 0))),
        duration: normalizeDuration(d.duration),
        atmosphereChanges: mapValues(obj(d.atmosphereChanges), (v) => num(v, 0)),
        transitions: array(d.transitions).map(normalizeTransition),
        presentations: mapValues(obj(d.presentations), (list) =>
            array(list).map(normalizePresentation),
        ),
    };
}

function normalizeDuration(raw: unknown): Duration {
    const d = obj(raw);
    // Java: minTicks = max(1, minTicks), maxTicks = max(minTicks, maxTicks)
    const minTicks = Math.max(1, Math.round(num(d.minTicks, 1)));
    return {
        minTicks,
        maxTicks: Math.max(minTicks, Math.round(num(d.maxTicks, minTicks))),
    };
}

function normalizeTransition(raw: unknown): Transition {
    const t = obj(raw);
    const transition: Transition = {
        to: str(t.to),
        weight: num(t.weight, 0),
        modifiers: array(t.modifiers).map(normalizeModifier),
    };
    // Отсутствие conditions значимо: null на стороне Java -> Conditions.empty()
    if (t.conditions != null) transition.conditions = normalizeConditions(t.conditions);
    return transition;
}

function normalizeModifier(raw: unknown): WeightModifier {
    const m = obj(raw);
    return {
        condition: normalizeConditions(m.condition),
        multiplier: num(m.multiplier, 1),
    };
}

export function normalizeConditions(raw: unknown): Conditions {
    const c = obj(raw);
    const conditions: Conditions = {
        periods: strings(c.periods),
        currentAnyTags: strings(c.currentAnyTags),
        currentAllTags: strings(c.currentAllTags),
        atmosphere: mapValues(obj(c.atmosphere), normalizeRange),
    };
    if (c.recentWeather != null) {
        conditions.recentWeather = normalizeHistoryCondition(c.recentWeather);
    }
    if (c.absentRecentWeather != null) {
        conditions.absentRecentWeather = normalizeHistoryCondition(c.absentRecentWeather);
    }
    return conditions;
}

export function normalizeHistoryCondition(raw: unknown): HistoryCondition {
    const h = obj(raw);
    return {
        anyTags: strings(h.anyTags),
        allTags: strings(h.allTags),
        anyStates: strings(h.anyStates),
        withinTicks: Math.max(1, Math.round(num(h.withinTicks, 1))),
    };
}

function normalizeRange(raw: unknown): Range {
    const r = obj(raw);
    const range: Range = {};
    if (typeof r.min === "number" && Number.isFinite(r.min)) range.min = r.min;
    if (typeof r.max === "number" && Number.isFinite(r.max)) range.max = r.max;
    return range;
}

function normalizePresentation(raw: unknown): Presentation {
    const p = obj(raw);
    // Java: weight <= 0 -> 1.0
    const weight = num(p.weight, 1);
    return {
        weight: weight <= 0 ? 1 : weight,
        color: str(p.color),
        image: str(p.image),
        text: strings(p.text),
        stats: mapValues(obj(p.stats), (v) => Math.round(num(v, 0))),
    };
}

export function emptyModel(): WeatherModel {
    return normalize({});
}

export function emptyConditions(): Conditions {
    return normalizeConditions({});
}

/** Пустая структура, эквивалентная отсутствию условий (Conditions.empty() в Java). */
export function isEmptyConditions(c: Conditions | undefined): boolean {
    if (!c) return true;
    return (
        c.periods.length === 0 &&
        c.currentAnyTags.length === 0 &&
        c.currentAllTags.length === 0 &&
        Object.keys(c.atmosphere).length === 0 &&
        !c.recentWeather &&
        !c.absentRecentWeather
    );
}

export function hasConditions(t: Transition): boolean {
    return !isEmptyConditions(t.conditions);
}

/* ---------- примитивы ---------- */

function obj(v: unknown): Record<string, unknown> {
    return v && typeof v === "object" && !Array.isArray(v)
        ? (v as Record<string, unknown>)
        : {};
}

function array(v: unknown): unknown[] {
    return Array.isArray(v) ? v : [];
}

function str(v: unknown): string {
    return typeof v === "string" ? v : "";
}

function strings(v: unknown): string[] {
    return array(v).filter((x): x is string => typeof x === "string");
}

function num(v: unknown, fallback: number): number {
    return typeof v === "number" && Number.isFinite(v) ? v : fallback;
}

function mapValues<T>(
    source: Record<string, unknown>,
    fn: (value: unknown) => T,
): Record<string, T> {
    const out: Record<string, T> = {};
    for (const key of Object.keys(source)) out[key] = fn(source[key]);
    return out;
}

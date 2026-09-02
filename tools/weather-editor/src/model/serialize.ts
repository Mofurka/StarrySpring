import {isEmptyConditions} from "./normalize";
import type {
    Conditions,
    HistoryCondition,
    Presentation,
    Range,
    Settings,
    StateDefinition,
    Transition,
    WeatherModel,
} from "./types";

/**
 * Сериализация в тот вид, в котором конфиг лежит в
 * config/plugins/irden/weather/weather-system.json.
 *
 * Порядок ключей и правило «пустые коллекции опускаем» сохранены намеренно,
 * чтобы diff после правки в редакторе оставался читаемым.
 *
 * Поля `color` у состояния здесь нет и не было: Java-запись Definition его не
 * знает. Раньше UI позволял его задать, а сериализатор молча выбрасывал —
 * теперь поля нет и в UI.
 */
export function serialize(model: WeatherModel): string {
    return JSON.stringify(toPlain(model), null, 2);
}

export function toPlain(model: WeatherModel): unknown {
    return {
        settings: plainSettings(model.settings),
        periods: mapValues(model.periods, (p) => ({
            fromHour: p.fromHour,
            toHour: p.toHour,
            channelName: p.channelName,
        })),
        globalTransitions: model.globalTransitions.map(plainTransition),
        states: mapValues(model.states, plainState),
    };
}

function plainSettings(s: Settings) {
    return {
        defaultState: s.defaultState,
        historySize: s.historySize,
        sameStateMultiplier: s.sameStateMultiplier,
        initialAtmosphere: {...s.initialAtmosphere},
        atmosphereBounds: mapValues(s.atmosphereBounds, plainRange),
    };
}

function plainState(d: StateDefinition) {
    const out: Record<string, unknown> = {serverWeather: d.serverWeather};
    if (d.tags.length) out.tags = [...d.tags];
    if (d.allowedPeriods.length) out.allowedPeriods = [...d.allowedPeriods];
    out.cooldownTicks = d.cooldownTicks;
    out.duration = {minTicks: d.duration.minTicks, maxTicks: d.duration.maxTicks};
    if (Object.keys(d.atmosphereChanges).length) {
        out.atmosphereChanges = {...d.atmosphereChanges};
    }
    if (d.transitions.length) out.transitions = d.transitions.map(plainTransition);
    if (Object.keys(d.presentations).length) {
        out.presentations = mapValues(d.presentations, (list) => list.map(plainPresentation));
    }
    return out;
}

function plainTransition(t: Transition) {
    const out: Record<string, unknown> = {to: t.to, weight: t.weight};
    const conditions = plainConditions(t.conditions);
    if (conditions) out.conditions = conditions;
    if (t.modifiers.length) {
        out.modifiers = t.modifiers.map((m) => ({
            condition: plainConditions(m.condition) ?? {},
            multiplier: m.multiplier,
        }));
    }
    return out;
}

function plainConditions(c: Conditions | undefined): Record<string, unknown> | null {
    if (!c || isEmptyConditions(c)) return null;
    const out: Record<string, unknown> = {};
    if (c.periods.length) out.periods = [...c.periods];
    if (c.currentAnyTags.length) out.currentAnyTags = [...c.currentAnyTags];
    if (c.currentAllTags.length) out.currentAllTags = [...c.currentAllTags];
    if (Object.keys(c.atmosphere).length) {
        out.atmosphere = mapValues(c.atmosphere, plainRange);
    }
    if (c.recentWeather) out.recentWeather = plainHistory(c.recentWeather);
    if (c.absentRecentWeather) out.absentRecentWeather = plainHistory(c.absentRecentWeather);
    return out;
}

function plainHistory(h: HistoryCondition) {
    const out: Record<string, unknown> = {withinTicks: h.withinTicks};
    if (h.anyTags.length) out.anyTags = [...h.anyTags];
    if (h.allTags.length) out.allTags = [...h.allTags];
    if (h.anyStates.length) out.anyStates = [...h.anyStates];
    return out;
}

function plainRange(r: Range) {
    const out: Record<string, number> = {};
    if (r.min != null) out.min = r.min;
    if (r.max != null) out.max = r.max;
    return out;
}

function plainPresentation(p: Presentation) {
    const out: Record<string, unknown> = {
        weight: p.weight,
        color: p.color,
        image: p.image,
        text: [...p.text],
    };
    if (Object.keys(p.stats).length) out.stats = {...p.stats};
    return out;
}

function mapValues<T, R>(
    source: Record<string, T>,
    fn: (value: T) => R,
): Record<string, R> {
    const out: Record<string, R> = {};
    for (const key of Object.keys(source)) out[key] = fn(source[key]);
    return out;
}

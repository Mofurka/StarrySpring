import {isEmptyConditions} from "./normalize";
import {hourCoverage, periodForHour} from "./periods";
import type {Conditions, Duration, HistoryCondition, Range, StateDefinition, Transition, WeatherModel,} from "./types";

/**
 * Порт WeatherEngine на TypeScript: 1 тик = 1 час, старт с полуночи,
 * детерминированный seed.
 *
 * Соответствие оригиналу здесь — единственное, ради чего вкладка «Симуляция»
 * существует, поэтому порядок операций внутри тика повторяет
 * WeatherEngine.tick буквально:
 *
 *   1. применить atmosphereChanges текущего состояния;
 *   2. принять решение по истории *без* записи за текущий тик
 *      (в Java в addCandidates уходит updatedCurrent с current.history());
 *   3. только потом дописать запись в историю (updatedHistory).
 *
 * Прежняя версия делала unshift до решения, а затем condMatch добавлял
 * текущее состояние ещё раз — в итоге окна withinTicks и cooldownTicks были
 * короче серверных на один тик.
 */

interface HistoryEntry {
    stateId: string;
    tags: string[];
}

export interface AtmosphereStat {
    min: number;
    max: number;
    mean: number;
    bound: Range;
}

export interface RuleStat {
    key: string;
    source: string;
    to: string;
    global: boolean;
    weight: number;
    hasConditions: boolean;
    /** сколько раз правило рассматривалось */
    attempted: number;
    blockedByPeriod: number;
    blockedByCooldown: number;
    blockedByConditions: number;
    /** сколько раз дошло до розыгрыша веса */
    eligible: number;
}

export interface SimResult {
    ticks: number;
    timeIn: Record<string, number>;
    entries: Record<string, number>;
    atmosphere: Record<string, AtmosphereStat>;
    rules: RuleStat[];
}

export type SimOutcome =
    | { ok: true; result: SimResult }
    | { ok: false; error: string };

export function simulate(model: WeatherModel, ticks: number): SimOutcome {
    const states = model.states;
    const settings = model.settings;
    const bounds = settings.atmosphereBounds;

    const start = settings.defaultState;
    if (!start || !states[start]) {
        return {
            ok: false,
            error: "Не задано корректное состояние по умолчанию (вкладка «Настройки»).",
        };
    }
    if (hourCoverage(model).some((n) => n !== 1)) {
        return {
            ok: false,
            error:
                "Периоды не покрывают ровно каждый час 0–23. Исправьте на вкладке «Периоды», иначе симуляция недостоверна.",
        };
    }

    const periodByHour: string[] = [];
    for (let hour = 0; hour < 24; hour++) {
        const period = periodForHour(model, hour);
        if (!period) return {ok: false, error: `Час ${hour}:00 не принадлежит ни одному периоду.`};
        periodByHour.push(period);
    }

    const rnd = mulberry32(0x9e3779b9);

    /* ---------- сбор статистики по правилам ---------- */

    const rules: RuleStat[] = [];
    const ruleStats = new Map<Transition, RuleStat>();
    const registerRule = (t: Transition, source: string, global: boolean) => {
        const stat: RuleStat = {
            key: `${global ? "∀" : source}→${t.to}#${rules.length}`,
            source: global ? "∀" : source,
            to: t.to,
            global,
            weight: t.weight,
            hasConditions: !isEmptyConditions(t.conditions),
            attempted: 0,
            blockedByPeriod: 0,
            blockedByCooldown: 0,
            blockedByConditions: 0,
            eligible: 0,
        };
        rules.push(stat);
        ruleStats.set(t, stat);
    };
    for (const [id, d] of Object.entries(states)) {
        for (const t of d.transitions) registerRule(t, id, false);
    }
    for (const t of model.globalTransitions) registerRule(t, "∀", true);

    const timeIn: Record<string, number> = {};
    const entries: Record<string, number> = {};
    for (const id of Object.keys(states)) {
        timeIn[id] = 0;
        entries[id] = 0;
    }

    const trackedKeys = [
        ...new Set([...Object.keys(bounds), ...Object.keys(settings.initialAtmosphere)]),
    ];
    const stats: Record<string, { min: number; max: number; sum: number }> = {};
    for (const key of trackedKeys) stats[key] = {min: Infinity, max: -Infinity, sum: 0};

    /* ---------- состояние симуляции ---------- */

    let current = start;
    let atmosphere: Record<string, number> = {...settings.initialAtmosphere};
    let remaining = randomDuration(states[current].duration, rnd);
    let history: HistoryEntry[] = [];

    for (let tick = 0; tick < ticks; tick++) {
        const period = periodByHour[tick % 24];
        const currentDefinition = states[current];

        atmosphere = applyAtmosphere(atmosphere, currentDefinition.atmosphereChanges, bounds);
        for (const key of trackedKeys) {
            const value = atmosphere[key] ?? 0;
            const s = stats[key];
            if (value < s.min) s.min = value;
            if (value > s.max) s.max = value;
            s.sum += value;
        }
        timeIn[current]++;

        const currentAllowed = isAllowed(currentDefinition, period);
        let nextState = current;
        let nextRemaining = remaining;

        if (currentAllowed && remaining > 1) {
            nextRemaining = remaining - 1;
        } else {
            const candidates = new Map<string, number>();

            const consider = (rule: Transition) => {
                const target = states[rule.to];
                const stat = ruleStats.get(rule);
                if (stat) stat.attempted++;
                if (!target) return;

                if (!isAllowed(target, period)) {
                    if (stat) stat.blockedByPeriod++;
                    return;
                }
                if (isOnCooldown(rule.to, target, current, history)) {
                    if (stat) stat.blockedByCooldown++;
                    return;
                }
                if (
                    !conditionsMatch(rule.conditions, period, current, currentDefinition, atmosphere, history)
                ) {
                    if (stat) stat.blockedByConditions++;
                    return;
                }
                if (stat) stat.eligible++;

                let weight = rule.weight;
                for (const modifier of rule.modifiers) {
                    if (
                        conditionsMatch(
                            modifier.condition,
                            period,
                            current,
                            currentDefinition,
                            atmosphere,
                            history,
                        )
                    ) {
                        // Умножаем как есть: множитель 0 — легальный способ полностью
                        // погасить переход. Прежний `|| 1` превращал его в единицу.
                        weight *= modifier.multiplier;
                    }
                }
                if (rule.to === current) weight *= settings.sameStateMultiplier;

                if (weight > 0) {
                    candidates.set(rule.to, (candidates.get(rule.to) ?? 0) + weight);
                }
            };

            for (const rule of currentDefinition.transitions) consider(rule);
            for (const rule of model.globalTransitions) consider(rule);

            let selected: string;
            if (candidates.size === 0) {
                // WeatherEngine.fallbackState
                if (currentAllowed) {
                    selected = current;
                } else if (isAllowed(states[start], period)) {
                    selected = start;
                } else {
                    return {
                        ok: false,
                        error:
                            `Тик ${tick}: в периоде «${period}» недоступно ни текущее состояние «${current}», ` +
                            `ни состояние по умолчанию «${start}». На сервере это IllegalStateException в WeatherEngine.fallbackState.`,
                    };
                }
            } else {
                selected = choose(candidates, rnd);
            }

            if (selected !== current) entries[selected]++;
            nextState = selected;
            nextRemaining = randomDuration(states[selected].duration, rnd);
        }

        // updatedHistory вызывается в обеих ветках WeatherEngine.tick — то есть
        // запись появляется каждый тик, а не только при смене состояния.
        history = [
            {stateId: current, tags: currentDefinition.tags},
            ...history,
        ].slice(0, settings.historySize);

        current = nextState;
        remaining = nextRemaining;
    }

    const atmosphereStats: Record<string, AtmosphereStat> = {};
    for (const key of trackedKeys) {
        const s = stats[key];
        atmosphereStats[key] = {
            min: s.min === Infinity ? 0 : s.min,
            max: s.max === -Infinity ? 0 : s.max,
            mean: s.sum / ticks,
            bound: bounds[key] ?? {},
        };
    }

    return {ok: true, result: {ticks, timeIn, entries, atmosphere: atmosphereStats, rules}};
}

/* ---------- шаги движка ---------- */

function isAllowed(definition: StateDefinition, period: string): boolean {
    return definition.allowedPeriods.length === 0 || definition.allowedPeriods.includes(period);
}

/**
 * WeatherEngine.isOnCooldown.
 *
 * Продление уже идущего состояния — не повторный вход, поэтому при
 * `current === target` кулдаун не применяется. Раньше здесь возвращался true,
 * из-за чего самопереход у состояния с cooldownTicks > 0 был мёртв всегда
 * (в боевом конфиге так пропадали warm_rain→warm_rain с весом 18,
 * groundmist→groundmist и fog→fog).
 */
function isOnCooldown(
    targetState: string,
    targetDefinition: StateDefinition,
    currentState: string,
    history: HistoryEntry[],
): boolean {
    const cooldown = targetDefinition.cooldownTicks;
    if (cooldown <= 0) return false;
    if (currentState === targetState) return false;
    return history.slice(0, cooldown).some((entry) => entry.stateId === targetState);
}

function conditionsMatch(
    conditions: Conditions | undefined,
    period: string,
    currentState: string,
    currentDefinition: StateDefinition,
    atmosphere: Record<string, number>,
    history: HistoryEntry[],
): boolean {
    if (!conditions || isEmptyConditions(conditions)) return true;

    if (conditions.periods.length && !conditions.periods.includes(period)) return false;

    const tags = currentDefinition.tags;
    if (
        conditions.currentAnyTags.length &&
        !conditions.currentAnyTags.some((tag) => tags.includes(tag))
    ) {
        return false;
    }
    if (!conditions.currentAllTags.every((tag) => tags.includes(tag))) return false;

    for (const [key, range] of Object.entries(conditions.atmosphere)) {
        if (!rangeContains(range, atmosphere[key] ?? 0)) return false;
    }

    // WeatherConditionEvaluator.timelineWithCurrent: текущее состояние
    // приклеивается к истории ровно один раз.
    const timeline: HistoryEntry[] = [{stateId: currentState, tags}, ...history];

    if (conditions.recentWeather && !historyMatches(timeline, conditions.recentWeather)) {
        return false;
    }
    if (conditions.absentRecentWeather && historyMatches(timeline, conditions.absentRecentWeather)) {
        return false;
    }
    return true;
}

function historyMatches(timeline: HistoryEntry[], condition: HistoryCondition): boolean {
    return timeline.slice(0, condition.withinTicks).some((entry) => {
        const anyTag =
            condition.anyTags.length === 0 || entry.tags.some((tag) => condition.anyTags.includes(tag));
        const allTags = condition.allTags.every((tag) => entry.tags.includes(tag));
        const state = condition.anyStates.length === 0 || condition.anyStates.includes(entry.stateId);
        return anyTag && allTags && state;
    });
}

function rangeContains(range: Range, value: number): boolean {
    return (range.min == null || value >= range.min) && (range.max == null || value <= range.max);
}

function applyAtmosphere(
    atmosphere: Record<string, number>,
    changes: Record<string, number>,
    bounds: Record<string, Range>,
): Record<string, number> {
    if (Object.keys(changes).length === 0) return atmosphere;

    const updated = {...atmosphere};
    for (const [key, delta] of Object.entries(changes)) {
        let value = (updated[key] ?? 0) + delta;
        const range = bounds[key];
        if (range) {
            if (range.min != null) value = Math.max(range.min, value);
            if (range.max != null) value = Math.min(range.max, value);
        }
        updated[key] = value;
    }
    return updated;
}

function choose(weights: Map<string, number>, rnd: () => number): string {
    let total = 0;
    for (const w of weights.values()) total += w;

    let roll = rnd() * total;
    for (const [key, weight] of weights) {
        roll -= weight;
        if (roll <= 0) return key;
    }
    return weights.keys().next().value as string;
}

function randomDuration(duration: Duration, rnd: () => number): number {
    if (duration.minTicks >= duration.maxTicks) return duration.minTicks;
    return duration.minTicks + Math.floor(rnd() * (duration.maxTicks - duration.minTicks + 1));
}

function mulberry32(seed: number): () => number {
    let a = seed;
    return () => {
        a |= 0;
        a = (a + 0x6d2b79f5) | 0;
        let t = Math.imul(a ^ (a >>> 15), 1 | a);
        t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
}

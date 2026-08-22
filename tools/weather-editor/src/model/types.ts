/**
 * Зеркало Java-записей из
 * plugins/plugin-irden/src/main/java/irden/space/proxy/plugin/irden/weather/WeatherConfig.java
 *
 * Модель в редакторе всегда нормализована (см. normalize.ts): все коллекции
 * присутствуют и не равны null, поэтому по всему UI не нужны `|| []`.
 * Опциональны только те поля, отсутствие которых на стороне Java значимо
 * (`conditions`, `recentWeather`, `absentRecentWeather`, границы диапазона).
 */

export interface Range {
  min?: number;
  max?: number;
}

export interface Period {
  fromHour: number;
  toHour: number;
  channelName: string;
}

export interface Duration {
  minTicks: number;
  maxTicks: number;
}

export interface HistoryCondition {
  anyTags: string[];
  allTags: string[];
  anyStates: string[];
  withinTicks: number;
}

export interface Conditions {
  periods: string[];
  currentAnyTags: string[];
  currentAllTags: string[];
  atmosphere: Record<string, Range>;
  recentWeather?: HistoryCondition;
  absentRecentWeather?: HistoryCondition;
}

export interface WeightModifier {
  condition: Conditions;
  multiplier: number;
}

export interface Transition {
  to: string;
  weight: number;
  conditions?: Conditions;
  modifiers: WeightModifier[];
}

export interface Presentation {
  weight: number;
  color: string;
  image: string;
  text: string[];
  stats: Record<string, number>;
}

export interface StateDefinition {
  serverWeather: string;
  tags: string[];
  allowedPeriods: string[];
  cooldownTicks: number;
  duration: Duration;
  atmosphereChanges: Record<string, number>;
  transitions: Transition[];
  /** ключ — id периода либо "*" (любой период) */
  presentations: Record<string, Presentation[]>;
}

export interface Settings {
  defaultState: string;
  historySize: number;
  sameStateMultiplier: number;
  initialAtmosphere: Record<string, number>;
  atmosphereBounds: Record<string, Range>;
}

export interface WeatherModel {
  settings: Settings;
  periods: Record<string, Period>;
  states: Record<string, StateDefinition>;
  globalTransitions: Transition[];
}

export type TabId =
  | "graph"
  | "settings"
  | "periods"
  | "states"
  | "global"
  | "sim"
  | "json";

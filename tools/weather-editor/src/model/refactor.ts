import type { Conditions, HistoryCondition, Transition, WeatherModel } from "./types";

/**
 * Каскадные переименования и удаления.
 *
 * Ссылки на состояние живут не только в `transition.to`, но и в
 * `conditions.recentWeather.anyStates` / `absentRecentWeather.anyStates` —
 * причём и у самих переходов, и у модификаторов веса. Прежняя версия правила
 * только `to`, из-за чего условие после переименования молча переставало
 * срабатывать. Ссылки на период живут в `allowedPeriods`, ключах
 * `presentations` и `conditions.periods` — их не правили вовсе.
 */

/** Все переходы модели: собственные у состояний плюс глобальные. */
export function allTransitions(model: WeatherModel): Transition[] {
  const out: Transition[] = [...model.globalTransitions];
  for (const state of Object.values(model.states)) out.push(...state.transitions);
  return out;
}

/** Каждый блок условий модели: у переходов и у их модификаторов. */
export function forEachConditions(
  model: WeatherModel,
  fn: (conditions: Conditions) => void,
): void {
  for (const transition of allTransitions(model)) {
    if (transition.conditions) fn(transition.conditions);
    for (const modifier of transition.modifiers) fn(modifier.condition);
  }
}

function forEachHistoryCondition(
  model: WeatherModel,
  fn: (history: HistoryCondition) => void,
): void {
  forEachConditions(model, (c) => {
    if (c.recentWeather) fn(c.recentWeather);
    if (c.absentRecentWeather) fn(c.absentRecentWeather);
  });
}

/** Пересоздаёт объект с переименованным ключом, сохраняя исходный порядок. */
function renameKey<T>(
  source: Record<string, T>,
  oldId: string,
  newId: string,
): Record<string, T> {
  const out: Record<string, T> = {};
  for (const key of Object.keys(source)) out[key === oldId ? newId : key] = source[key];
  return out;
}

function replaceInList(list: string[], oldId: string, newId: string): void {
  for (let i = 0; i < list.length; i++) {
    if (list[i] === oldId) list[i] = newId;
  }
}

function removeFromList(list: string[], id: string): number {
  let removed = 0;
  for (let i = list.length - 1; i >= 0; i--) {
    if (list[i] === id) {
      list.splice(i, 1);
      removed++;
    }
  }
  return removed;
}

/* ---------- состояния ---------- */

export function renameState(model: WeatherModel, oldId: string, newId: string): void {
  model.states = renameKey(model.states, oldId, newId);

  for (const transition of allTransitions(model)) {
    if (transition.to === oldId) transition.to = newId;
  }
  forEachHistoryCondition(model, (h) => replaceInList(h.anyStates, oldId, newId));

  if (model.settings.defaultState === oldId) model.settings.defaultState = newId;
}

export interface StateReferences {
  transitions: number;
  historyConditions: number;
  isDefault: boolean;
}

/** Что сломается при удалении состояния — показываем до подтверждения. */
export function countStateReferences(model: WeatherModel, id: string): StateReferences {
  let transitions = 0;
  let historyConditions = 0;

  for (const transition of allTransitions(model)) {
    if (transition.to === id) transitions++;
  }
  forEachHistoryCondition(model, (h) => {
    if (h.anyStates.includes(id)) historyConditions++;
  });

  return { transitions, historyConditions, isDefault: model.settings.defaultState === id };
}

/**
 * Удаляет состояние, оставляя висящие ссылки на месте: их подсветит валидатор,
 * и автор сам решит, куда перенаправить переход. Молча вырезать чужие переходы
 * дороже, чем показать ошибку.
 */
export function deleteState(model: WeatherModel, id: string): void {
  delete model.states[id];
}

/* ---------- периоды ---------- */

export function renamePeriod(model: WeatherModel, oldId: string, newId: string): void {
  model.periods = renameKey(model.periods, oldId, newId);

  for (const state of Object.values(model.states)) {
    replaceInList(state.allowedPeriods, oldId, newId);
    if (oldId in state.presentations) {
      state.presentations = renameKey(state.presentations, oldId, newId);
    }
  }
  forEachConditions(model, (c) => replaceInList(c.periods, oldId, newId));
}

export interface PeriodReferences {
  allowedPeriods: number;
  presentationBlocks: number;
  presentationVariants: number;
  conditions: number;
}

export function countPeriodReferences(model: WeatherModel, id: string): PeriodReferences {
  let allowedPeriods = 0;
  let presentationBlocks = 0;
  let presentationVariants = 0;
  let conditions = 0;

  for (const state of Object.values(model.states)) {
    if (state.allowedPeriods.includes(id)) allowedPeriods++;
    const block = state.presentations[id];
    if (block) {
      presentationBlocks++;
      presentationVariants += block.length;
    }
  }
  forEachConditions(model, (c) => {
    if (c.periods.includes(id)) conditions++;
  });

  return { allowedPeriods, presentationBlocks, presentationVariants, conditions };
}

/**
 * Удаляет период вместе со всеми ссылками. Здесь каскад оправдан: висящий id
 * периода — всегда ошибка загрузки на сервере, восстановить его по остаткам
 * нельзя. Сколько текстов презентаций при этом исчезнет, показываем заранее
 * через countPeriodReferences.
 */
export function deletePeriod(model: WeatherModel, id: string): void {
  delete model.periods[id];

  for (const state of Object.values(model.states)) {
    removeFromList(state.allowedPeriods, id);
    delete state.presentations[id];
  }
  forEachConditions(model, (c) => removeFromList(c.periods, id));
}

/* ---------- ключи ассоциативных редакторов ---------- */

/**
 * Переименование ключа в map-редакторе одним действием, с сохранением порядка.
 *
 * Прежний редактор коммитил на каждое нажатие клавиши и читал значение по
 * захваченному в замыкании старому ключу: после второго символа значение
 * становилось undefined, а в объекте оседала цепочка полудописанных ключей.
 */
export function renameMapKey<T>(
  source: Record<string, T>,
  oldKey: string,
  newKey: string,
): Record<string, T> {
  if (oldKey === newKey || !(oldKey in source) || newKey in source) return source;
  return renameKey(source, oldKey, newKey);
}

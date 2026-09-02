import {useSyncExternalStore} from "react";
import {loadDefaultConfig} from "../model/defaultConfig";
import {normalize} from "../model/normalize";
import {toPlain} from "../model/serialize";
import type {WeatherModel} from "../model/types";

const STORAGE_KEY = "irden.weather-editor.draft";
const AUTOSAVE_DELAY = 600;

interface StoredDraft {
    fileName: string;
    savedAt: number;
    config: unknown;
}

/**
 * Модель редактируется мутабельно, а перерисовка триггерится счётчиком версии
 * через useSyncExternalStore.
 *
 * Это сознательный выбор: редакторы условий, переходов и презентаций правят
 * глубоко вложенные узлы, и иммутабельные апдейты по всему дереву дали бы
 * втрое больше кода без выигрыша — единственный писатель здесь UI, гонок нет.
 * Взамен есть одна точка входа: любое изменение обязано закончиться commit().
 */
class ModelStore {
    fileName = "weather-system.json";
    /** есть несохранённые в файл правки */
    dirty = false;
    /** момент последнего автосохранения в localStorage */
    autosavedAt: number | null = null;
    /** черновик подхвачен из localStorage при старте */
    restoredFromDraft = false;
    private model: WeatherModel;
    private version = 0;
    private listeners = new Set<() => void>();
    private autosaveTimer: ReturnType<typeof setTimeout> | null = null;

    constructor(model: WeatherModel) {
        this.model = model;
    }

    subscribe = (listener: () => void): (() => void) => {
        this.listeners.add(listener);
        return () => {
            this.listeners.delete(listener);
        };
    };

    getVersion = (): number => this.version;

    get(): WeatherModel {
        return this.model;
    }

    /** Вызывается после любой мутации модели. */
    commit = (): void => {
        this.dirty = true;
        this.bump();
        this.scheduleAutosave();
    };

    /** Полная замена модели: импорт, загрузка примера, применение JSON. */
    replace = (model: WeatherModel, fileName?: string): void => {
        this.model = model;
        if (fileName) this.fileName = fileName;
        this.dirty = false;
        this.restoredFromDraft = false;
        this.bump();
        this.scheduleAutosave();
    };

    markSaved = (fileName?: string): void => {
        if (fileName) this.fileName = fileName;
        this.dirty = false;
        this.bump();
    };

    discardDraft(): void {
        try {
            localStorage.removeItem(STORAGE_KEY);
        } catch {
            /* см. writeDraft */
        }
        this.autosavedAt = null;
        this.restoredFromDraft = false;
        this.bump();
    }

    private bump(): void {
        this.version++;
        for (const listener of this.listeners) listener();
    }

    private scheduleAutosave(): void {
        if (this.autosaveTimer) clearTimeout(this.autosaveTimer);
        this.autosaveTimer = setTimeout(() => {
            this.autosaveTimer = null;
            this.writeDraft();
        }, AUTOSAVE_DELAY);
    }

    private writeDraft(): void {
        const draft: StoredDraft = {
            fileName: this.fileName,
            savedAt: Date.now(),
            config: toPlain(this.model),
        };
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(draft));
            this.autosavedAt = draft.savedAt;
            this.bump();
        } catch {
            // Приватный режим или переполненное хранилище — молча живём без автосейва.
        }
    }
}

function readDraft(): StoredDraft | null {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) return null;
        const parsed = JSON.parse(raw) as StoredDraft;
        if (!parsed || typeof parsed !== "object" || !parsed.config) return null;
        return parsed;
    } catch {
        return null;
    }
}

function createStore(): ModelStore {
    const draft = readDraft();
    if (draft) {
        try {
            const store = new ModelStore(normalize(draft.config));
            store.fileName = draft.fileName || "weather-system.json";
            store.autosavedAt = draft.savedAt ?? null;
            store.restoredFromDraft = true;
            store.dirty = true;
            return store;
        } catch {
            // Битый черновик не должен мешать открыть редактор.
        }
    }
    try {
        return new ModelStore(normalize(loadDefaultConfig()));
    } catch {
        return new ModelStore(normalize({}));
    }
}

export const store = createStore();

/** Перерисовка на каждый commit. Возвращает саму модель — она мутабельна. */
export function useModel(): WeatherModel {
    useSyncExternalStore(store.subscribe, store.getVersion, store.getVersion);
    return store.get();
}

/** Версия модели — для useMemo по производным данным (валидация, симуляция). */
export function useModelVersion(): number {
    return useSyncExternalStore(store.subscribe, store.getVersion, store.getVersion);
}

export const commit = store.commit;

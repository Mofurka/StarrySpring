import {useMemo} from "react";
import {validate, type ValidationResult} from "../model/validate";
import type {WeatherModel} from "../model/types";
import {store, useModelVersion} from "./store";

export interface Derived {
    model: WeatherModel;
    stateIds: string[];
    periodIds: string[];
    /** все теги, встречающиеся у состояний — для подсказок в редакторах условий */
    tags: string[];
    /** объявленные переменные атмосферы: границы, иначе стартовые значения */
    atmosphereKeys: string[];
}

export function useDerived(): Derived {
    const version = useModelVersion();
    return useMemo(() => {
        const model = store.get();
        const tags = new Set<string>();
        for (const state of Object.values(model.states)) {
            for (const tag of state.tags) tags.add(tag);
        }
        const bounds = Object.keys(model.settings.atmosphereBounds);
        return {
            model,
            stateIds: Object.keys(model.states),
            periodIds: Object.keys(model.periods),
            tags: [...tags].sort(),
            atmosphereKeys: bounds.length
                ? bounds
                : Object.keys(model.settings.initialAtmosphere),
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [version]);
}

export function useValidation(): ValidationResult {
    const version = useModelVersion();
    // eslint-disable-next-line react-hooks/exhaustive-deps
    return useMemo(() => validate(store.get()), [version]);
}

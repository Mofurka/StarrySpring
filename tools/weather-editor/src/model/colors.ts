import type {StateDefinition} from "./types";

/**
 * Формат цвета в конфиге — строго `0xRRGGBB`.
 *
 * WeatherAnnouncer делает `Integer.parseInt(color().substring(2), 16)` без
 * какой-либо проверки, поэтому пустая или короткая строка роняет анонс
 * в @Async-листенере (падение молча съедается). Валидатор редактора считает
 * несоответствие этому регэкспу ошибкой, а не замечанием.
 */
export const STORED_COLOR_RE = /^0x[0-9a-f]{6}$/;

/** `0xRRGGBB` | `#RRGGBB` | `RRGGBB` -> `#rrggbb`, иначе null. */
export function normHex(value: string | undefined): string | null {
    if (!value) return null;
    let x = String(value).trim().toLowerCase();
    if (x.startsWith("0x")) x = x.slice(2);
    else if (x.startsWith("#")) x = x.slice(1);
    return /^[0-9a-f]{6}$/.test(x) ? `#${x}` : null;
}

/** `#rrggbb` -> `0xrrggbb` (формат, который читает Java). */
export function toStored(hex: string): string {
    if (!hex) return "";
    return `0x${hex.replace("#", "").toLowerCase()}`;
}

export function isValidStoredColor(value: string): boolean {
    return STORED_COLOR_RE.test(value.trim().toLowerCase());
}

/** Относительная яркость — нужна, чтобы подобрать обводку узла графа. */
export function luminance(hex: string): number {
    const h = normHex(hex);
    if (!h) return 0.5;
    const r = parseInt(h.slice(1, 3), 16) / 255;
    const g = parseInt(h.slice(3, 5), 16) / 255;
    const b = parseInt(h.slice(5, 7), 16) / 255;
    return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

export const TAG_COLORS: Record<string, string> = {
    dangerous: "#cf4b3a",
    storm: "#7c4bd0",
    horror: "#8a3b64",
    special: "#c98a2b",
    magic: "#3aa0c9",
    precipitation: "#2f76c9",
    wet: "#3a8fbf",
    fog: "#8fa3ad",
    clear: "#e0b13a",
    dry: "#caa24a",
    wind: "#5aa38b",
    cloudy: "#8895a0",
};

export const NEUTRAL_NODE_FILL = "#8895a0";

/**
 * Цвет узла графа выводится только из тегов.
 *
 * Раньше здесь сначала читался `d.color`, но такого поля нет ни в Java-записи
 * Definition, ни в сериализаторе редактора — настройка терялась при первом же
 * экспорте. Поле убрано из UI, источник цвета один: теги.
 */
export function nodeFill(d: StateDefinition): string {
    for (const tag of d.tags) {
        const c = TAG_COLORS[tag];
        if (c) return c;
    }
    return NEUTRAL_NODE_FILL;
}

import {useState} from "react";
import {renameMapKey} from "../model/refactor";
import type {Range} from "../model/types";
import {KeyInput, NumInput, SelectInput} from "./controls";

/**
 * Редактор `ключ → число` (atmosphereChanges, initialAtmosphere, stats).
 *
 * Все операции возвращают новый объект через onReplace: только так
 * переименование ключа остаётся одним действием и сохраняет порядок.
 */
export function MapEditor({
                              value,
                              onReplace,
                              keyOptions,
                              keyPlaceholder = "ключ",
                              integer = false,
                          }: {
    value: Record<string, number>;
    onReplace: (next: Record<string, number>) => void;
    /** фиксированный набор ключей: имена не редактируются, добавление — из списка */
    keyOptions?: string[];
    keyPlaceholder?: string;
    integer?: boolean;
}) {
    const [draftKey, setDraftKey] = useState("");
    const keys = Object.keys(value);
    const available = keyOptions?.filter((k) => !(k in value)) ?? null;

    const setValue = (key: string, next: number) => onReplace({...value, [key]: next});

    const remove = (key: string) => {
        const next = {...value};
        delete next[key];
        onReplace(next);
    };

    const add = (key: string) => {
        const trimmed = key.trim();
        if (!trimmed || trimmed in value) return;
        onReplace({...value, [trimmed]: 0});
    };

    return (
        <div className="grid" style={{gap: 7}}>
            {keys.map((key) => (
                <div className="kv" key={key}>
                    {keyOptions ? (
                        <span className="mono" style={{fontSize: 12}}>
              {key}
            </span>
                    ) : (
                        <KeyInput
                            value={key}
                            onCommit={(next) => onReplace(renameMapKey(value, key, next))}
                        />
                    )}
                    <NumInput
                        value={value[key]}
                        onChange={(v) => setValue(key, v)}
                        width="110px"
                        integer={integer}
                    />
                    <span/>
                    <button className="iconbtn" title="Удалить" onClick={() => remove(key)}>
                        ×
                    </button>
                </div>
            ))}
            <div className="row">
                {available ? (
                    <SelectInput
                        value=""
                        options={available}
                        blank="+ переменная"
                        onChange={(k) => k && add(k)}
                    />
                ) : (
                    <input
                        type="text"
                        value={draftKey}
                        placeholder={`+ ${keyPlaceholder}`}
                        onChange={(e) => setDraftKey(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key !== "Enter") return;
                            add(draftKey);
                            setDraftKey("");
                        }}
                        onBlur={() => {
                            add(draftKey);
                            setDraftKey("");
                        }}
                    />
                )}
            </div>
        </div>
    );
}

/**
 * Редактор `ключ → {min?, max?}`: границы атмосферы в настройках и
 * ограничения в условиях переходов.
 */
export function RangeMapEditor({
                                   value,
                                   onReplace,
                                   keyOptions,
                                   editableKeys = false,
                                   addLabel = "+ ограничение",
                                   defaultRange = {min: 0},
                               }: {
    value: Record<string, Range>;
    onReplace: (next: Record<string, Range>) => void;
    keyOptions?: string[];
    editableKeys?: boolean;
    addLabel?: string;
    defaultRange?: Range;
}) {
    const [draftKey, setDraftKey] = useState("");
    const keys = Object.keys(value);
    const available = keyOptions?.filter((k) => !(k in value)) ?? null;

    const setBound = (key: string, field: "min" | "max", next: number | null) => {
        const range: Range = {...value[key]};
        if (next == null) delete range[field];
        else range[field] = next;
        onReplace({...value, [key]: range});
    };

    const remove = (key: string) => {
        const next = {...value};
        delete next[key];
        onReplace(next);
    };

    const add = (key: string) => {
        const trimmed = key.trim();
        if (!trimmed || trimmed in value) return;
        onReplace({...value, [trimmed]: {...defaultRange}});
    };

    return (
        <div className="grid" style={{gap: 7}}>
            <div className="kv tinyhead">
                <span>переменная</span>
                <span>мин</span>
                <span>макс</span>
                <span/>
            </div>
            {keys.map((key) => (
                <div className="kv" key={key}>
                    {editableKeys ? (
                        <KeyInput
                            value={key}
                            onCommit={(next) => onReplace(renameMapKey(value, key, next))}
                        />
                    ) : (
                        <span className="mono" style={{fontSize: 12}}>
              {key}
            </span>
                    )}
                    <BoundInput
                        value={value[key]?.min}
                        onChange={(v) => setBound(key, "min", v)}
                    />
                    <BoundInput
                        value={value[key]?.max}
                        onChange={(v) => setBound(key, "max", v)}
                    />
                    <button className="iconbtn" title="Удалить" onClick={() => remove(key)}>
                        ×
                    </button>
                </div>
            ))}
            <div className="row">
                {available ? (
                    <SelectInput
                        value=""
                        options={available}
                        blank={addLabel}
                        onChange={(k) => k && add(k)}
                    />
                ) : (
                    <input
                        type="text"
                        value={draftKey}
                        placeholder={addLabel}
                        onChange={(e) => setDraftKey(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key !== "Enter") return;
                            add(draftKey);
                            setDraftKey("");
                        }}
                        onBlur={() => {
                            add(draftKey);
                            setDraftKey("");
                        }}
                    />
                )}
            </div>
        </div>
    );
}

/** Граница диапазона может отсутствовать, поэтому пустое поле значимо. */
function BoundInput({
                        value,
                        onChange,
                    }: {
    value: number | undefined;
    onChange: (value: number | null) => void;
}) {
    const [draft, setDraft] = useState(value == null ? "" : String(value));

    const external = value == null ? "" : String(value);
    const [seen, setSeen] = useState(external);
    if (seen !== external) {
        setSeen(external);
        if (Number(draft) !== value) setDraft(external);
    }

    return (
        <input
            type="number"
            className="num"
            step="any"
            value={draft}
            style={{width: 96}}
            placeholder="—"
            onChange={(e) => {
                setDraft(e.target.value);
                if (e.target.value.trim() === "") onChange(null);
                else if (Number.isFinite(Number(e.target.value))) onChange(Number(e.target.value));
            }}
        />
    );
}

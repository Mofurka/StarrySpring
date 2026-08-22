import { useEffect, useId, useState, type CSSProperties, type ReactNode } from "react";

export function Field({
  label,
  hint,
  children,
}: {
  label: ReactNode;
  hint?: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="field">
      <label>{label}</label>
      {children}
      {hint ? <span className="hint">{hint}</span> : null}
    </div>
  );
}

/**
 * Числовой ввод с локальным черновиком.
 *
 * Значение коммитится сразу, как только строка парсится, но сама строка живёт
 * в состоянии компонента — иначе промежуточные «-», «0.» и «1e» превращались
 * бы в NaN и затирали поле на каждом нажатии.
 */
export function NumInput({
  value,
  onChange,
  step = "any",
  min,
  max,
  width,
  integer = false,
}: {
  value: number;
  onChange: (value: number) => void;
  step?: number | "any";
  min?: number;
  max?: number;
  width?: string;
  integer?: boolean;
}) {
  const [draft, setDraft] = useState(() => String(value));

  useEffect(() => {
    setDraft((current) => (Number(current) === value ? current : String(value)));
  }, [value]);

  const commit = (raw: string) => {
    const parsed = Number(raw);
    if (raw.trim() === "" || !Number.isFinite(parsed)) return;
    onChange(integer ? Math.round(parsed) : parsed);
  };

  return (
    <input
      type="number"
      className="num"
      value={draft}
      step={integer ? 1 : step}
      min={min}
      max={max}
      style={width ? { width } : undefined}
      onChange={(e) => {
        setDraft(e.target.value);
        commit(e.target.value);
      }}
      onBlur={() => {
        const parsed = Number(draft);
        if (draft.trim() === "" || !Number.isFinite(parsed)) setDraft(String(value));
        else setDraft(String(integer ? Math.round(parsed) : parsed));
      }}
    />
  );
}

export function TextInput({
  value,
  onChange,
  placeholder,
  width,
  list,
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  width?: string;
  list?: string;
}) {
  return (
    <input
      type="text"
      value={value}
      placeholder={placeholder}
      list={list}
      style={width ? { width } : undefined}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}

/**
 * Текстовое поле, которое коммитит по blur/Enter, а не на каждый символ.
 * Нужно везде, где значение — это ключ: переименование ключа посимвольно
 * оставляло в объекте цепочку недописанных ключей с пустыми значениями.
 */
export function KeyInput({
  value,
  onCommit,
  placeholder,
  width,
}: {
  value: string;
  onCommit: (value: string) => void;
  placeholder?: string;
  width?: string;
}) {
  const [draft, setDraft] = useState(value);

  useEffect(() => setDraft(value), [value]);

  const commit = () => {
    const next = draft.trim();
    if (!next || next === value) {
      setDraft(value);
      return;
    }
    onCommit(next);
  };

  return (
    <input
      type="text"
      value={draft}
      placeholder={placeholder}
      style={width ? { width } : undefined}
      onChange={(e) => setDraft(e.target.value)}
      onBlur={commit}
      onKeyDown={(e) => {
        if (e.key === "Enter") e.currentTarget.blur();
        if (e.key === "Escape") setDraft(value);
      }}
    />
  );
}

export function SelectInput({
  value,
  options,
  onChange,
  blank,
  width,
}: {
  value: string;
  options: string[];
  onChange: (value: string) => void;
  blank?: string;
  width?: string;
}) {
  const unknown = value && !options.includes(value);
  return (
    <select
      value={value}
      style={width ? { width } : undefined}
      onChange={(e) => onChange(e.target.value)}
    >
      {blank ? <option value="">{blank}</option> : null}
      {options.map((o) => (
        <option key={o} value={o}>
          {o}
        </option>
      ))}
      {unknown ? <option value={value}>{value} (?)</option> : null}
    </select>
  );
}

/**
 * Редактор списка строк. `suggestions` подставляет datalist — самый дешёвый
 * способ не плодить опечатки в тегах, которые валидатор теперь ловит,
 * но лучше бы им не появляться.
 */
export function ChipsEditor({
  values,
  onChange,
  tag = false,
  suggestions,
  placeholder = "+ добавить",
}: {
  values: string[];
  onChange: (values: string[]) => void;
  tag?: boolean;
  suggestions?: string[];
  placeholder?: string;
}) {
  const [draft, setDraft] = useState("");
  const listId = useId();

  const add = () => {
    const next = draft.trim();
    if (!next || values.includes(next)) {
      setDraft("");
      return;
    }
    onChange([...values, next]);
    setDraft("");
  };

  return (
    <div className="chips">
      {values.map((value, i) => (
        <span key={`${value}-${i}`} className={`chip${tag ? " tag" : ""}`}>
          {value}
          <button
            title="Убрать"
            onClick={() => onChange(values.filter((_, index) => index !== i))}
          >
            ×
          </button>
        </span>
      ))}
      <span className="chip add">
        <input
          type="text"
          value={draft}
          placeholder={placeholder}
          list={suggestions?.length ? listId : undefined}
          onChange={(e) => setDraft(e.target.value)}
          onBlur={add}
          onKeyDown={(e) => {
            if (e.key === "Enter") add();
          }}
        />
        {suggestions?.length ? (
          <datalist id={listId}>
            {suggestions.map((s) => (
              <option key={s} value={s} />
            ))}
          </datalist>
        ) : null}
      </span>
    </div>
  );
}

export function Subhead({ children }: { children: ReactNode }) {
  return (
    <div className="subhead">
      <h4>{children}</h4>
      <span className="line" />
    </div>
  );
}

export function Panel({
  children,
  style,
}: {
  children: ReactNode;
  style?: CSSProperties;
}) {
  return (
    <div className="panel" style={{ padding: 16, ...style }}>
      {children}
    </div>
  );
}

export function SectionHead({ title, lead }: { title: string; lead?: ReactNode }) {
  return (
    <div>
      <h2 className="sec">{title}</h2>
      {lead ? <p className="sec-lead">{lead}</p> : null}
    </div>
  );
}

import { emptyConditions, normalizeHistoryCondition } from "../model/normalize";
import type { Conditions, HistoryCondition } from "../model/types";
import { useDerived } from "../state/derived";
import { commit } from "../state/store";
import { ChipsEditor, Field, NumInput } from "./controls";
import { RangeMapEditor } from "./MapEditor";

/** Тумблеры периодов: allowedPeriods у состояния и periods в условии. */
export function PeriodChecks({
  selected,
  onChange,
}: {
  selected: string[];
  onChange: (next: string[]) => void;
}) {
  const { periodIds } = useDerived();
  if (!periodIds.length) {
    return (
      <div className="chips">
        <span className="empty">нет периодов</span>
      </div>
    );
  }
  return (
    <div className="chips">
      {periodIds.map((p) => {
        const on = selected.includes(p);
        return (
          <button
            key={p}
            className={`chip${on ? " tag" : ""}`}
            style={{ cursor: "pointer" }}
            onClick={() =>
              onChange(on ? selected.filter((x) => x !== p) : [...selected, p])
            }
          >
            {on ? `✓ ${p}` : p}
          </button>
        );
      })}
    </div>
  );
}

export function ConditionsEditor({
  conditions,
  onSet,
  title = "условия перехода",
  removable = true,
}: {
  conditions: Conditions | undefined;
  /** добавление и удаление блока целиком */
  onSet: (next: Conditions | undefined) => void;
  title?: string;
  /**
   * У перехода блок условий необязателен и его можно убрать целиком.
   * У модификатора веса condition — обязательное поле, поэтому там крестик
   * только очищает содержимое.
   */
  removable?: boolean;
}) {
  const { tags, atmosphereKeys } = useDerived();

  // Признак «блок раскрыт» — именно наличие объекта, а не его непустота.
  // Иначе только что добавленный пустой блок мгновенно считался отсутствующим
  // и кнопка «+ условия» выглядела нерабочей.
  if (!conditions) {
    return (
      <div>
        <button className="mini" onClick={() => onSet(emptyConditions())}>
          + условия
        </button>
      </div>
    );
  }

  return (
    <div className="cond">
      <div className="thead">
        <span className="tinyhead">{title}</span>
        <button
          className="iconbtn"
          style={{ marginLeft: "auto" }}
          title={removable ? "Убрать все условия" : "Очистить условие"}
          onClick={() => onSet(removable ? undefined : emptyConditions())}
        >
          ×
        </button>
      </div>

      <Field label="периоды">
        <PeriodChecks
          selected={conditions.periods}
          onChange={(next) => {
            conditions.periods = next;
            commit();
          }}
        />
      </Field>

      <Field label="любой из тегов текущей погоды">
        <ChipsEditor
          values={conditions.currentAnyTags}
          suggestions={tags}
          onChange={(next) => {
            conditions.currentAnyTags = next;
            commit();
          }}
        />
      </Field>

      <Field label="все теги текущей погоды">
        <ChipsEditor
          values={conditions.currentAllTags}
          suggestions={tags}
          onChange={(next) => {
            conditions.currentAllTags = next;
            commit();
          }}
        />
      </Field>

      <Field label="атмосфера в диапазоне">
        <RangeMapEditor
          value={conditions.atmosphere}
          keyOptions={atmosphereKeys}
          onReplace={(next) => {
            conditions.atmosphere = next;
            commit();
          }}
        />
      </Field>

      <HistoryConditionEditor
        title="недавно была погода"
        value={conditions.recentWeather}
        onSet={(next) => {
          conditions.recentWeather = next;
          commit();
        }}
      />
      <HistoryConditionEditor
        title="недавно НЕ было погоды"
        value={conditions.absentRecentWeather}
        onSet={(next) => {
          conditions.absentRecentWeather = next;
          commit();
        }}
      />
    </div>
  );
}

function HistoryConditionEditor({
  title,
  value,
  onSet,
}: {
  title: string;
  value: HistoryCondition | undefined;
  onSet: (next: HistoryCondition | undefined) => void;
}) {
  const { tags, stateIds } = useDerived();

  if (!value) {
    return (
      <div>
        <button
          className="mini"
          onClick={() => onSet(normalizeHistoryCondition({ withinTicks: 4 }))}
        >
          + {title}
        </button>
      </div>
    );
  }

  return (
    <div className="trow" style={{ marginBottom: 0 }}>
      <div className="thead">
        <span className="tinyhead">{title}</span>
        <button
          className="iconbtn"
          style={{ marginLeft: "auto" }}
          onClick={() => onSet(undefined)}
        >
          ×
        </button>
      </div>
      <div className="cond" style={{ borderTop: 0, paddingTop: 6 }}>
        <Field
          label="в пределах тиков"
          hint="окно истории; движок смотрит назад от текущего состояния"
        >
          <NumInput
            value={value.withinTicks}
            min={1}
            integer
            width="90px"
            onChange={(v) => {
              value.withinTicks = Math.max(1, v);
              commit();
            }}
          />
        </Field>
        <Field label="любой из тегов">
          <ChipsEditor
            values={value.anyTags}
            suggestions={tags}
            onChange={(next) => {
              value.anyTags = next;
              commit();
            }}
          />
        </Field>
        <Field label="все теги">
          <ChipsEditor
            values={value.allTags}
            suggestions={tags}
            onChange={(next) => {
              value.allTags = next;
              commit();
            }}
          />
        </Field>
        <Field label="любое из состояний">
          <ChipsEditor
            values={value.anyStates}
            suggestions={stateIds}
            onChange={(next) => {
              value.anyStates = next;
              commit();
            }}
          />
        </Field>
      </div>
    </div>
  );
}

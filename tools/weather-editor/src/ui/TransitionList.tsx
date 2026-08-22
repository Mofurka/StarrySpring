import { emptyConditions } from "../model/normalize";
import type { Transition } from "../model/types";
import { useDerived } from "../state/derived";
import { commit } from "../state/store";
import { ConditionsEditor } from "./ConditionsEditor";
import { NumInput, SelectInput } from "./controls";

export function TransitionList({ list }: { list: Transition[] }) {
  const { stateIds } = useDerived();
  const maxWeight = Math.max(1, ...list.map((t) => t.weight));

  return (
    <div>
      {list.length === 0 ? <div className="empty">нет переходов</div> : null}

      {list.map((t, i) => (
        <div className="trow" key={i}>
          <div className="thead">
            <span className="tinyhead">→</span>
            <SelectInput
              value={t.to}
              options={stateIds}
              blank="- выбрать -"
              width="170px"
              onChange={(v) => {
                t.to = v;
                commit();
              }}
            />
            <span className="tinyhead">вес</span>
            <NumInput
              value={t.weight}
              min={0}
              width="80px"
              onChange={(v) => {
                t.weight = v;
                commit();
              }}
            />
            <span
              className="wbar"
              style={{
                flex: 1,
                maxWidth: 120,
                width: Math.max(3, (t.weight / maxWeight) * 100),
              }}
            />
            <button
              className="iconbtn"
              title="Удалить переход"
              style={{ marginLeft: "auto" }}
              onClick={() => {
                list.splice(i, 1);
                commit();
              }}
            >
              ×
            </button>
          </div>

          <ConditionsEditor
            conditions={t.conditions}
            onSet={(next) => {
              if (next) t.conditions = next;
              else delete t.conditions;
              commit();
            }}
          />

          <div className="subhead">
            <span className="line" />
            <h4>модификаторы</h4>
            <span className="line" />
          </div>

          <div>
            {t.modifiers.map((m, mi) => (
              <div className="trow" key={mi} style={{ background: "var(--panel)" }}>
                <div className="thead">
                  <span className="tinyhead">×</span>
                  <NumInput
                    value={m.multiplier}
                    min={0}
                    width="80px"
                    onChange={(v) => {
                      m.multiplier = v;
                      commit();
                    }}
                  />
                  <span className="hint">
                    множитель веса при условии; 0 полностью гасит переход
                  </span>
                  <button
                    className="iconbtn"
                    style={{ marginLeft: "auto" }}
                    onClick={() => {
                      t.modifiers.splice(mi, 1);
                      commit();
                    }}
                  >
                    ×
                  </button>
                </div>
                <div style={{ marginTop: 8 }}>
                  <ConditionsEditor
                    title="условие модификатора"
                    conditions={m.condition}
                    removable={false}
                    onSet={(next) => {
                      m.condition = next ?? emptyConditions();
                      commit();
                    }}
                  />
                </div>
              </div>
            ))}
            <button
              className="mini"
              onClick={() => {
                t.modifiers.push({ condition: emptyConditions(), multiplier: 1 });
                commit();
              }}
            >
              + модификатор
            </button>
          </div>
        </div>
      ))}

      <button
        className="mini"
        style={{ marginTop: 4 }}
        onClick={() => {
          list.push({ to: stateIds[0] ?? "", weight: 10, modifiers: [] });
          commit();
        }}
      >
        + переход
      </button>
    </div>
  );
}

import { useDerived } from "../../state/derived";
import { commit } from "../../state/store";
import { Field, NumInput, Panel, SectionHead, SelectInput } from "../controls";
import { MapEditor, RangeMapEditor } from "../MapEditor";

export function SettingsTab() {
  const { model, stateIds } = useDerived();
  const s = model.settings;

  return (
    <>
      <SectionHead
        title="Настройки"
        lead="Глобальные параметры симуляции: стартовая точка, память истории и границы атмосферы."
      />

      <Panel>
        <div
          className="grid"
          style={{ gridTemplateColumns: "repeat(auto-fit,minmax(200px,1fr))" }}
        >
          <Field
            label="состояние по умолчанию"
            hint="старт и запасной вариант; должно быть разрешено во всех периодах"
          >
            <SelectInput
              value={s.defaultState}
              options={stateIds}
              blank="- выбрать -"
              onChange={(v) => {
                s.defaultState = v;
                commit();
              }}
            />
          </Field>

          <Field label="размер истории (тиков)" hint="окно для cooldown / recentWeather">
            <NumInput
              value={s.historySize}
              min={1}
              integer
              onChange={(v) => {
                s.historySize = Math.max(1, v);
                commit();
              }}
            />
          </Field>

          <Field
            label="множитель того же состояния"
            hint="склонность остаться в текущей погоде"
          >
            <NumInput
              value={s.sameStateMultiplier}
              min={0}
              onChange={(v) => {
                s.sameStateMultiplier = v;
                commit();
              }}
            />
          </Field>
        </div>
      </Panel>

      <Panel style={{ marginTop: 14 }}>
        <div className="eyebrow">стартовая атмосфера</div>
        <p className="sec-lead" style={{ marginTop: 4 }}>
          Значения переменных при инициализации (и после сброса состояния).
        </p>
        <MapEditor
          value={s.initialAtmosphere}
          keyPlaceholder="переменная"
          onReplace={(next) => {
            s.initialAtmosphere = next;
            commit();
          }}
        />
      </Panel>

      <Panel style={{ marginTop: 14 }}>
        <div className="eyebrow">границы атмосферы</div>
        <p className="sec-lead" style={{ marginTop: 4 }}>
          Каждое изменение атмосферы клампится к этим пределам. Ключи отсюда доступны
          в условиях переходов.
        </p>
        <RangeMapEditor
          value={s.atmosphereBounds}
          editableKeys
          addLabel="+ переменная"
          defaultRange={{ min: 0, max: 1 }}
          onReplace={(next) => {
            s.atmosphereBounds = next;
            commit();
          }}
        />
      </Panel>
    </>
  );
}

import { useState } from "react";
import { nodeFill } from "../../model/colors";
import { countStateReferences, deleteState, renameState } from "../../model/refactor";
import type { StateDefinition } from "../../model/types";
import { useDerived } from "../../state/derived";
import { commit } from "../../state/store";
import { ChipsEditor, Field, NumInput, SectionHead, Subhead, TextInput } from "../controls";
import { PeriodChecks } from "../ConditionsEditor";
import { useFeedback } from "../Feedback";
import { MapEditor } from "../MapEditor";
import { PresentationsEditor } from "../PresentationsEditor";
import { TransitionList } from "../TransitionList";

const ID_RE = /^[a-z0-9_]+$/i;

export function StatesTab() {
  const { model, stateIds } = useDerived();
  const { askText, askConfirm, toast } = useFeedback();
  const [filter, setFilter] = useState("");
  // Раскрытость карточек живёт здесь, а не в CSS-классе узла: раньше любое
  // изменение модели перерисовывало вкладку и схлопывало открытую карточку —
  // достаточно было отметить период внутри неё.
  const [open, setOpen] = useState<ReadonlySet<string>>(new Set());

  const toggle = (id: string) => {
    setOpen((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const query = filter.trim().toLowerCase();
  const visible = stateIds.filter((id) => {
    if (!query) return true;
    const d = model.states[id];
    return `${id} ${d.tags.join(" ")} ${d.serverWeather}`.toLowerCase().includes(query);
  });

  const addState = async () => {
    const raw = await askText("id нового состояния (латиница, цифры, _)", "");
    if (!raw) return;
    const id = raw.trim();
    if (!ID_RE.test(id)) {
      toast("id: только латиница, цифры, _");
      return;
    }
    if (id in model.states) {
      toast("такое состояние уже есть");
      return;
    }
    model.states[id] = {
      serverWeather: "",
      tags: [],
      allowedPeriods: [...Object.keys(model.periods)],
      cooldownTicks: 0,
      duration: { minTicks: 1, maxTicks: 2 },
      atmosphereChanges: {},
      transitions: [],
      presentations: {},
    };
    commit();
    setOpen((prev) => new Set(prev).add(id));
    toast("Заполните serverWeather — без него сервер не загрузит конфиг");
  };

  const rename = async (id: string) => {
    const raw = await askText(`Новый id для «${id}»`, id);
    if (!raw) return;
    const next = raw.trim();
    if (next === id) return;
    if (!ID_RE.test(next)) {
      toast("id: только латиница, цифры, _");
      return;
    }
    if (next in model.states) {
      toast("такое состояние уже есть");
      return;
    }
    renameState(model, id, next);
    commit();
    setOpen((prev) => {
      const updated = new Set(prev);
      if (updated.delete(id)) updated.add(next);
      return updated;
    });
    toast("Переименовано: переходы, anyStates и defaultState обновлены");
  };

  const remove = async (id: string) => {
    const refs = countStateReferences(model, id);
    const parts: string[] = [];
    if (refs.transitions) parts.push(`${refs.transitions} переходов ведут сюда`);
    if (refs.historyConditions) parts.push(`${refs.historyConditions} условий ссылаются в anyStates`);
    if (refs.isDefault) parts.push("это состояние по умолчанию");

    const ok = await askConfirm(
      `Удалить состояние «${id}»?`,
      parts.length
        ? `${parts.join("; ")}. Эти ссылки останутся и станут ошибками валидации — перенаправьте их вручную.`
        : "На него никто не ссылается.",
    );
    if (!ok) return;
    deleteState(model, id);
    commit();
    setOpen((prev) => {
      const updated = new Set(prev);
      updated.delete(id);
      return updated;
    });
    toast("Состояние удалено");
  };

  return (
    <>
      <div
        className="row"
        style={{ justifyContent: "space-between", alignItems: "center" }}
      >
        <SectionHead
          title="Состояния погоды"
          lead={`${stateIds.length} состояний. Клик по карточке раскрывает редактор.`}
        />
        <button className="tbtn primary" onClick={() => void addState()}>
          + состояние
        </button>
      </div>

      <input
        type="text"
        placeholder="фильтр по id / тегу / serverWeather…"
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        style={{ margin: "14px 0", width: "100%", maxWidth: 340 }}
      />

      {visible.length === 0 ? <div className="empty">ничего не найдено</div> : null}

      {visible.map((id) => (
        <StateCard
          key={id}
          id={id}
          state={model.states[id]}
          open={open.has(id)}
          onToggle={() => toggle(id)}
          onRename={() => void rename(id)}
          onDelete={() => void remove(id)}
        />
      ))}
    </>
  );
}

function StateCard({
  id,
  state: d,
  open,
  onToggle,
  onRename,
  onDelete,
}: {
  id: string;
  state: StateDefinition;
  open: boolean;
  onToggle: () => void;
  onRename: () => void;
  onDelete: () => void;
}) {
  const { tags, atmosphereKeys } = useDerived();
  const fill = nodeFill(d);

  return (
    <div className={`card${open ? " open" : ""}`}>
      <div className="card-head" onClick={onToggle}>
        <span className="stripe" style={{ background: fill }} />
        <div className="card-title">
          <span className="id">{id}</span>
          <span className="meta">
            {d.serverWeather || "—"} · {d.tags.join(", ") || "без тегов"} ·{" "}
            {d.transitions.length} перех.
          </span>
        </div>
        {d.tags.includes("dangerous") ? <span className="pill danger">опасно</span> : null}
        {d.tags.includes("special") ? <span className="pill special">особое</span> : null}
        <span className="chev">▶</span>
      </div>

      {open ? (
        <div className="card-body">
          <div
            className="grid"
            style={{
              gridTemplateColumns: "repeat(auto-fit,minmax(180px,1fr))",
              marginTop: 12,
            }}
          >
            <Field label="id состояния" hint="каскадно обновит ссылки">
              <button className="mini" onClick={onRename}>
                переименовать «{id}»
              </button>
            </Field>

            <Field label="serverWeather (код для игры)" hint="форсится на сервере через RCON">
              <TextInput
                value={d.serverWeather}
                onChange={(v) => {
                  d.serverWeather = v;
                  commit();
                }}
              />
            </Field>

            <Field label="cooldown (тиков)" hint="пауза перед повторным входом; продление текущей погоды им не блокируется">
              <NumInput
                value={d.cooldownTicks}
                min={0}
                integer
                onChange={(v) => {
                  d.cooldownTicks = Math.max(0, v);
                  commit();
                }}
              />
            </Field>

            <Field label="длительность мин/макс">
              <div className="row" style={{ flexWrap: "nowrap" }}>
                <NumInput
                  value={d.duration.minTicks}
                  min={1}
                  integer
                  width="70px"
                  onChange={(v) => {
                    d.duration.minTicks = Math.max(1, v);
                    commit();
                  }}
                />
                <NumInput
                  value={d.duration.maxTicks}
                  min={1}
                  integer
                  width="70px"
                  onChange={(v) => {
                    d.duration.maxTicks = Math.max(1, v);
                    commit();
                  }}
                />
              </div>
            </Field>
          </div>

          <Subhead>теги</Subhead>
          <ChipsEditor
            values={d.tags}
            tag
            suggestions={tags}
            onChange={(next) => {
              d.tags = next;
              commit();
            }}
          />

          <Subhead>разрешённые периоды</Subhead>
          <PeriodChecks
            selected={d.allowedPeriods}
            onChange={(next) => {
              d.allowedPeriods = next;
              commit();
            }}
          />

          <Subhead>изменения атмосферы за тик</Subhead>
          <MapEditor
            value={d.atmosphereChanges}
            keyOptions={atmosphereKeys}
            onReplace={(next) => {
              d.atmosphereChanges = next;
              commit();
            }}
          />

          <Subhead>переходы</Subhead>
          <TransitionList list={d.transitions} />

          <Subhead>презентации</Subhead>
          <PresentationsEditor state={d} />

          <div style={{ marginTop: 18, textAlign: "right" }}>
            <button
              className="tbtn ghost"
              style={{ color: "var(--crit)", borderColor: "var(--crit)" }}
              onClick={onDelete}
            >
              Удалить состояние
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}

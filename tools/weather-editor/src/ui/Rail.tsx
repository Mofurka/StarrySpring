import type { TabId } from "../model/types";
import type { ValidationResult } from "../model/validate";
import { useDerived } from "../state/derived";

const TABS: Array<[TabId, string]> = [
  ["graph", "Граф"],
  ["settings", "Настройки"],
  ["periods", "Периоды"],
  ["states", "Состояния"],
  ["global", "Глобальные"],
  ["sim", "Симуляция"],
  ["json", "JSON"],
];

export function Rail({
  tab,
  onTab,
  validation,
}: {
  tab: TabId;
  onTab: (tab: TabId) => void;
  validation: ValidationResult;
}) {
  const { model, stateIds, periodIds } = useDerived();

  const counts: Record<TabId, number | null> = {
    graph: stateIds.length,
    settings: null,
    periods: periodIds.length,
    states: stateIds.length,
    global: model.globalTransitions.length,
    sim: null,
    json: null,
  };

  const { errors, warnings } = validation;
  const clean = errors.length === 0 && warnings.length === 0;

  return (
    <aside className="rail">
      <nav>
        {TABS.map(([id, label]) => (
          <a
            key={id}
            href="#"
            className={id === tab ? "active" : ""}
            onClick={(e) => {
              e.preventDefault();
              onTab(id);
              window.scrollTo(0, 0);
            }}
          >
            <span>{label}</span>
            {counts[id] != null ? <span className="c num">{counts[id]}</span> : null}
          </a>
        ))}
      </nav>

      <div className="eyebrow">Проверка</div>
      <div className="vlist">
        {clean ? <div className="vclean">Конфигурация валидна ✓</div> : null}
        {errors.slice(0, 40).map((issue, i) => (
          <div
            key={`e${i}`}
            className="vitem err"
            style={{ cursor: "pointer" }}
            onClick={() => onTab(issue.tab)}
          >
            <span className="k" />
            <span>{issue.message}</span>
          </div>
        ))}
        {warnings.slice(0, 40).map((issue, i) => (
          <div
            key={`w${i}`}
            className="vitem warn"
            style={{ cursor: "pointer" }}
            onClick={() => onTab(issue.tab)}
          >
            <span className="k" />
            <span>{issue.message}</span>
          </div>
        ))}
      </div>
    </aside>
  );
}

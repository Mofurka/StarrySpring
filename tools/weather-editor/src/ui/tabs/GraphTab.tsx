import { useMemo, useState } from "react";
import { luminance, nodeFill } from "../../model/colors";
import { hasConditions } from "../../model/normalize";
import type { StateDefinition, WeatherModel } from "../../model/types";
import { useDerived } from "../../state/derived";
import { SectionHead } from "../controls";

type Filter = "all" | "clear" | "cloudy" | "wet" | "special";

const FILTERS: Array<[Filter, string]> = [
  ["all", "все"],
  ["clear", "ясные"],
  ["cloudy", "облачные"],
  ["wet", "осадки"],
  ["special", "особые"],
];

const GLOBAL_COLOR = "#c98a2b";
const W = 1000;
const H = 760;

function passFilter(d: StateDefinition, filter: Filter): boolean {
  if (filter === "all") return true;
  const tags = d.tags;
  if (filter === "clear") return tags.includes("clear");
  if (filter === "cloudy") return tags.includes("cloudy") || tags.includes("fog");
  if (filter === "wet") return tags.includes("precipitation") || tags.includes("wet");
  return tags.includes("special");
}

interface Tip {
  id: string;
  x: number;
  y: number;
}

export function GraphTab() {
  const { model, stateIds } = useDerived();
  const [filter, setFilter] = useState<Filter>("all");
  const [showAllEdges, setShowAllEdges] = useState(false);
  const [selected, setSelected] = useState<string | null>(null);
  const [tip, setTip] = useState<Tip | null>(null);

  const ids = useMemo(
    () => stateIds.filter((id) => passFilter(model.states[id], filter)),
    [stateIds, model, filter],
  );

  const positions = useMemo(() => {
    const cx = W / 2;
    const cy = H / 2 + 6;
    const r = Math.min(W, H) / 2 - 120;
    const out: Record<string, { x: number; y: number }> = {};
    ids.forEach((id, i) => {
      const angle = -Math.PI / 2 + (i / ids.length) * 2 * Math.PI;
      out[id] = { x: cx + r * Math.cos(angle), y: cy + r * Math.sin(angle) };
    });
    return out;
  }, [ids]);

  const sel = selected && positions[selected] ? selected : null;
  const edgeCount =
    model.globalTransitions.length +
    stateIds.reduce((n, id) => n + model.states[id].transitions.length, 0);

  const { inSet, outSet } = useMemo(() => {
    const incoming = new Set<string>();
    const outgoing = new Set<string>();
    if (sel) {
      for (const t of model.states[sel].transitions) {
        if (positions[t.to]) outgoing.add(t.to);
      }
      for (const id of ids) {
        for (const t of model.states[id].transitions) {
          if (t.to === sel) incoming.add(id);
        }
      }
      for (const t of model.globalTransitions) {
        if (positions[t.to]) outgoing.add(t.to);
      }
    }
    return { inSet: incoming, outSet: outgoing };
  }, [sel, ids, model, positions]);

  const edges: Array<{
    key: string;
    d: string;
    width: number;
    kind: "out" | "in" | "glob" | "all";
    dashed: boolean;
  }> = [];

  const pushEdge = (
    from: string,
    to: string,
    weight: number,
    dashed: boolean,
    kind: "out" | "in" | "glob" | "all",
    key: string,
  ) => {
    const a = positions[from];
    const b = positions[to];
    if (!a || !b) return;
    const cx = W / 2;
    const cy = H / 2 + 6;
    const mx = (a.x + b.x) / 2;
    const my = (a.y + b.y) / 2;
    const qx = mx + (cx - mx) * 0.28;
    const qy = my + (cy - my) * 0.28;
    edges.push({
      key,
      d: `M${a.x},${a.y} Q${qx},${qy} ${b.x},${b.y}`,
      width: Math.max(0.7, Math.min(6, Math.sqrt(weight))),
      kind,
      dashed,
    });
  };

  if (sel) {
    model.states[sel].transitions.forEach((t, i) =>
      pushEdge(sel, t.to, t.weight, hasConditions(t), "out", `out-${i}`),
    );
    for (const id of ids) {
      model.states[id].transitions.forEach((t, i) => {
        if (t.to === sel) pushEdge(id, sel, t.weight, hasConditions(t), "in", `in-${id}-${i}`);
      });
    }
    model.globalTransitions.forEach((t, i) =>
      pushEdge(sel, t.to, t.weight, hasConditions(t), "glob", `glob-${i}`),
    );
  } else if (showAllEdges) {
    for (const id of ids) {
      model.states[id].transitions.forEach((t, i) =>
        pushEdge(id, t.to, t.weight, hasConditions(t), "all", `all-${id}-${i}`),
      );
    }
  }

  const edgeStyle = (kind: "out" | "in" | "glob" | "all") => {
    if (kind === "out") return { stroke: "var(--accent)", strokeOpacity: 0.9, marker: "url(#arrow)" };
    if (kind === "in") return { stroke: "var(--muted)", strokeOpacity: 0.8, marker: "url(#arrow-mut)" };
    if (kind === "glob") return { stroke: GLOBAL_COLOR, strokeOpacity: 0.9, marker: "url(#arrow-glob)" };
    return { stroke: "var(--muted)", strokeOpacity: 0.22, marker: undefined };
  };

  return (
    <>
      <SectionHead
        title="Граф переходов"
        lead="Полу-марковская модель: узлы — состояния (цвет берётся из тегов), дуги — переходы, толщина — вес. Кликните узел, чтобы выделить его окружение; ещё раз — сбросить."
      />

      <div className="gtoolbar">
        <div className="seg">
          {FILTERS.map(([key, label]) => (
            <button
              key={key}
              className={filter === key ? "on" : ""}
              onClick={() => setFilter(key)}
            >
              {label}
            </button>
          ))}
        </div>
        <button className="tbtn ghost" onClick={() => setShowAllEdges((v) => !v)}>
          {showAllEdges ? "скрыть все дуги" : "показать все дуги"}
        </button>
        {sel ? (
          <button className="tbtn ghost" onClick={() => setSelected(null)}>
            сбросить выбор ({sel})
          </button>
        ) : null}
      </div>

      <div className="graphwrap">
        <svg
          viewBox={`0 0 ${W} ${H}`}
          preserveAspectRatio="xMidYMid meet"
          role="img"
          aria-label="Граф переходов погоды"
          onClick={() => setSelected(null)}
        >
          <defs>
            <Marker id="arrow" color="var(--accent)" />
            <Marker id="arrow-mut" color="var(--muted)" />
            <Marker id="arrow-glob" color={GLOBAL_COLOR} />
          </defs>

          <g>
            {edges.map((e) => {
              const style = edgeStyle(e.kind);
              return (
                <path
                  key={e.key}
                  d={e.d}
                  fill="none"
                  strokeWidth={e.width}
                  strokeLinecap="round"
                  strokeDasharray={e.dashed ? "2 5" : undefined}
                  markerEnd={style.marker}
                  style={{ stroke: style.stroke, strokeOpacity: style.strokeOpacity }}
                />
              );
            })}
          </g>

          <g>
            {ids.map((id) => {
              const d = model.states[id];
              const p = positions[id];
              const fill = nodeFill(d);
              const isSel = id === sel;
              const dim = sel && !isSel && !inSet.has(id) && !outSet.has(id);
              const right = p.x >= W / 2;
              return (
                <g
                  key={id}
                  className="gnode"
                  transform={`translate(${p.x},${p.y})`}
                  opacity={dim ? 0.28 : 1}
                  onClick={(e) => {
                    e.stopPropagation();
                    setSelected((prev) => (prev === id ? null : id));
                  }}
                  onMouseMove={(e) => {
                    const box = e.currentTarget.ownerSVGElement?.getBoundingClientRect();
                    if (!box) return;
                    setTip({ id, x: e.clientX - box.left, y: e.clientY - box.top });
                  }}
                  onMouseLeave={() => setTip(null)}
                >
                  <circle
                    r={isSel ? 18 : 13}
                    fill={fill}
                    strokeWidth={isSel ? 3 : 1.5}
                    style={{
                      stroke: isSel
                        ? "var(--accent)"
                        : luminance(fill) > 0.7
                          ? "rgba(0,0,0,.35)"
                          : "rgba(255,255,255,.55)",
                    }}
                  />
                  <text
                    x={right ? 20 : -20}
                    y={5}
                    textAnchor={right ? "start" : "end"}
                    style={{ fill: "var(--ink)" }}
                  >
                    {id}
                  </text>
                </g>
              );
            })}
          </g>

          {sel ? (
            <g transform={`translate(${W / 2},${H / 2 + 6})`}>
              <circle r={7} fill={GLOBAL_COLOR} opacity={0.5} />
              <text y={4} textAnchor="middle" fontSize={13} style={{ fill: "var(--muted)" }}>
                ∀
              </text>
            </g>
          ) : null}
        </svg>

        {tip ? <GraphTip model={model} tip={tip} /> : null}

        <div className="glegend">
          <span>
            <span className="swatch" style={{ background: "var(--accent)" }} />
            исходящие
          </span>
          <span>
            <span className="swatch" style={{ background: "var(--muted)" }} />
            входящие
          </span>
          <span>
            <span className="swatch" style={{ background: GLOBAL_COLOR }} />
            глобальные (∀)
          </span>
          <span>пунктир — переход с условиями</span>
          <span className="mono">
            {stateIds.length} состояний · {edgeCount} переходов
          </span>
        </div>
      </div>

      {sel ? <NeighborPanel model={model} selected={sel} onSelect={setSelected} /> : null}
    </>
  );
}

function Marker({ id, color }: { id: string; color: string }) {
  return (
    <marker
      id={id}
      viewBox="0 0 10 10"
      refX="9"
      refY="5"
      markerWidth="7"
      markerHeight="7"
      orient="auto-start-reverse"
    >
      <path d="M0,0 L10,5 L0,10 z" style={{ fill: color }} />
    </marker>
  );
}

function GraphTip({ model, tip }: { model: WeatherModel; tip: Tip }) {
  const d = model.states[tip.id];
  if (!d) return null;
  const x = Math.max(6, Math.min(tip.x + 14, 1000));
  return (
    <div className="gtip show" style={{ left: x, top: tip.y + 14 }}>
      <div className="t">{tip.id}</div>
      <div>
        {d.serverWeather || "—"} · {d.tags.join(", ") || "без тегов"}
      </div>
      <div style={{ color: "#9fb4c2", marginTop: 3 }}>
        периоды: {d.allowedPeriods.join(", ") || "любые"}
      </div>
      <div style={{ color: "#9fb4c2" }}>
        длит. {d.duration.minTicks}–{d.duration.maxTicks}, cooldown {d.cooldownTicks}
      </div>
      <div style={{ color: "#9fb4c2" }}>переходов: {d.transitions.length}</div>
    </div>
  );
}

function NeighborPanel({
  model,
  selected,
  onSelect,
}: {
  model: WeatherModel;
  selected: string;
  onSelect: (id: string) => void;
}) {
  const d = model.states[selected];
  const outgoing = d.transitions.map((t) => ({
    id: t.to,
    weight: t.weight,
    cond: hasConditions(t),
  }));
  const incoming: Array<{ id: string; weight: number; cond: boolean }> = [];
  for (const id of Object.keys(model.states)) {
    for (const t of model.states[id].transitions) {
      if (t.to === selected) incoming.push({ id, weight: t.weight, cond: hasConditions(t) });
    }
  }
  const global = model.globalTransitions.filter((t) => t.to === selected);

  const line = (
    arr: Array<{ id: string; weight: number; cond: boolean }>,
    label: string,
  ) => (
    <div>
      <div className="tinyhead" style={{ marginBottom: 6 }}>
        {label}
      </div>
      <div className="chips">
        {arr.length ? (
          arr.map((x, i) => (
            <span
              key={`${x.id}-${i}`}
              className="chip"
              style={{ cursor: "pointer" }}
              onClick={() => onSelect(x.id)}
            >
              {x.id}
              <span className="mono" style={{ color: "var(--muted)" }}>
                ·{x.weight}
                {x.cond ? " ⚡" : ""}
              </span>
            </span>
          ))
        ) : (
          <span className="empty">нет</span>
        )}
      </div>
    </div>
  );

  return (
    <div className="panel" style={{ padding: 16, marginTop: 14 }}>
      <div className="eyebrow">окружение · {selected}</div>
      <div
        className="grid"
        style={{ gridTemplateColumns: "1fr 1fr", gap: 18, marginTop: 10 }}
      >
        {line(outgoing, "→ исходящие")}
        {line(incoming, "← входящие")}
      </div>
      {global.length ? (
        <div style={{ marginTop: 12 }}>
          <span className="tinyhead">также доступно глобально (∀): </span>
          <span className="mono">
            {global
              .map((t) => `вес ${t.weight}${hasConditions(t) ? " ⚡" : ""}`)
              .join(", ")}
          </span>
        </div>
      ) : null}
    </div>
  );
}

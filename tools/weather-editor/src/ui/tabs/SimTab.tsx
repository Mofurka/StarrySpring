import { useCallback, useEffect, useState } from "react";
import { nodeFill } from "../../model/colors";
import { simulate, type AtmosphereStat, type RuleStat, type SimResult } from "../../model/engine";
import type { WeatherModel } from "../../model/types";
import { useDerived } from "../../state/derived";
import { store } from "../../state/store";
import { Field, SectionHead } from "../controls";

export function SimTab() {
  const { model } = useDerived();
  const [ticks, setTicks] = useState(30000);
  const [result, setResult] = useState<SimResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [running, setRunning] = useState(false);

  const run = useCallback((count: number) => {
    setRunning(true);
    setError(null);
    // Отдаём кадр браузеру, чтобы успел отрисоваться индикатор.
    setTimeout(() => {
      const outcome = simulate(store.get(), count);
      if (outcome.ok) {
        setResult(outcome.result);
        setError(null);
      } else {
        setResult(null);
        setError(outcome.error);
      }
      setRunning(false);
    }, 10);
  }, []);

  useEffect(() => {
    run(30000);
  }, [run]);

  return (
    <>
      <SectionHead
        title="Симуляция"
        lead="Прогон движка в браузере: 1 тик = 1 час, старт с полуночи, детерминированный seed. Порядок операций внутри тика совпадает с WeatherEngine, включая момент записи в историю — от этого зависят окна withinTicks и cooldownTicks."
      />

      <div className="gtoolbar">
        <Field label="тиков (часов)">
          <input
            type="number"
            className="num"
            value={ticks}
            step={100}
            min={1}
            style={{ width: 120 }}
            onChange={(e) => setTicks(Number(e.target.value) || 0)}
          />
        </Field>
        <button
          className="tbtn primary"
          style={{ alignSelf: "flex-end" }}
          disabled={running}
          onClick={() => {
            const n = Math.max(1, Math.min(200000, Math.round(ticks) || 30000));
            setTicks(n);
            run(n);
          }}
        >
          {running ? "считаю…" : "Прогнать"}
        </button>
        <span className="hint" style={{ alignSelf: "flex-end" }}>
          ~1 час игрового времени = 1 тик крона
        </span>
      </div>

      {error ? (
        <div className="panel" style={{ padding: 16, borderColor: "var(--crit)" }}>
          <span className="simbadge crit">нельзя симулировать</span>
          <p style={{ marginTop: 8, marginBottom: 0 }}>{error}</p>
        </div>
      ) : null}

      {!error && running ? <div className="empty">считаю…</div> : null}

      {!error && !running && result ? <SimReport model={model} result={result} /> : null}
    </>
  );
}

function SimReport({ model, result }: { model: WeatherModel; result: SimResult }) {
  const ids = Object.keys(result.timeIn).sort((a, b) => result.timeIn[b] - result.timeIn[a]);
  const seen = ids.filter((id) => result.timeIn[id] > 0).length;
  const unreachable = ids.filter(
    (id) => result.timeIn[id] === 0 && id !== model.settings.defaultState,
  ).length;
  const pinned = Object.values(result.atmosphere).filter((a) => {
    const { min, max } = a.bound;
    if (min == null || max == null || max <= min) return false;
    const f = (a.mean - min) / (max - min);
    return f <= 0.12 || f >= 0.88;
  }).length;

  const maxPct = Math.max(...ids.map((id) => result.timeIn[id])) / result.ticks * 100 || 1;

  // Порог по числу попыток обязателен: правило, которое рассматривалось
  // один раз и не прошло, — это не «мёртвое правило», а отсутствие выборки.
  const minAttempts = Math.max(30, result.ticks / 2000);
  const problems = result.rules
    .filter((r) => r.attempted >= minAttempts && r.eligible / r.attempted < 0.03)
    .sort((a, b) => a.eligible / a.attempted - b.eligible / b.attempted);
  const never = result.rules.filter((r) => r.attempted === 0);

  return (
    <>
      <div className="statrow">
        <Stat value={result.ticks.toLocaleString("ru")} label="тиков" />
        <Stat value={`${seen} / ${ids.length}`} label="погод встречено" />
        <Stat
          value={String(unreachable)}
          label="недостижимо"
          tone={unreachable ? "crit" : "ok"}
        />
        <Stat
          value={String(pinned)}
          label="переменных у границы"
          tone={pinned ? "warn" : "ok"}
        />
      </div>

      <div className="panel" style={{ padding: 16 }}>
        <div className="eyebrow">равновесие атмосферы</div>
        <p className="sec-lead" style={{ margin: "4px 0 12px" }}>
          Полоса — наблюдаемый диапазон в пределах границ, засечка — среднее. «Прижата
          к границе» = дельты несбалансированы (сумма по времени тянет в пол/потолок).
        </p>
        {Object.entries(result.atmosphere).map(([name, stat]) => (
          <AtmosphereBar key={name} name={name} stat={stat} />
        ))}
      </div>

      <div className="panel" style={{ padding: 16, marginTop: 14 }}>
        <div className="eyebrow">распределение состояний · доля времени</div>
        <div className="sbar" style={{ marginTop: 10, color: "var(--muted)" }}>
          <span className="tinyhead">состояние</span>
          <span className="tinyhead" />
          <span className="tinyhead" style={{ textAlign: "right" }}>
            % времени
          </span>
          <span className="tinyhead" style={{ textAlign: "right" }}>
            входов
          </span>
        </div>
        {ids.map((id) => {
          const pct = (result.timeIn[id] / result.ticks) * 100;
          const fill = nodeFill(model.states[id]);
          const tone = result.timeIn[id] === 0 ? "crit" : pct < 0.1 ? "warn" : null;
          return (
            <div className="sbar" key={id}>
              <span className="id">
                <span className="sw" style={{ background: fill }} />
                {id}
                {tone === "crit" ? <span className="simbadge crit">нет</span> : null}
                {tone === "warn" ? <span className="simbadge warn">редко</span> : null}
              </span>
              <div className="track">
                <div
                  className="fill"
                  style={{
                    width: `${Math.max(0.5, (pct / maxPct) * 100)}%`,
                    background: fill,
                    opacity: 0.85,
                  }}
                />
              </div>
              <span className="pct">{pct.toFixed(2)}%</span>
              <span className="pct" style={{ color: "var(--muted)" }}>
                {result.entries[id]}
              </span>
            </div>
          );
        })}
      </div>

      {problems.length ? (
        <div className="panel" style={{ padding: 16, marginTop: 14 }}>
          <div className="eyebrow">переходы, которые почти никогда не срабатывают</div>
          <p className="sec-lead" style={{ margin: "4px 0 10px" }}>
            Правило рассматривалось минимум {Math.round(minAttempts)} раз, но до розыгрыша
            веса доходило меньше 3% попыток. Проверки идут по порядку — период, кулдаун,
            условия — и колонки показывают, какая из них отсекла правило первой. Раньше
            сюда попадали только переходы с условиями, из-за чего мёртвые правила без
            условий видны не были вовсе.
          </p>
          <div className="rulerow tinyhead">
            <span>переход</span>
            <span style={{ textAlign: "right" }}>попыток</span>
            <span style={{ textAlign: "right" }}>прошло</span>
            <span style={{ textAlign: "right" }}>период</span>
            <span style={{ textAlign: "right" }}>кулдаун</span>
            <span style={{ textAlign: "right" }}>условия</span>
          </div>
          {problems.slice(0, 30).map((r) => (
            <RuleRow key={r.key} rule={r} />
          ))}
        </div>
      ) : null}

      {never.length ? (
        <div className="panel" style={{ padding: 16, marginTop: 14 }}>
          <div className="eyebrow">переходы, которые ни разу не рассматривались</div>
          <p className="sec-lead" style={{ margin: "4px 0 10px" }}>
            Движок не дошёл до этих правил: исходное состояние не встретилось за прогон
            либо всегда продлевало себя.
          </p>
          <div className="chips">
            {never.slice(0, 40).map((r) => (
              <span key={r.key} className="chip">
                {r.source} → {r.to}
                <span className="mono" style={{ color: "var(--muted)" }}>
                  ·{r.weight}
                </span>
              </span>
            ))}
          </div>
        </div>
      ) : null}
    </>
  );
}

function RuleRow({ rule }: { rule: RuleStat }) {
  const pass = rule.attempted ? (rule.eligible / rule.attempted) * 100 : 0;
  const share = (n: number) => (rule.attempted ? `${((n / rule.attempted) * 100).toFixed(0)}%` : "—");
  const dead = rule.eligible === 0;

  return (
    <div className="rulerow">
      <span className="id">
        {rule.source} → {rule.to}
        <span className="mono" style={{ color: "var(--muted)" }}>
          ·{rule.weight}
        </span>
        {dead ? <span className="simbadge crit">мёртвый</span> : null}
        {rule.hasConditions ? null : (
          <span className="simbadge warn">без условий</span>
        )}
      </span>
      <span className="pct" style={{ color: "var(--muted)" }}>
        {rule.attempted}
      </span>
      <span className="pct" style={{ color: dead ? "var(--crit)" : "var(--warn)" }}>
        {pass.toFixed(2)}%
      </span>
      <span className="pct" style={{ color: "var(--muted)" }}>
        {share(rule.blockedByPeriod)}
      </span>
      <span className="pct" style={{ color: "var(--muted)" }}>
        {share(rule.blockedByCooldown)}
      </span>
      <span className="pct" style={{ color: "var(--muted)" }}>
        {share(rule.blockedByConditions)}
      </span>
    </div>
  );
}

function Stat({
  value,
  label,
  tone,
}: {
  value: string;
  label: string;
  tone?: "ok" | "warn" | "crit";
}) {
  return (
    <div className="stat">
      <div className="v" style={tone ? { color: `var(--${tone})` } : undefined}>
        {value}
      </div>
      <div className="l">{label}</div>
    </div>
  );
}

function AtmosphereBar({ name, stat }: { name: string; stat: AtmosphereStat }) {
  const { min, max } = stat.bound;
  const bounded = min != null && max != null && max > min;
  const pct = (x: number) =>
    bounded ? Math.max(0, Math.min(100, ((x - min!) / (max! - min!)) * 100)) : 50;

  let status = "—";
  let tone: "ok" | "warn" | "crit" = "ok";
  if (bounded) {
    const f = (stat.mean - min!) / (max! - min!);
    if (f <= 0.12) {
      status = "прижата к полу";
      tone = "crit";
    } else if (f >= 0.88) {
      status = "прижата к потолку";
      tone = "crit";
    } else if (f <= 0.25) {
      status = "дрейф вниз";
      tone = "warn";
    } else if (f >= 0.75) {
      status = "дрейф вверх";
      tone = "warn";
    } else {
      status = "стабильна";
    }
  }

  const lo = pct(stat.min);
  const hi = pct(stat.max);

  return (
    <div className="abar">
      <span className="name">{name}</span>
      <div className="track">
        <div
          className="band"
          style={{
            left: `${Math.min(lo, hi)}%`,
            width: `${Math.max(1.5, Math.abs(hi - lo))}%`,
          }}
        />
        <div className="mean" style={{ left: `${pct(stat.mean)}%` }} />
      </div>
      <span className="nums">
        {stat.min.toFixed(2)} · {stat.mean.toFixed(2)} · {stat.max.toFixed(2)}
      </span>
      <span style={{ textAlign: "right" }}>
        <span className={`simbadge ${tone}`}>{status}</span>
      </span>
    </div>
  );
}

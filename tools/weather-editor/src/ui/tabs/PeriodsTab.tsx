import {hourCoverage, periodColor, periodForHour} from "../../model/periods";
import {countPeriodReferences, deletePeriod, renamePeriod} from "../../model/refactor";
import {useDerived} from "../../state/derived";
import {commit} from "../../state/store";
import {KeyInput, NumInput, SectionHead, TextInput} from "../controls";
import {useFeedback} from "../Feedback";

export function PeriodsTab() {
    const {model, periodIds} = useDerived();
    const {askConfirm, toast} = useFeedback();
    const cover = hourCoverage(model);

    const remove = async (id: string) => {
        const refs = countPeriodReferences(model, id);
        const lines: string[] = [];
        if (refs.allowedPeriods) lines.push(`${refs.allowedPeriods} состояний потеряют его в allowedPeriods`);
        if (refs.presentationBlocks) {
            lines.push(
                `будет удалено ${refs.presentationBlocks} блоков презентаций (${refs.presentationVariants} вариантов текста)`,
            );
        }
        if (refs.conditions) lines.push(`${refs.conditions} условий перехода потеряют ссылку на него`);

        const ok = await askConfirm(
            `Удалить период «${id}»?`,
            lines.length
                ? `Ссылки будут вычищены каскадно: ${lines.join("; ")}.`
                : "На него никто не ссылается.",
            "Удалить",
        );
        if (!ok) return;
        deletePeriod(model, id);
        commit();
        toast("Период удалён вместе со ссылками");
    };

    return (
        <>
            <SectionHead
                title="Периоды суток"
                lead="Каждый час 0–23 должен принадлежать ровно одному периоду. Диапазон может переходить через полночь (fromHour > toHour)."
            />

            <div className="panel" style={{padding: 14}}>
                <div className="eyebrow">сутки · покрытие часов</div>
                <div className="clock">
                    {Array.from({length: 24}, (_, h) => {
                        const pid = periodForHour(model, h);
                        const ok = cover[h] === 1;
                        return (
                            <div
                                key={h}
                                className={`seg-h${ok ? "" : " gap"}`}
                                style={ok && pid ? {background: periodColor(model, pid)} : undefined}
                                title={`${h}:00 - ${pid ?? "НЕ ПОКРЫТ"}${cover[h] > 1 ? " (пересечение!)" : ""}`}
                            >
                                {h % 2 === 0 ? h : ""}
                            </div>
                        );
                    })}
                </div>

                <div style={{overflowX: "auto", marginTop: 14}}>
                    <table className="periods">
                        <thead>
                        <tr>
                            <th/>
                            <th>период (id)</th>
                            <th>с часа</th>
                            <th>до часа</th>
                            <th>Discord-канал</th>
                            <th/>
                        </tr>
                        </thead>
                        <tbody>
                        {periodIds.map((id) => {
                            const p = model.periods[id];
                            return (
                                <tr key={id}>
                                    <td>
                      <span
                          className="swatch-box"
                          style={{width: 16, height: 16, background: periodColor(model, id)}}
                      />
                                    </td>
                                    <td>
                                        <KeyInput
                                            value={id}
                                            width="130px"
                                            onCommit={(next) => {
                                                if (next in model.periods) {
                                                    toast("Период с таким id уже есть");
                                                    return;
                                                }
                                                renamePeriod(model, id, next);
                                                commit();
                                                toast("Переименовано, ссылки обновлены");
                                            }}
                                        />
                                    </td>
                                    <td>
                                        <NumInput
                                            value={p.fromHour}
                                            min={0}
                                            max={23}
                                            integer
                                            width="70px"
                                            onChange={(v) => {
                                                p.fromHour = v;
                                                commit();
                                            }}
                                        />
                                    </td>
                                    <td>
                                        <NumInput
                                            value={p.toHour}
                                            min={1}
                                            max={24}
                                            integer
                                            width="70px"
                                            onChange={(v) => {
                                                p.toHour = v;
                                                commit();
                                            }}
                                        />
                                    </td>
                                    <td>
                                        <TextInput
                                            value={p.channelName}
                                            width="100%"
                                            onChange={(v) => {
                                                p.channelName = v;
                                                commit();
                                            }}
                                        />
                                    </td>
                                    <td>
                                        <button className="iconbtn" onClick={() => void remove(id)}>
                                            ×
                                        </button>
                                    </td>
                                </tr>
                            );
                        })}
                        </tbody>
                    </table>
                </div>

                <div className="row" style={{marginTop: 12}}>
                    <button
                        className="mini"
                        onClick={() => {
                            let name = "period";
                            let i = 1;
                            while (name in model.periods) name = `period${++i}`;
                            model.periods[name] = {fromHour: 0, toHour: 1, channelName: ""};
                            commit();
                        }}
                    >
                        + период
                    </button>
                </div>
            </div>
        </>
    );
}

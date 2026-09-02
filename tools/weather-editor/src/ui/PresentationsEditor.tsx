import {isValidStoredColor, normHex, toStored} from "../model/colors";
import type {Presentation, StateDefinition} from "../model/types";
import {useDerived} from "../state/derived";
import {commit} from "../state/store";
import {Field, NumInput, SelectInput, TextInput} from "./controls";
import {MapEditor} from "./MapEditor";

const NEW_PRESENTATION = (): Presentation => ({
    weight: 5,
    color: "0xffffff",
    image: "",
    text: [""],
    stats: {},
});

export function PresentationsEditor({state}: { state: StateDefinition }) {
    const {periodIds} = useDerived();
    const blocks = Object.keys(state.presentations);
    const available = [...periodIds, "*"].filter((k) => !(k in state.presentations));

    return (
        <div>
            {blocks.map((period) => {
                const list = state.presentations[period];
                return (
                    <div key={period}>
                        <div className="subhead">
                            <h4>период: {period}</h4>
                            <span className="line"/>
                            <button
                                className="mini"
                                onClick={() => {
                                    list.push(NEW_PRESENTATION());
                                    commit();
                                }}
                            >
                                + вариант
                            </button>
                            <button
                                className="iconbtn"
                                title="Удалить блок презентаций"
                                onClick={() => {
                                    delete state.presentations[period];
                                    commit();
                                }}
                            >
                                ×
                            </button>
                        </div>
                        {list.map((p, i) => (
                            <PresentationCard
                                key={i}
                                presentation={p}
                                onDelete={() => {
                                    list.splice(i, 1);
                                    commit();
                                }}
                            />
                        ))}
                        {list.length === 0 ? <div className="empty">нет вариантов</div> : null}
                    </div>
                );
            })}

            <div className="row" style={{marginTop: 8}}>
                <SelectInput
                    value=""
                    options={available}
                    blank="+ период презентации"
                    onChange={(k) => {
                        if (!k) return;
                        state.presentations[k] = [NEW_PRESENTATION()];
                        commit();
                    }}
                />
            </div>
        </div>
    );
}

function PresentationCard({
                              presentation: p,
                              onDelete,
                          }: {
    presentation: Presentation;
    onDelete: () => void;
}) {
    const hex = normHex(p.color) ?? "#ffffff";
    const colorValid = isValidStoredColor(p.color);

    return (
        <div className="pcard">
            <div className="row">
                <Field label="вес">
                    <NumInput
                        value={p.weight}
                        width="80px"
                        onChange={(v) => {
                            p.weight = v;
                            commit();
                        }}
                    />
                </Field>

                <Field
                    label="цвет"
                    hint={
                        colorValid ? (
                            <span className="mono">{p.color}</span>
                        ) : (
                            <span className="mono" style={{color: "var(--crit)"}}>
                {p.color ? `«${p.color}» не 0xRRGGBB` : "цвет не задан"}
              </span>
                        )
                    }
                >
                    <div className="swatch-in">
            <span
                className="swatch-box"
                style={{
                    background: hex,
                    borderColor: colorValid ? undefined : "var(--crit)",
                }}
            />
                        <input
                            type="color"
                            value={hex}
                            style={{width: 38, height: 30, padding: 2}}
                            onChange={(e) => {
                                p.color = toStored(e.target.value);
                                commit();
                            }}
                        />
                    </div>
                </Field>

                <div style={{marginLeft: "auto"}}>
                    <button className="iconbtn" title="Удалить вариант" onClick={onDelete}>
                        ×
                    </button>
                </div>
            </div>

            <Field label="картинка (URL)">
                <div className="row" style={{flexWrap: "nowrap"}}>
                    <TextInput
                        value={p.image}
                        width="100%"
                        placeholder="https://…"
                        onChange={(v) => {
                            p.image = v;
                            commit();
                        }}
                    />
                    {p.image ? (
                        <a href={p.image} target="_blank" rel="noopener noreferrer" className="imglink">
                            открыть ↗
                        </a>
                    ) : (
                        <span className="hint">нет ссылки</span>
                    )}
                </div>
            </Field>

            <Field label="текст (варианты строк выбираются случайно)">
                <div className="textlines">
                    {p.text.map((line, li) => (
                        <div
                            className="row"
                            key={li}
                            style={{flexWrap: "nowrap", alignItems: "flex-start"}}
                        >
              <textarea
                  rows={2}
                  value={line}
                  style={{width: "100%"}}
                  onChange={(e) => {
                      p.text[li] = e.target.value;
                      commit();
                  }}
              />
                            <button
                                className="iconbtn"
                                onClick={() => {
                                    p.text.splice(li, 1);
                                    commit();
                                }}
                            >
                                ×
                            </button>
                        </div>
                    ))}
                    <button
                        className="mini"
                        onClick={() => {
                            p.text.push("");
                            commit();
                        }}
                    >
                        + строка текста
                    </button>
                </div>
            </Field>

            <Field label="статы персонажа">
                <MapEditor
                    value={p.stats}
                    integer
                    keyPlaceholder="код (WIL, DET…)"
                    onReplace={(next) => {
                        p.stats = next;
                        commit();
                    }}
                />
            </Field>
        </div>
    );
}

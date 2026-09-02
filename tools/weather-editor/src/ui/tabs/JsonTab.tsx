import {useEffect, useState} from "react";
import {normalize} from "../../model/normalize";
import {serialize} from "../../model/serialize";
import {useDerived} from "../../state/derived";
import {store} from "../../state/store";
import {SectionHead} from "../controls";
import {useFeedback} from "../Feedback";

export function JsonTab() {
    const {model} = useDerived();
    const {toast} = useFeedback();
    const text = serialize(model);
    const [draft, setDraft] = useState(text);
    const [error, setError] = useState<string | null>(null);

    // Пока пользователь не правил поле вручную, оно следует за моделью.
    const [base, setBase] = useState(text);
    useEffect(() => {
        if (base !== text) {
            setBase(text);
            setDraft((current) => (current === base ? text : current));
        }
    }, [text, base]);

    const apply = () => {
        try {
            store.replace(normalize(JSON.parse(draft)));
            setError(null);
            toast("Применено");
        } catch (e) {
            setError(`Ошибка JSON: ${(e as Error).message}`);
        }
    };

    return (
        <>
            <SectionHead
                title="JSON"
                lead="Готовый к вставке в config/plugins/irden/weather/weather-system.json. Можно вставить свой конфиг и применить."
            />

            <div className="row" style={{marginBottom: 10}}>
                <button
                    className="tbtn primary"
                    onClick={() => {
                        void navigator.clipboard
                            ?.writeText(draft)
                            .then(() => toast("Скопировано в буфер"), () => toast("Не удалось скопировать"));
                    }}
                >
                    Скопировать
                </button>
                <button
                    className="tbtn"
                    onClick={() => {
                        setDraft(text);
                        setError(null);
                        toast("Поле сброшено к текущей модели");
                    }}
                >
                    Сбросить поле
                </button>
                <button className="tbtn" onClick={apply}>
                    Применить из поля
                </button>
                {error ? (
                    <span className="hint" style={{color: "var(--crit)"}}>
            {error}
          </span>
                ) : null}
            </div>

            <div className="panel" style={{padding: 2}}>
        <textarea
            className="jsonarea"
            value={draft}
            spellCheck={false}
            onChange={(e) => setDraft(e.target.value)}
        />
            </div>
        </>
    );
}

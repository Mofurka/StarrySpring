import {useEffect, useState} from "react";
import type {TabId} from "../model/types";
import {useValidation} from "../state/derived";
import {store, useModelVersion} from "../state/store";
import {Rail} from "./Rail";
import {TopBar} from "./TopBar";
import {GlobalTab} from "./tabs/GlobalTab";
import {GraphTab} from "./tabs/GraphTab";
import {JsonTab} from "./tabs/JsonTab";
import {PeriodsTab} from "./tabs/PeriodsTab";
import {SettingsTab} from "./tabs/SettingsTab";
import {SimTab} from "./tabs/SimTab";
import {StatesTab} from "./tabs/StatesTab";

const TAB_COMPONENTS: Record<TabId, () => JSX.Element> = {
    graph: GraphTab,
    settings: SettingsTab,
    periods: PeriodsTab,
    states: StatesTab,
    global: GlobalTab,
    sim: SimTab,
    json: JsonTab,
};

export function App() {
    const [tab, setTab] = useState<TabId>("graph");
    const validation = useValidation();
    const [draftDismissed, setDraftDismissed] = useState(false);

    // Правки живут в localStorage, но случайно закрытая вкладка с несохранённым
    // в файл конфигом всё равно стоит подтверждения.
    useEffect(() => {
        const onBeforeUnload = (e: BeforeUnloadEvent) => {
            if (!store.dirty) return;
            e.preventDefault();
            e.returnValue = "";
        };
        window.addEventListener("beforeunload", onBeforeUnload);
        return () => window.removeEventListener("beforeunload", onBeforeUnload);
    }, []);

    const Tab = TAB_COMPONENTS[tab];
    const showDraftBanner = store.restoredFromDraft && !draftDismissed;

    return (
        <>
            <TopBar validation={validation}/>
            <div className="layout">
                <Rail tab={tab} onTab={setTab} validation={validation}/>
                <main className="main">
                    {showDraftBanner ? (
                        <div className="banner">
              <span>
                Восстановлен несохранённый черновик
                  {store.autosavedAt
                      ? ` от ${new Date(store.autosavedAt).toLocaleString("ru")}`
                      : ""}
                  . Чтобы начать с конфига из репозитория — «Из репозитория» в шапке.
              </span>
                            <button className="mini" onClick={() => setDraftDismissed(true)}>
                                Понятно
                            </button>
                        </div>
                    ) : null}

                    <Tab key={tab}/>
                </main>
            </div>
            <AutosaveHint/>
        </>
    );
}

function AutosaveHint() {
    useModelVersion();
    if (!store.autosavedAt) return null;
    return (
        <div className="autosave mono">
            черновик сохранён · {new Date(store.autosavedAt).toLocaleTimeString("ru")}
        </div>
    );
}

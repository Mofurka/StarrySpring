import {useRef, useState} from "react";
import {loadDefaultConfig} from "../model/defaultConfig";
import {normalize} from "../model/normalize";
import {serialize} from "../model/serialize";
import {plural, type ValidationResult} from "../model/validate";
import {
    downloadFile,
    type FileHandle,
    isAbortError,
    openFile,
    saveAs,
    supportsFileSystemAccess,
    writeTo,
} from "../state/fileAccess";
import {store, useModelVersion} from "../state/store";
import {useFeedback} from "./Feedback";

export function TopBar({validation}: { validation: ValidationResult }) {
    useModelVersion();
    const {toast, askConfirm} = useFeedback();
    const fileInput = useRef<HTMLInputElement>(null);
    const [handle, setHandle] = useState<FileHandle | null>(null);

    const {errors, warnings} = validation;

    const importText = (text: string, name?: string) => {
        try {
            store.replace(normalize(JSON.parse(text)), name);
            toast(`Загружено: ${name ?? "из буфера"}`);
            return true;
        } catch (e) {
            toast(`Ошибка JSON: ${(e as Error).message}`);
            return false;
        }
    };

    const open = async () => {
        if (!supportsFileSystemAccess) {
            fileInput.current?.click();
            return;
        }
        try {
            const picked = await openFile();
            if (!picked) return;
            if (importText(picked.text, picked.handle.name)) setHandle(picked.handle);
        } catch (e) {
            if (!isAbortError(e)) toast("Не удалось открыть файл");
        }
    };

    const save = async () => {
        const text = serialize(store.get());
        if (!supportsFileSystemAccess) {
            downloadFile(text, store.fileName || "weather-system.json");
            store.markSaved();
            toast("Файл скачан");
            return;
        }
        try {
            if (handle) {
                await writeTo(handle, text);
                store.markSaved(handle.name);
                toast(`Сохранено в ${handle.name}`);
                return;
            }
            const created = await saveAs(text, store.fileName || "weather-system.json");
            if (!created) return;
            setHandle(created);
            store.markSaved(created.name);
            toast(`Сохранено в ${created.name}`);
        } catch (e) {
            if (!isAbortError(e)) toast("Не удалось сохранить файл");
        }
    };

    const loadExample = async () => {
        const ok = await askConfirm(
            "Загрузить конфиг из репозитория?",
            "Текущие правки будут потеряны, включая автосохранённый черновик.",
            "Загрузить",
        );
        if (!ok) return;
        store.discardDraft();
        store.replace(normalize(loadDefaultConfig()), "weather-system.json");
        setHandle(null);
        toast("Загружен конфиг из репозитория");
    };

    const toggleTheme = () => {
        const root = document.documentElement;
        const current = root.getAttribute("data-theme");
        const systemDark = matchMedia("(prefers-color-scheme:dark)").matches;
        const next = current ? (current === "dark" ? "light" : "dark") : systemDark ? "light" : "dark";
        root.setAttribute("data-theme", next);
        try {
            localStorage.setItem("irden.weather-editor.theme", next);
        } catch {
            /* приватный режим */
        }
    };

    return (
        <div className="topbar">
            <div className="brand">
                <b>Редактор погоды</b>
                <span className="sub">
          {store.fileName}
                    {store.dirty ? " ·" : ""}
        </span>
                {store.dirty ? <span className="dirty" title="Есть несохранённые правки"/> : null}
            </div>

            <span className={`badge ${errors.length ? "bad" : warnings.length ? "warnish" : "ok"}`}>
        <span className="dot"/>
        <span>
          {errors.length
              ? `${errors.length} ${plural(errors.length, "ошибка", "ошибки", "ошибок")}`
              : warnings.length
                  ? `${warnings.length} ${plural(warnings.length, "замечание", "замечания", "замечаний")}`
                  : "всё чисто"}
        </span>
      </span>

            <button className="tbtn ghost" title="Тема" onClick={toggleTheme}>
                ◐
            </button>
            <button className="tbtn" onClick={() => void open()}>
                Открыть
            </button>
            <button className="tbtn" title="Загрузить конфиг из репозитория" onClick={() => void loadExample()}>
                Из репозитория
            </button>
            <button className="tbtn primary" onClick={() => void save()}>
                {supportsFileSystemAccess && handle ? "Сохранить" : "Сохранить как…"}
            </button>

            <input
                ref={fileInput}
                type="file"
                accept="application/json,.json"
                style={{display: "none"}}
                onChange={(e) => {
                    const file = e.target.files?.[0];
                    e.target.value = "";
                    if (!file) return;
                    void file.text().then((text) => importText(text, file.name));
                }}
            />
        </div>
    );
}

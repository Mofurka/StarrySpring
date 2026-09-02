/**
 * File System Access API с деградацией до «скачать / выбрать файл».
 *
 * Это закрывает основную боль прежнего редактора: правки жили только в
 * памяти вкладки, а положить их обратно в
 * config/plugins/irden/weather/weather-system.json можно было лишь через
 * «Скачать» и ручное копирование. Где API есть (Chrome, Edge), файл
 * открывается и перезаписывается на месте.
 */

interface FileSystemWritable {
    write(data: string): Promise<void>;

    close(): Promise<void>;
}

export interface FileHandle {
    name: string;

    createWritable(): Promise<FileSystemWritable>;

    getFile(): Promise<File>;
}

interface PickerWindow {
    showOpenFilePicker?: (options?: unknown) => Promise<FileHandle[]>;
    showSaveFilePicker?: (options?: unknown) => Promise<FileHandle>;
}

const picker = window as unknown as PickerWindow;

export const supportsFileSystemAccess =
    typeof picker.showOpenFilePicker === "function" &&
    typeof picker.showSaveFilePicker === "function";

const PICKER_OPTIONS = {
    types: [
        {
            description: "Конфигурация погоды",
            accept: {"application/json": [".json"]},
        },
    ],
    excludeAcceptAllOption: false,
};

/** Пользователь закрыл системный диалог — это не ошибка. */
export function isAbortError(e: unknown): boolean {
    return e instanceof DOMException && e.name === "AbortError";
}

export async function openFile(): Promise<{ handle: FileHandle; text: string } | null> {
    if (!picker.showOpenFilePicker) return null;
    const [handle] = await picker.showOpenFilePicker({...PICKER_OPTIONS, multiple: false});
    if (!handle) return null;
    const file = await handle.getFile();
    return {handle, text: await file.text()};
}

export async function saveAs(text: string, suggestedName: string): Promise<FileHandle | null> {
    if (!picker.showSaveFilePicker) return null;
    const handle = await picker.showSaveFilePicker({...PICKER_OPTIONS, suggestedName});
    await writeTo(handle, text);
    return handle;
}

export async function writeTo(handle: FileHandle, text: string): Promise<void> {
    const writable = await handle.createWritable();
    await writable.write(text);
    await writable.close();
}

/** Фолбэк: обычное скачивание в загрузки. */
export function downloadFile(text: string, fileName: string): void {
    const blob = new Blob([text], {type: "application/json"});
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName;
    document.body.append(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
}

import {createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useRef, useState,} from "react";

interface ConfirmRequest {
    kind: "confirm";
    title: string;
    message?: ReactNode;
    okLabel: string;
    resolve: (value: boolean) => void;
}

interface PromptRequest {
    kind: "prompt";
    title: string;
    initial: string;
    resolve: (value: string | null) => void;
}

type Request = ConfirmRequest | PromptRequest;

interface Feedback {
    toast: (message: string) => void;
    askText: (title: string, initial?: string) => Promise<string | null>;
    askConfirm: (title: string, message?: ReactNode, okLabel?: string) => Promise<boolean>;
}

const FeedbackContext = createContext<Feedback | null>(null);

export function useFeedback(): Feedback {
    const ctx = useContext(FeedbackContext);
    if (!ctx) throw new Error("useFeedback вне FeedbackProvider");
    return ctx;
}

export function FeedbackProvider({children}: { children: ReactNode }) {
    const [message, setMessage] = useState<string | null>(null);
    const [request, setRequest] = useState<Request | null>(null);
    const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

    const toast = useCallback((text: string) => {
        if (toastTimer.current) clearTimeout(toastTimer.current);
        setMessage(text);
        toastTimer.current = setTimeout(() => setMessage(null), 1900);
    }, []);

    const askText = useCallback(
        (title: string, initial = "") =>
            new Promise<string | null>((resolve) => {
                setRequest({kind: "prompt", title, initial, resolve});
            }),
        [],
    );

    const askConfirm = useCallback(
        (title: string, msg?: ReactNode, okLabel = "Удалить") =>
            new Promise<boolean>((resolve) => {
                setRequest({kind: "confirm", title, message: msg, okLabel, resolve});
            }),
        [],
    );

    const value = useMemo(
        () => ({toast, askText, askConfirm}),
        [toast, askText, askConfirm],
    );

    const finish = useCallback(
        (result: string | null | boolean) => {
            if (!request) return;
            if (request.kind === "prompt") request.resolve(result as string | null);
            else request.resolve(Boolean(result));
            setRequest(null);
        },
        [request],
    );

    return (
        <FeedbackContext.Provider value={value}>
            {children}
            {request ? <Modal request={request} onFinish={finish}/> : null}
            <div className={`toast${message ? " show" : ""}`}>{message}</div>
        </FeedbackContext.Provider>
    );
}

function Modal({
                   request,
                   onFinish,
               }: {
    request: Request;
    onFinish: (result: string | null | boolean) => void;
}) {
    const [draft, setDraft] = useState(request.kind === "prompt" ? request.initial : "");
    const inputRef = useRef<HTMLInputElement>(null);
    const cancelValue = request.kind === "prompt" ? null : false;

    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            if (e.key === "Escape") onFinish(cancelValue);
        };
        document.addEventListener("keydown", onKey);
        inputRef.current?.focus();
        inputRef.current?.select();
        return () => document.removeEventListener("keydown", onKey);
    }, [onFinish, cancelValue]);

    return (
        <div
            className="modal-ov show"
            onMouseDown={(e) => {
                if (e.target === e.currentTarget) onFinish(cancelValue);
            }}
        >
            <div className="modal">
                <h3>{request.title}</h3>
                {request.kind === "confirm" && request.message ? <p>{request.message}</p> : null}
                {request.kind === "prompt" ? (
                    <input
                        ref={inputRef}
                        type="text"
                        value={draft}
                        onChange={(e) => setDraft(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key === "Enter") onFinish(draft);
                        }}
                    />
                ) : null}
                <div className="actions">
                    <button className="tbtn ghost" onClick={() => onFinish(cancelValue)}>
                        Отмена
                    </button>
                    <button
                        className="tbtn primary"
                        style={
                            request.kind === "confirm"
                                ? {background: "var(--crit)", borderColor: "var(--crit)"}
                                : undefined
                        }
                        onClick={() => onFinish(request.kind === "prompt" ? draft : true)}
                    >
                        {request.kind === "prompt" ? "OK" : request.okLabel}
                    </button>
                </div>
            </div>
        </div>
    );
}

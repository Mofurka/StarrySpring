import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { App } from "./ui/App";
import { FeedbackProvider } from "./ui/Feedback";
import "./styles.css";

try {
  const saved = localStorage.getItem("irden.weather-editor.theme");
  if (saved === "dark" || saved === "light") {
    document.documentElement.setAttribute("data-theme", saved);
  }
} catch {
  // Приватный режим — остаёмся на системной теме.
}

const container = document.getElementById("root");
if (!container) throw new Error("Не найден #root");

createRoot(container).render(
  <StrictMode>
    <FeedbackProvider>
      <App />
    </FeedbackProvider>
  </StrictMode>,
);

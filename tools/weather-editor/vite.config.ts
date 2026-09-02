import {defineConfig} from "vite";
import react from "@vitejs/plugin-react";
import {viteSingleFile} from "vite-plugin-singlefile";

// Собирается в один самодостаточный HTML: скрипты и стили инлайнятся,
// поэтому результат по-прежнему открывается двойным кликом из файловой системы.
// scripts/emit.mjs кладёт его в tools/weather-editor.html.
export default defineConfig({
    base: "./",
    plugins: [react(), viteSingleFile()],
    build: {
        target: "es2022",
        cssCodeSplit: false,
        assetsInlineLimit: 100_000_000,
        chunkSizeWarningLimit: 4096,
        reportCompressedSize: false,
    },
});

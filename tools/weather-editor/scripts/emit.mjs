// Кладёт собранный self-contained HTML туда, где редактор жил раньше,
// чтобы старые ссылки и закладки на tools/weather-editor.html не сломались.
import {copyFileSync, existsSync, statSync} from "node:fs";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const built = resolve(here, "..", "dist", "index.html");
const target = resolve(here, "..", "..", "weather-editor.html");

if (!existsSync(built)) {
    console.error(`[emit] нет собранного файла: ${built}`);
    process.exit(1);
}

copyFileSync(built, target);
const kb = (statSync(target).size / 1024).toFixed(0);
console.log(`[emit] tools/weather-editor.html обновлён (${kb} КБ)`);

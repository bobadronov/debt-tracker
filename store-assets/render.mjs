import { chromium } from "playwright";
import path from "node:path";
import { fileURLToPath } from "node:url";

const dir = path.dirname(fileURLToPath(import.meta.url));
const browser = await chromium.launch();
const page = await browser.newPage({
  viewport: { width: 1024, height: 500 },
  deviceScaleFactor: 1,
});
await page.goto("file://" + path.join(dir, "feature-graphic.html"));
await page.screenshot({ path: path.join(dir, "feature-graphic.png") });
await browser.close();
console.log("done");

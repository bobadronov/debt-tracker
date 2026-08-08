import { chromium } from "playwright";
import path from "node:path";
import fs from "node:fs";
import { fileURLToPath } from "node:url";

const dir = path.dirname(fileURLToPath(import.meta.url));

const sizes = [
  { tag: "phone", dir: "phone", width: 1080, height: 1920, mode: "phone" },
  { tag: "7in-tablet", dir: "tablet-7in", width: 1440, height: 2560, mode: "tablet" },
  { tag: "10in-tablet", dir: "tablet-10in", width: 2160, height: 3840, mode: "tablet" },
];

const screens = [
  "home-debtors",
  "home-creditors",
  "stats",
  "debtor-detail",
  "settings",
];

const browser = await chromium.launch();
const page = await browser.newPage();
await page.goto("file://" + path.join(dir, "screens.html"));

for (const size of sizes) {
  const outDir = path.join(dir, size.dir);
  fs.mkdirSync(outDir, { recursive: true });
  await page.setViewportSize({ width: size.width, height: size.height });
  await page.evaluate((mode) => setMode(mode), size.mode);
  for (const screen of screens) {
    await page.evaluate((name) => showScreen(name), screen);
    await page.screenshot({ path: path.join(outDir, `${screen}.png`) });
    console.log(`${size.tag}/${screen}.png`);
  }
}

await browser.close();

// Guest smoke test: lang, adoption list, adopt button, map tiles, home CTAs.
// Run: browser_run_code_unsafe { filename: "scripts/ux-audit/smoke-guest.js" } (needs the dev server on :3100)
async (page) => {
  const BASE = "http://localhost:3100";
  const out = {};
  const errs = [];
  page.on("console", (m) => { if (m.type() === "error") errs.push(m.text().slice(0, 160)); });
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.evaluate(() => localStorage.clear()).catch(() => {});

  await page.goto(BASE + "/", { timeout: 120000 });
  await page.waitForTimeout(6000);
  out.lang = await page.evaluate(() => document.documentElement.lang);
  out.heroCtas = await page.locator("section").first().locator("a").allInnerTexts();

  await page.goto(BASE + "/nhan-nuoi");
  await page.waitForTimeout(8000);
  out.available = await page.locator("text=bé đang sẵn sàng").first().innerText().catch(() => "n/a");
  out.rawEnums = await page.evaluate(() =>
    (document.body.innerText.match(/\b(AVAILABLE|UNDER_TREATMENT|HEALTHY|RECOVERING|SPECIAL_NEEDS|DOG|CAT|months?)\b/g) || []).length);

  await page.locator("article").first().click();
  await page.waitForURL(/nhan-nuoi\/.+/);
  await page.waitForTimeout(6000);
  const btn = page.getByRole("button", { name: /Nhận nuôi ngay|Hiện không thể nhận nuôi/ });
  out.adoptButton = { text: await btn.innerText(), disabled: await btn.isDisabled() };

  await page.goto(BASE + "/ban-do");
  await page.waitForTimeout(8000);
  out.mapTiles = await page.evaluate(() =>
    [...new Set([...document.querySelectorAll(".leaflet-tile")].map((t) => new URL(t.src).hostname))]);
  out.consoleErrors = errs.slice(0, 5);
  return out;
}

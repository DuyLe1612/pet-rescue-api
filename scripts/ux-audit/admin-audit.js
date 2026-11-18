async (page) => {
  const BASE = "http://localhost:3100";
  // Screenshots go to .ux-tmp/admin/ (git-ignored) relative to where Playwright MCP runs.
  const DIR = globalThis.__DIR || ".ux-tmp/admin/";
  const ACCOUNT = globalThis.__ACCOUNT || "demo.admin@petrescue.demo";
  const PAGES = globalThis.__PAGES || ["/dashboard", "/quan-ly-thu-cung", "/quan-ly-trung-tam", "/quan-ly-nguoi-dung",
    "/quan-ly-bai-dang", "/quan-ly-bai-viet", "/quan-ly-cuu-tro", "/quan-ly-nhan-nuoi", "/quan-ly-banner",
    "/organization/members", "/trang-ca-nhan"];
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto(BASE + "/dang-nhap", { timeout: 120000 });
  await page.evaluate(() => localStorage.clear());
  await page.reload();
  await page.getByLabel("Email hoặc username").fill(ACCOUNT);
  await page.locator("input[type=password]").fill("Demo@12345");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await page.waitForURL((u) => !u.pathname.includes("dang-nhap"), { timeout: 60000 });
  const out = {};
  for (const path of PAGES) {
    await page.goto(BASE + path);
    await page.waitForTimeout(7000);
    const name = path.replace(/\W+/g, "_").replace(/^_/, "");
    await page.screenshot({ path: DIR + name + ".jpeg", type: "jpeg", quality: 60, fullPage: true });
    out[path] = await page.evaluate(() => {
      const main = document.querySelector("main") || document.body;
      const text = main.innerText;
      const uuids = [...new Set(text.match(/\b[0-9a-f]{8}(-[0-9a-f]{4}){0,3}(-[0-9a-f]{4,12})?\b|\b[0-9a-f]{8}\.\.\./g) || [])];
      const words = text.match(/\b[A-Z][A-Z_]{2,}\b/g) || [];
      const ignore = new Set(["OK", "ID", "URL", "SOS", "PDF", "VNĐ", "TNV", "AI"]);
      const enums = [...new Set(words.filter((w) => !ignore.has(w)))];
      const h1 = [...main.querySelectorAll("h1,h2")].slice(0, 3).map((h) => `${h.tagName}:${getComputedStyle(h).fontSize}:${h.innerText.trim().slice(0, 40)}`);
      const buttons = [...main.querySelectorAll("button,a[role=button],a[data-slot=button]")]
        .filter((b) => b.offsetParent && b.innerText.trim())
        .slice(0, 14)
        .map((b) => {
          const r = b.getBoundingClientRect();
          const s = getComputedStyle(b);
          return `${b.innerText.trim().slice(0, 22)}|${s.backgroundColor}|${Math.round(r.x)},${Math.round(r.y)}`;
        });
      const crumb = document.querySelector("header nav")?.innerText?.replace(/\n/g, " / ");
      return { crumb, h1, uuids: uuids.slice(0, 6), uuidCount: uuids.length, enums: enums.slice(0, 15), buttons, empty: /Chưa có|Không có|không có dữ liệu/i.test(text) };
    });
  }
  return out;
}

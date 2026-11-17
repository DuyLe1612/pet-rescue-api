// Owner login: empty-field messages, redirect to /dashboard, breadcrumb, shortcuts.
// For another role, copy this file and change EMAIL (user/admin in _lib.js).
async (page) => {
  const BASE = "http://localhost:3100";
  const EMAIL = "demo.owner1@petrescue.demo";
  const PASSWORD = "Demo@12345";
  await page.goto(BASE + "/dang-nhap");
  await page.evaluate(() => localStorage.clear());
  await page.reload();
  await page.waitForTimeout(2000);
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await page.waitForTimeout(800);
  const emptyErrs = (await page.locator("[data-slot=field-error], [role=alert]").allInnerTexts()).filter(Boolean);
  await page.getByLabel("Email hoặc username").fill(EMAIL);
  await page.locator("input[type=password]").fill(PASSWORD);
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await page.waitForURL((u) => !u.pathname.includes("dang-nhap"), { timeout: 40000 });
  await page.waitForTimeout(6000);
  return {
    emptyErrs,
    url: page.url(),
    breadcrumb: await page.locator("header").first().innerText(),
    main: (await page.locator("main").innerText()).slice(0, 300),
  };
}

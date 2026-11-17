// WCAG 1.4.3 contrast check on the current page (handles lab()/oklch() via canvas).
// Navigate first, then run this. Returns failing colour pairs grouped, with one example each.
async (page) => {
  return await page.evaluate(() => {
    const cv = document.createElement("canvas"); cv.width = cv.height = 1;
    const cx = cv.getContext("2d", { willReadFrequently: true });
    const rgb = (c, bg) => { cx.fillStyle = bg || "#fff"; cx.fillRect(0, 0, 1, 1); cx.fillStyle = c; cx.fillRect(0, 0, 1, 1); const d = cx.getImageData(0, 0, 1, 1).data; return [d[0], d[1], d[2]]; };
    const lum = (a) => { const f = (v) => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }; return 0.2126 * f(a[0]) + 0.7152 * f(a[1]) + 0.0722 * f(a[2]); };
    const bgOf = (el) => { let e = el; while (e) { const s = getComputedStyle(e); if (s.backgroundImage !== "none") return null; cx.clearRect(0, 0, 1, 1); cx.fillStyle = s.backgroundColor; cx.fillRect(0, 0, 1, 1); if (cx.getImageData(0, 0, 1, 1).data[3] > 230) return s.backgroundColor; e = e.parentElement; } return "#fff"; };
    const out = {};
    for (const el of document.querySelectorAll("body *")) {
      if (![...el.childNodes].some((n) => n.nodeType === 3 && n.textContent.trim())) continue;
      const r = el.getBoundingClientRect(); if (!r.width || !r.height) continue;
      const s = getComputedStyle(el); const b = bgOf(el); if (!b) continue;
      const B = rgb(b), F = rgb(s.color, b);
      const ratio = (Math.max(lum(F), lum(B)) + 0.05) / (Math.min(lum(F), lum(B)) + 0.05);
      const fs = parseFloat(s.fontSize);
      const large = fs >= 24 || (fs >= 18.66 && +s.fontWeight >= 700);
      if (ratio < (large ? 3 : 4.5)) {
        const k = `${F} on ${B}`;
        out[k] = out[k] || { ratio: +ratio.toFixed(2), fs, ex: el.innerText.trim().slice(0, 35), n: 0 };
        out[k].n++;
      }
    }
    return { url: location.pathname, lang: document.documentElement.lang, fails: out };
  });
}

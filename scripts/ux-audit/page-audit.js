async (page) => {
  return await page.evaluate(() => {
  const parse = (c) => { const m = c.match(/rgba?\(([^)]+)\)/); if (!m) return null; const p = m[1].split(/[ ,/]+/).filter(Boolean).map(Number); return { r: p[0], g: p[1], b: p[2], a: p.length > 3 ? p[3] : 1 }; };
  const lum = ({ r, g, b }) => { const f = (v) => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }; return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b); };
  const bgOf = (el) => { let e = el; while (e) { const s = getComputedStyle(e); if (s.backgroundImage && s.backgroundImage !== 'none') return null; const c = parse(s.backgroundColor); if (c && c.a > 0.9) return c; e = e.parentElement; } return { r: 255, g: 255, b: 255, a: 1 }; };
  const low = []; const fonts = {}; const sizes = {}; const colors = {};
  const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
  const seen = new Set();
  while (walker.nextNode()) {
    const t = walker.currentNode; const el = t.parentElement; const txt = t.textContent.trim();
    if (!txt || seen.has(el)) continue; seen.add(el);
    const r = el.getBoundingClientRect(); if (!r.width || !r.height) continue;
    const s = getComputedStyle(el); if (s.visibility === 'hidden' || +s.opacity === 0) continue;
    const fam = s.fontFamily.split(',')[0].replace(/"/g, ''); fonts[fam] = (fonts[fam] || 0) + 1;
    const fs = parseFloat(s.fontSize); sizes[fs] = (sizes[fs] || 0) + 1;
    colors[s.color] = (colors[s.color] || 0) + 1;
    const fg = parse(s.color); const bg = bgOf(el); if (!fg || !bg) continue;
    const L1 = lum(fg), L2 = lum(bg); const ratio = (Math.max(L1, L2) + 0.05) / (Math.min(L1, L2) + 0.05);
    const large = fs >= 24 || (fs >= 18.66 && +s.fontWeight >= 700);
    const need = large ? 3 : 4.5;
    if (ratio < need) low.push({ t: txt.slice(0, 40), ratio: +ratio.toFixed(2), fs, fg: s.color, bg: `rgb(${bg.r},${bg.g},${bg.b})` });
  }
  const small = [...document.querySelectorAll('a,button,[role=button],input,select,[role=checkbox],[role=combobox]')]
    .map(e => ({ e, r: e.getBoundingClientRect() }))
    .filter(({ r }) => r.width > 0 && r.height > 0 && (r.width < 24 || r.height < 24))
    .map(({ e, r }) => ({ tag: e.tagName, label: (e.innerText || e.getAttribute('aria-label') || e.title || e.name || '').trim().slice(0, 30), w: Math.round(r.width), h: Math.round(r.height) }));
  const unlabeled = [...document.querySelectorAll('button,a')].filter(e => e.getBoundingClientRect().width && !(e.innerText.trim() || e.getAttribute('aria-label') || e.title || e.querySelector('img[alt]:not([alt=""])'))).map(e => e.outerHTML.slice(0, 90));
  const imgsNoAlt = [...document.images].filter(i => !i.hasAttribute('alt')).length;
  const headings = [...document.querySelectorAll('h1,h2,h3,h4')].map(h => h.tagName + ':' + h.innerText.trim().slice(0, 40));
  const inputsNoLabel = [...document.querySelectorAll('input:not([type=hidden]),textarea,select')].filter(i => !(i.labels && i.labels.length) && !i.getAttribute('aria-label') && !i.getAttribute('aria-labelledby')).map(i => (i.name || i.placeholder || i.type));
  const top = (o, n = 8) => Object.entries(o).sort((a, b) => b[1] - a[1]).slice(0, n);
  return { url: location.pathname, lang: document.documentElement.lang, lowContrast: low.slice(0, 20), lowCount: low.length, smallTargets: small.slice(0, 15), smallCount: small.length, unlabeled: unlabeled.slice(0, 8), imgsNoAlt, headings: headings.slice(0, 20), inputsNoLabel, fonts: top(fonts), fontSizes: top(sizes, 12), textColors: top(colors, 8), hScroll: document.documentElement.scrollWidth > innerWidth };
  });
}

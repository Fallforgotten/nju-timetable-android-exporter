globalThis.cengkeStartMobilePagination = function () {
  window.__cengkeAllResult = ''; window.__cengkeAllProgress = ''; window.__cengkeAllStop = false;
  const source = (location.pathname || '').includes('/kcbcx/') ? 'schoolwide' : 'personal';
  const findPager = () => {
    const pattern = /(\d+)[-–—](\d+)总记录数[:：]?(\d+)/;
    const candidates = [...document.querySelectorAll('div, nav, section, td')].filter(el => {
      if (!el.getClientRects().length) return false;
      const text = el.textContent.replace(/\s/g, '');
      return text.length < 1500 && pattern.test(text) && /总页数/.test(text) && /跳转/.test(text);
    });
    const roots = candidates.filter(el => !candidates.some(other => other !== el && el.contains(other)));
    const scored = roots.map(root => {
      const controls = [...root.querySelectorAll('button, [role="button"], input[type="button"], a')].filter(el => el.getClientRects().length);
      const rect = root.getBoundingClientRect();
      return { root, controls, bottom: rect.bottom };
    }).filter(item => item.controls.length >= 4).sort((a, b) => b.bottom - a.bottom);
    if (!scored.length) throw new Error(`分页栏识别结果为 ${roots.length} 个；无法确认分页栏。`);
    if (scored.length > 1 && Math.abs(scored[0].bottom - scored[1].bottom) < 2) throw new Error('存在多个位置相同的分页栏，无法确认分页栏。');
    return scored[0];
  };
  const read = () => { const item = findPager(), text = item.root.textContent.replace(/\s/g, ''), match = text.match(/(\d+)[-–—](\d+)总记录数[:：]?(\d+)/); if (!match) throw new Error('无法识别分页范围和总记录数。'); const data = globalThis.cengkeNjuExtract(document, { source }); if (data.error) throw new Error(data.error); return { start: Number(match[1]), end: Number(match[2]), total: Number(match[3]), data }; };
  const next = () => { const item = findPager(), root = item.root, icons = [...root.querySelectorAll('.jqx-icon-arrow-right, [class*="jqx-icon-arrow-right-"]')]; const labelled = [...root.querySelectorAll('button, a, [role="button"], [title], [aria-label]')].filter(el => /^(下一页|Next(?: page)?)$/i.test(el.getAttribute('aria-label') || el.getAttribute('title') || el.textContent.trim())); const iconButtons = icons.map(icon => icon.closest('button, [role="button"]') || icon.parentElement).filter(el => el); let buttons = [...new Set([...labelled, ...iconButtons])].filter(el => el.getClientRects().length); if (buttons.length !== 1 && item.controls.length >= 4) buttons = [item.controls[2]]; if (buttons.length !== 1 || buttons[0].disabled || buttons[0].getAttribute('aria-disabled') === 'true' || /jqx-fill-state-disabled/.test(buttons[0].className)) throw new Error('无法确认可用的下一页按钮，已停止'); buttons[0].click(); };
  globalThis.cengkeCollectPages({ read, next, sleep: ms => new Promise(resolve => setTimeout(resolve, ms)), stopped: () => window.__cengkeAllStop === true, progress: text => { window.__cengkeAllProgress = text; } }).then(result => { window.__cengkeAllResult = JSON.stringify(result); }).catch(error => { window.__cengkeAllResult = JSON.stringify({ complete: false, rows: [], reason: error.message }); });
  return JSON.stringify({ started: true });
};

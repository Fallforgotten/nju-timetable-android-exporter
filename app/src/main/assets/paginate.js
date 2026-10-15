globalThis.cengkeCollectPages = async function ({ read, next, sleep, stopped, progress }) {
  const rows = new Map(), seen = new Set();
  let first, pages = 0, expected = 1;
  const result = (complete, reason) => ({ format: 'cengke-nju-visible-v1', source: first?.data.source || 'schoolwide', term: first?.data.term || '', scope: complete ? 'all-filtered-pages' : 'partial-pages', complete, reason, pages, expectedRows: first?.total, rows: [...rows.values()] });
  try {
    let current = await read(); first = current;
    if (current.start !== 1) throw new Error('请先在学校页面回到第一页，再开始全部导出。');
    while (true) {
      if (stopped()) throw new Error('用户已停止');
      if (current.start !== expected || current.total !== first.total || current.data.term !== first.data.term) throw new Error('页码、总条数或学期变化，已停止；请保持筛选条件不变。');
      if (!Number.isInteger(current.total) || current.total < 1 || current.total > 20000 || current.end < current.start || current.end > current.total) throw new Error('分页数量无效或超过 20000 条，请缩小查询范围。');
      const fingerprint = JSON.stringify(current.data.rows);
      if (seen.has(fingerprint)) throw new Error('翻页后仍是重复数据，未将其当作新页。');
      if (current.data.rows.length !== current.end - current.start + 1) throw new Error('当前页读取条数与分页条数不一致，无法确认完整性。');
      seen.add(fingerprint); for (const row of current.data.rows) rows.set(JSON.stringify(row), row); pages++;
      progress(`已读取 ${pages} 页，${current.end}/${current.total} 条，去重后 ${rows.size} 个教学班`);
      if (current.end === current.total) { if (rows.size !== current.total) throw new Error('跨页存在重复教学班，总条数不一致，请核对部分结果。'); return result(true, '已核对分页连续性和总条数'); }
      if (pages >= 2000) throw new Error('达到页数上限，请缩小范围');
      expected = current.end + 1; await sleep(1200); if (stopped()) throw new Error('用户已停止'); await next();
      let stable = '', candidate, ready = false;
      for (let attempt = 0; attempt < 30; attempt++) { if (stopped()) throw new Error('用户已停止'); await sleep(500); try { candidate = await read(); } catch { stable = ''; continue; } const signature = JSON.stringify(candidate); if (candidate.start === expected && JSON.stringify(candidate.data.rows) !== fingerprint && signature === stable) { ready = true; break; } stable = signature; }
      if (!ready) throw new Error('下一页在 15 秒内未稳定加载，已停止，避免漏页'); current = candidate;
    }
  } catch (error) { return result(false, error.message); }
};

globalThis.cengkeFindPager = function (doc) {
  const pattern = /(\d+)[-–—](\d+)总记录数[:：]?(\d+)/;
  const candidates = [...doc.querySelectorAll('div, nav, section, td')].filter(el => { if (!el.getClientRects().length) return false; const text = el.textContent.replace(/\s/g, ''); return text.length < 1500 && pattern.test(text) && /总页数/.test(text) && /跳转/.test(text); });
  const roots = candidates.filter(el => !candidates.some(other => other !== el && el.contains(other)));
  if (roots.length !== 1) throw new Error(`分页栏识别结果为 ${roots.length} 个；无法确认分页栏。`); return roots[0];
};

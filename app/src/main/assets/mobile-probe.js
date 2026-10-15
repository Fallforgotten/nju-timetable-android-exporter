(function () {
  const path = location.pathname || '';
  const source = path.includes('/kcbcx/') ? 'schoolwide' : 'personal';
  const result = globalThis.cengkeNjuExtract(document, { source });
  return JSON.stringify({
    ok: !result.error,
    count: Array.isArray(result.rows) ? result.rows.length : 0,
    error: result.error || '',
    diagnostics: result.diagnostics || null,
    data: result.error ? null : result
  });
})()

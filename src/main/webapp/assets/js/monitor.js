// Live dashboard: reads /metrics (same site, owner only) every 3 seconds and fills the cards.
(function () {
  function $(id) { return document.getElementById(id); }
  function text(id, v) { $(id).textContent = v; }
  function bar(id, pct) {
    var el = $(id);
    pct = Math.max(0, Math.min(100, pct));
    el.style.width = pct + '%';
    el.className = 'progress-bar ' + (pct >= 85 ? 'bg-danger' : pct >= 65 ? 'bg-warning' : 'bg-success');
  }
  function uptime(s) {
    var d = Math.floor(s / 86400), h = Math.floor(s % 86400 / 3600), m = Math.floor(s % 3600 / 60);
    return (d ? d + 'd ' : '') + (h ? h + 'h ' : '') + m + 'm ' + (s % 60) + 's';
  }
  function show(m) {
    var t = m.traffic, mem = m.memory, cpu = m.cpu, db = m.dbPool, th = m.threads, gc = m.gc;
    text('up', 'UP'); text('uptime', 'Running ' + uptime(m.uptimeSeconds));
    text('rpm', t.requestsLastMinute); text('total', t.requestsTotal + ' since start');
    text('avg', t.avgMs + ' ms'); text('p95', 'p95 about ' + t.p95MsApprox + ' ms, slow (>1s): ' + t.slowOver1s);
    text('err5', t.errors5xx + ' server errors'); text('err4', t.errors4xx + ' client errors (404, bad input)');
    $('err5').className = 'h4 mb-0 ' + (t.errors5xx > 0 ? 'text-danger' : 'text-success');

    var heapPct = mem.heapUsedMb * 100 / mem.heapMaxMb;
    text('heapText', mem.heapUsedMb + ' / ' + mem.heapMaxMb + ' MB'); bar('heapBar', heapPct);
    text('nonheap', 'Non-heap: ' + mem.nonHeapUsedMb + ' MB');
    text('gc', 'Garbage collections: ' + gc.collections + ' (total pause ' + gc.totalPauseMs + ' ms)');

    var cpuPct = Math.max(cpu.processCpuPercent, 0);
    text('cpuText', 'App ' + cpu.processCpuPercent + '%, whole machine ' + cpu.systemCpuPercent + '%'); bar('cpuBar', cpuPct);
    text('cores', cpu.cores + ' cores, load average ' + cpu.loadAverage.toFixed(2));
    text('threads', 'Threads: ' + th.live + ' live, peak ' + th.peak);

    text('dbText', db.active + ' busy, ' + db.idle + ' idle, ' + db.total + ' open / max ' + db.max);
    bar('dbBar', db.active * 100 / db.max);
    text('dbWait', 'Requests waiting for a connection: ' + db.waitingThreads + (db.waitingThreads > 0 ? ' (pool may be too small)' : ' (good)'));
    text('status', 'Updated ' + new Date().toLocaleTimeString());
  }
  function tick() {
    fetch('/metrics', { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
      .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); })
      .then(show)
      .catch(function () { text('status', 'Could not load metrics'); text('up', 'DOWN?'); });
  }
  tick();
  setInterval(tick, 3000);
})();

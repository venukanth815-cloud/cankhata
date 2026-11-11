<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Server monitor - CanKhata</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<%@ include file="_nav.jspf" %>
<main class="container pb-4">
  <div class="d-flex justify-content-between align-items-center mb-3">
    <h1 class="h4 mb-0">Server monitor</h1>
    <span class="small text-muted" id="status">Loading...</span>
  </div>

  <div class="row g-3">
    <div class="col-6 col-lg-3"><div class="card p-3"><div class="text-muted small">Status</div><div class="h4 mb-0" id="up">-</div><div class="small text-muted" id="uptime">-</div></div></div>
    <div class="col-6 col-lg-3"><div class="card p-3"><div class="text-muted small">Requests (last minute)</div><div class="h4 mb-0" id="rpm">-</div><div class="small text-muted" id="total">-</div></div></div>
    <div class="col-6 col-lg-3"><div class="card p-3"><div class="text-muted small">Average response</div><div class="h4 mb-0" id="avg">-</div><div class="small text-muted" id="p95">-</div></div></div>
    <div class="col-6 col-lg-3"><div class="card p-3"><div class="text-muted small">Errors</div><div class="h4 mb-0" id="err5">-</div><div class="small text-muted" id="err4">-</div></div></div>

    <div class="col-12 col-md-6">
      <div class="card p-3">
        <div class="d-flex justify-content-between"><span class="fw-semibold">Memory (heap)</span><span class="small text-muted" id="heapText">-</span></div>
        <div class="progress mt-2"><div class="progress-bar" id="heapBar" style="width:0%"></div></div>
        <div class="small text-muted mt-2" id="nonheap">-</div>
        <div class="small text-muted" id="gc">-</div>
      </div>
    </div>
    <div class="col-12 col-md-6">
      <div class="card p-3">
        <div class="d-flex justify-content-between"><span class="fw-semibold">CPU</span><span class="small text-muted" id="cpuText">-</span></div>
        <div class="progress mt-2"><div class="progress-bar" id="cpuBar" style="width:0%"></div></div>
        <div class="small text-muted mt-2" id="cores">-</div>
        <div class="small text-muted" id="threads">-</div>
      </div>
    </div>
    <div class="col-12">
      <div class="card p-3">
        <div class="d-flex justify-content-between"><span class="fw-semibold">Database connections</span><span class="small text-muted" id="dbText">-</span></div>
        <div class="progress mt-2"><div class="progress-bar" id="dbBar" style="width:0%"></div></div>
        <div class="small text-muted mt-2" id="dbWait">-</div>
      </div>
    </div>
  </div>
  <p class="small text-muted mt-3 mb-0">Refreshes every 3 seconds. Raw JSON for tools: <a href="${ctx}/metrics" target="_blank" rel="noopener">/metrics</a> &middot; Health: <a href="${ctx}/health" target="_blank" rel="noopener">/health</a></p>
</main>
<script src="${ctx}/assets/js/monitor.js" defer></script>
</body>
</html>

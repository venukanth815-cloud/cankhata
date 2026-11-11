<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Daily sheet - CanKhata</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<%@ include file="_nav.jspf" %>
<main class="container pb-4">
  <c:if test="${saved}"><div class="alert alert-success py-2">Saved.</div></c:if>
  <c:if test="${not empty formError}"><div class="alert alert-danger py-2"><c:out value="${formError}"/></div></c:if>
  <h1 class="h4 mb-3">Daily sheet</h1>
  <form method="get" action="${ctx}/owner/sheet" class="d-flex gap-2 mb-3">
    <input class="form-control" type="date" name="date" value="${day}" required aria-label="Date">
    <button class="btn btn-ck" type="submit">Open</button>
  </form>

  <c:if test="${empty sheet or open}">
    <form method="post" action="${ctx}/owner/sheet" class="mb-3">
      <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="date" value="${day}"><input type="hidden" name="action" value="generate">
      <button class="btn btn-outline-secondary" type="submit">${empty sheet ? 'Generate sheet from standing orders' : 'Add newly due deliveries'}</button>
    </form>
  </c:if>

  <c:if test="${empty sheet}"><p class="text-muted">No sheet for ${day} yet.</p></c:if>
  <c:if test="${not empty sheet}">
    <div class="mb-2">Status: <span class="badge ${open ? 'bg-success' : 'bg-secondary'}">${sheet.status}</span></div>

    <div class="card p-3 mb-3">
      <c:forEach var="r" items="${rows}">
        <div class="d-flex justify-content-between border-bottom py-2">
          <div>
            <div class="fw-semibold"><c:out value="${r.customer_name}"/> <span class="text-muted small">&middot; <c:out value="${r.can_name}"/></span></div>
            <div class="small text-muted"><c:out value="${r.address}"/> &middot; <c:out value="${r.boy_name}"/></div>
          </div>
          <div class="text-end small">
            <span class="badge ${r.status == 'DELIVERED' ? 'bg-success' : (r.status == 'MISSED' ? 'bg-danger' : 'bg-warning text-dark')}">${r.status}</span>
            <div>${r.delivered_qty}/${r.planned_qty} &middot; empties ${r.empties_collected}<c:if test="${r.cash_collected_paise > 0}"> &middot; Rs ${r.cashText}</c:if></div>
          </div>
        </div>
      </c:forEach>
      <c:if test="${empty rows}"><p class="text-muted mb-0">No deliveries on this sheet.</p></c:if>
    </div>

    <div class="card p-3 mb-3">
      <h2 class="h6">Van check</h2>
      <c:forEach var="v" items="${vans}">
        <form method="post" action="${ctx}/owner/sheet" class="row g-2 align-items-end border-bottom pb-2 mb-2">
          <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="date" value="${day}"><input type="hidden" name="action" value="van">
          <input type="hidden" name="sheetId" value="${sheet.id}"><input type="hidden" name="boyId" value="${v.boy_id}">
          <div class="col-12 fw-semibold"><c:out value="${v.boy_name}"/> <span class="small text-muted">delivered ${v.delivered}, empties collected ${v.empties_collected}</span></div>
          <div class="col-4"><label class="form-label small mb-0">Full out</label><input class="form-control" type="number" min="0" name="fullOut" value="${v.full_out}"></div>
          <div class="col-4"><label class="form-label small mb-0">Full back</label><input class="form-control" type="number" min="0" name="fullBack" value="${v.full_back}"></div>
          <div class="col-4"><label class="form-label small mb-0">Empties in</label><input class="form-control" type="number" min="0" name="emptiesIn" value="${v.empties_in}"></div>
          <c:if test="${open}"><div class="col-12"><button class="btn btn-sm btn-outline-secondary" type="submit">Save</button></div></c:if>
          <c:if test="${v.fullMissing != 0 or v.emptiesMissing != 0}">
            <div class="col-12"><div class="alert alert-warning py-1 mb-0 small">
              <c:if test="${v.fullMissing != 0}">Full cans do not add up: ${v.fullMissing} unaccounted. </c:if>
              <c:if test="${v.emptiesMissing != 0}">Empties differ by ${v.emptiesMissing}.</c:if></div></div>
          </c:if>
        </form>
      </c:forEach>
    </div>

    <c:if test="${open}">
      <form method="post" action="${ctx}/owner/sheet">
        <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="date" value="${day}"><input type="hidden" name="action" value="close">
        <input type="hidden" name="sheetId" value="${sheet.id}">
        <button class="btn btn-ck" type="submit">Close this day</button>
        <span class="small text-muted ms-2">After closing, nothing on this day can be changed.</span>
      </form>
    </c:if>
  </c:if>
</main>
</body>
</html>

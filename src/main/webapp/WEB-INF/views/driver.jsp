<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Today's deliveries - CanKhata</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<%@ include file="_nav.jspf" %>
<main class="container pb-4">
  <c:if test="${saved}"><div class="alert alert-success py-2">Saved.</div></c:if>
  <c:if test="${not empty formError}"><div class="alert alert-danger py-2"><c:out value="${formError}"/></div></c:if>
  <h1 class="h4 mb-3">Today's deliveries</h1>
  <c:forEach var="r" items="${rows}">
    <div class="card p-3 mb-3">
      <div class="d-flex justify-content-between">
        <div class="fw-semibold"><c:out value="${r.customer_name}"/></div>
        <span class="badge ${r.status == 'DELIVERED' ? 'bg-success' : (r.status == 'MISSED' ? 'bg-danger' : 'bg-warning text-dark')}">${r.status}</span>
      </div>
      <div class="small"><c:out value="${r.address}"/><c:if test="${not empty r.landmark}"> (<c:out value="${r.landmark}"/>)</c:if></div>
      <div class="small mb-2"><a href="tel:${r.phone}">${r.phone}</a> &middot; <c:out value="${r.can_name}"/> x ${r.planned_qty} &middot; has ${r.held} can(s)
        <c:if test="${r.dueNow}"> &middot; <span class="text-danger">owes Rs ${r.dueText}</span></c:if></div>
      <c:if test="${r.status == 'PLANNED'}">
        <form method="post" action="${ctx}/driver/today" class="row g-2">
          <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="id" value="${r.id}">
          <div class="col-4"><label class="form-label small mb-0">Delivered</label><input class="form-control" type="number" min="0" max="100" name="delivered" value="${r.planned_qty}" required></div>
          <div class="col-4"><label class="form-label small mb-0">Empties</label><input class="form-control" type="number" min="0" max="100" name="empties" value="0"></div>
          <div class="col-4"><label class="form-label small mb-0">Cash (Rs)</label><input class="form-control" name="cash" inputmode="decimal" placeholder="0"></div>
          <div class="col-12"><input class="form-control" name="note" maxlength="255" placeholder="Note (optional)"></div>
          <div class="col-8"><button class="btn btn-ck w-100" type="submit">Delivered</button></div>
        </form>
        <form method="post" action="${ctx}/driver/today" class="d-flex gap-2 mt-2">
          <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="id" value="${r.id}"><input type="hidden" name="action" value="missed">
          <input class="form-control form-control-sm" name="reason" maxlength="255" placeholder="Why missed? (door locked, away...)" required>
          <button class="btn btn-sm btn-outline-danger text-nowrap" type="submit">Missed</button>
        </form>
      </c:if>
      <c:if test="${r.status == 'DELIVERED'}"><div class="small text-muted">Delivered ${r.delivered_qty}, empties back ${r.empties_collected}.</div></c:if>
    </div>
  </c:forEach>
  <c:if test="${empty rows}"><p class="text-muted">No deliveries for today. Ask the owner to generate today's sheet.</p></c:if>
</main>
</body>
</html>

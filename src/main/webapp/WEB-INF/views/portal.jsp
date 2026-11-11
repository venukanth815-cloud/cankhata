<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>My account - CanKhata</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<%@ include file="_nav.jspf" %>
<main class="container pb-4">
  <c:if test="${saved}"><div class="alert alert-success py-2">Saved.</div></c:if>
  <c:if test="${not empty formError}"><div class="alert alert-danger py-2"><c:out value="${formError}"/></div></c:if>
  <h1 class="h4 mb-3">Hello, <c:out value="${c.name}"/></h1>
  <c:forEach var="m" items="${messages}"><div class="alert alert-info py-2"><c:out value="${m.message}"/></div></c:forEach>

  <div class="row g-3 mb-3">
    <div class="col-6"><div class="card p-3"><div class="small text-muted">You owe</div><div class="h4 mb-0 ${c.balancePaise > 0 ? 'text-danger' : ''}">Rs ${c.balanceText}</div></div></div>
    <div class="col-6"><div class="card p-3"><div class="small text-muted">Deposit with us</div><div class="h4 mb-0">Rs ${c.depositText}</div></div></div>
    <c:forEach var="k" items="${cans}"><div class="col-6"><div class="card p-3"><div class="small text-muted">Cans you hold: <c:out value="${k.name}"/></div><div class="h4 mb-0">${k.held}</div></div></div></c:forEach>
  </div>

  <div class="card p-3 mb-3">
    <h2 class="h6">My deliveries</h2>
    <c:forEach var="s" items="${subs}">
      <c:if test="${s.active}">
        <div class="border-bottom pb-2 mb-2">
          <div class="fw-semibold">${s.qty} x <c:out value="${s.can_name}"/> &middot; ${s.frequency}</div>
          <c:forEach var="k" items="${s.skips}">
            <form method="post" action="${ctx}/customer/home" class="d-inline">
              <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="action" value="unskip"><input type="hidden" name="skipId" value="${k.id}">
              <span class="skip-chip">Skipping ${k.skip_date} <button class="btn btn-link btn-sm p-0 align-baseline" type="submit">Undo</button></span>
            </form>
          </c:forEach>
          <form method="post" action="${ctx}/customer/home" class="d-flex gap-2 mt-2">
            <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="subId" value="${s.id}">
            <input class="form-control form-control-sm" type="date" name="date" min="${tomorrow}" required aria-label="Skip date">
            <button class="btn btn-sm btn-outline-secondary text-nowrap" type="submit">Skip this day</button>
          </form>
        </div>
      </c:if>
    </c:forEach>
    <c:if test="${empty subs}"><p class="text-muted mb-0 small">No standing order. Please contact us.</p></c:if>
  </div>

  <div class="card p-3 mb-3">
    <h2 class="h6">Recent deliveries</h2>
    <c:forEach var="d" items="${deliveries}">
      <div class="small border-bottom py-1">${d.sheet_date} &middot; <c:out value="${d.can_name}"/> &middot; ${d.status}
        <c:if test="${d.status == 'DELIVERED'}">&middot; ${d.delivered_qty} delivered, ${d.empties_collected} empties taken</c:if></div>
    </c:forEach>
    <c:if test="${empty deliveries}"><p class="text-muted mb-0 small">Nothing yet.</p></c:if>
  </div>

  <div class="card p-3">
    <h2 class="h6">Bills and payments</h2>
    <c:forEach var="i" items="${invoices}"><div class="small border-bottom py-1">${i.invoice_month} &middot; Rs ${i.totalText} &middot; ${i.status}</div></c:forEach>
    <c:forEach var="p" items="${payments}"><div class="small border-bottom py-1">Paid Rs ${p.amountText} &middot; ${p.method} &middot; ${p.receipt_no}</div></c:forEach>
  </div>
</main>
</body>
</html>

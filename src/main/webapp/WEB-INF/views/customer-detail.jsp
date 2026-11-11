<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Customer account - CanKhata</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<%@ include file="_nav.jspf" %>
<main class="container pb-4">
  <c:if test="${saved}"><div class="alert alert-success py-2">Saved.</div></c:if>
  <c:if test="${not empty formError}"><div class="alert alert-danger py-2"><c:out value="${formError}"/></div></c:if>
  <a class="small" href="${ctx}/owner/customers">&larr; Customers</a>
  <h1 class="h4 my-2"><c:out value="${c.name}"/> <c:if test="${not c.active}"><span class="badge bg-secondary">Inactive</span></c:if></h1>
  <div class="text-muted small mb-3"><c:out value="${c.phone}"/> &middot; <c:out value="${c.address}"/> &middot; <c:out value="${c.route_name}"/></div>

  <div class="row g-3 mb-3">
    <div class="col-6 col-md-3"><div class="card p-3"><div class="small text-muted">Owes</div><div class="h4 mb-0 ${c.balancePaise > 0 ? 'text-danger' : ''}">Rs ${c.balanceText}</div></div></div>
    <div class="col-6 col-md-3"><div class="card p-3"><div class="small text-muted">Deposit held</div><div class="h4 mb-0">Rs ${c.depositText}</div></div></div>
    <c:forEach var="k" items="${cans}"><div class="col-6 col-md-3"><div class="card p-3"><div class="small text-muted"><c:out value="${k.name}"/> held</div><div class="h4 mb-0">${k.held}</div></div></div></c:forEach>
  </div>

  <div class="row g-3">
    <div class="col-12 col-lg-6">
      <div class="card p-3 mb-3">
        <h2 class="h6">Standing orders</h2>
        <c:forEach var="s" items="${subs}">
          <div class="border-bottom pb-2 mb-2">
            <div class="fw-semibold">${s.qty} x <c:out value="${s.can_name}"/> &middot; ${s.frequency}
              <c:if test="${not s.active}"><span class="badge bg-secondary">Ended</span></c:if></div>
            <div class="small text-muted">From ${s.start_date}<c:if test="${not empty s.end_date}"> to ${s.end_date}</c:if></div>
            <c:forEach var="k" items="${s.skips}">
              <form method="post" action="${ctx}/owner/customer" class="d-inline">
                <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="id" value="${c.id}">
                <input type="hidden" name="action" value="unskip"><input type="hidden" name="skipId" value="${k.id}">
                <span class="skip-chip">Skipping ${k.skip_date} <button class="btn btn-link btn-sm p-0 align-baseline" type="submit">Undo</button></span>
              </form>
            </c:forEach>
            <c:if test="${s.active}">
              <form method="post" action="${ctx}/owner/customer" class="d-flex gap-2 mt-2">
                <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="id" value="${c.id}">
                <input type="hidden" name="subId" value="${s.id}">
                <input class="form-control form-control-sm" type="date" name="date" min="${today}" required>
                <button class="btn btn-sm btn-outline-secondary" name="action" value="skip" type="submit">Skip day</button>
                <button class="btn btn-sm btn-outline-danger" name="action" value="endSub" type="submit" formnovalidate>End</button>
              </form>
            </c:if>
          </div>
        </c:forEach>
        <c:if test="${empty subs}"><p class="text-muted">No standing order yet.</p></c:if>

        <form method="post" action="${ctx}/owner/customer" class="row g-2 mt-1">
          <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="id" value="${c.id}"><input type="hidden" name="action" value="addSub">
          <div class="col-12"><div class="small fw-semibold">Add a standing order</div></div>
          <div class="col-8"><select class="form-select" name="canTypeId" aria-label="Can type"><c:forEach var="t" items="${canTypes}"><option value="${t.id}"><c:out value="${t.name}"/></option></c:forEach></select></div>
          <div class="col-4"><input class="form-control" name="qty" type="number" min="1" max="100" value="1" aria-label="Quantity"></div>
          <div class="col-12">
            <select class="form-select" name="frequency" aria-label="How often">
              <option value="DAILY">Every day</option><option value="ALTERNATE">Every 2nd day</option><option value="WEEKDAYS">Chosen weekdays</option>
            </select></div>
          <div class="col-12 small">
            Weekdays (only for "Chosen weekdays"):
            <c:forEach var="d" items="${['Mon','Tue','Wed','Thu','Fri','Sat','Sun']}" varStatus="st">
              <label class="me-2"><input type="checkbox" name="days" value="${st.index + 1}"> ${d}</label>
            </c:forEach></div>
          <div class="col-6"><label class="form-label small mb-0" for="start">Start</label><input class="form-control" id="start" type="date" name="start" value="${today}" required></div>
          <div class="col-6"><label class="form-label small mb-0" for="end">End (optional)</label><input class="form-control" id="end" type="date" name="end"></div>
          <div class="col-12"><button class="btn btn-ck w-100" type="submit">Add standing order</button></div>
        </form>
      </div>

      <div class="card p-3">
        <h2 class="h6">Recent deliveries</h2>
        <c:forEach var="d" items="${deliveries}">
          <div class="small border-bottom py-1">${d.sheet_date} &middot; <c:out value="${d.can_name}"/> &middot; ${d.status}
            <c:if test="${d.status == 'DELIVERED'}">&middot; ${d.delivered_qty} in, ${d.empties_collected} empties back</c:if></div>
        </c:forEach>
        <c:if test="${empty deliveries}"><p class="text-muted mb-0 small">No deliveries yet.</p></c:if>
      </div>
    </div>

    <div class="col-12 col-lg-6">
      <div class="card p-3 mb-3">
        <h2 class="h6">Receive a payment</h2>
        <form method="post" action="${ctx}/owner/customer" class="row g-2">
          <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="id" value="${c.id}"><input type="hidden" name="action" value="payment">
          <input type="hidden" name="token" value="${token}">
          <div class="col-6"><input class="form-control" name="amount" inputmode="decimal" placeholder="Amount (Rs)" required></div>
          <div class="col-6"><select class="form-select" name="method" aria-label="Method"><option value="CASH">Cash</option><option value="UPI">UPI</option></select></div>
          <div class="col-12"><button class="btn btn-ck w-100" type="submit">Save payment</button></div>
        </form>
        <c:forEach var="p" items="${payments}">
          <div class="small border-bottom py-1">${p.paid_at} &middot; Rs ${p.amountText} &middot; ${p.method} &middot; ${p.receipt_no}</div>
        </c:forEach>
      </div>

      <div class="card p-3 mb-3">
        <h2 class="h6">Deposit</h2>
        <form method="post" action="${ctx}/owner/customer" class="row g-2">
          <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="id" value="${c.id}"><input type="hidden" name="action" value="deposit">
          <div class="col-12"><select class="form-select" name="type" aria-label="Type"><option value="COLLECTED">Collected</option><option value="REFUNDED">Refunded</option></select></div>
          <div class="col-6"><input class="form-control" name="amount" inputmode="decimal" placeholder="Amount (Rs)" required></div>
          <div class="col-6"><input class="form-control" name="cans" type="number" min="0" max="1000" placeholder="Cans" value="1" required></div>
          <div class="col-12"><button class="btn btn-outline-secondary w-100" type="submit">Save deposit entry</button></div>
        </form>
      </div>

      <div class="card p-3 mb-3">
        <h2 class="h6">Lost, damaged or corrected cans</h2>
        <form method="post" action="${ctx}/owner/customer" class="row g-2">
          <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="id" value="${c.id}"><input type="hidden" name="action" value="adjust">
          <div class="col-6"><select class="form-select" name="canTypeId" aria-label="Can type"><c:forEach var="t" items="${canTypes}"><option value="${t.id}"><c:out value="${t.name}"/></option></c:forEach></select></div>
          <div class="col-6"><select class="form-select" name="entryType" aria-label="Entry"><option value="LOST">Lost</option><option value="DAMAGED">Damaged</option><option value="ADJUSTMENT">Correction (+/-)</option></select></div>
          <div class="col-4"><input class="form-control" name="qty" type="number" value="1" aria-label="Quantity"></div>
          <div class="col-8"><input class="form-control" name="note" maxlength="255" placeholder="Reason (required)" required></div>
          <div class="col-12"><button class="btn btn-outline-secondary w-100" type="submit">Save</button></div>
        </form>
        <c:forEach var="l" items="${ledger}">
          <div class="small border-bottom py-1">${l.created_at} &middot; ${l.entry_type} ${l.qty} <c:out value="${l.can_name}"/> <c:out value="${l.note}"/></div>
        </c:forEach>
      </div>

      <div class="card p-3">
        <h2 class="h6">Invoices</h2>
        <c:forEach var="i" items="${invoices}"><div class="small border-bottom py-1">${i.invoice_month} &middot; Rs ${i.totalText} &middot; ${i.status}</div></c:forEach>
        <c:if test="${empty invoices}"><p class="text-muted small mb-2">None yet. Generate them from Billing.</p></c:if>
        <c:if test="${empty c.user_id}">
          <form method="post" action="${ctx}/owner/customer" class="d-flex gap-2 mt-2">
            <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="id" value="${c.id}"><input type="hidden" name="action" value="login">
            <input class="form-control form-control-sm" type="password" name="password" autocomplete="new-password" placeholder="Set a login password" maxlength="72" required>
            <button class="btn btn-sm btn-outline-secondary text-nowrap" type="submit">Create login</button>
          </form>
        </c:if>
        <c:if test="${not empty c.user_id}">
          <div class="small text-muted mt-2">Has a login (phone number + password). Forgot it? Set a new one:</div>
          <form method="post" action="${ctx}/owner/customer" class="d-flex gap-2 mt-1">
            <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="id" value="${c.id}"><input type="hidden" name="action" value="resetLogin">
            <input class="form-control form-control-sm" type="password" name="password" autocomplete="new-password" placeholder="New password" maxlength="72" required>
            <button class="btn btn-sm btn-outline-secondary text-nowrap" type="submit">Reset password</button>
          </form>
        </c:if>
      </div>
    </div>
  </div>
</main>
</body>
</html>

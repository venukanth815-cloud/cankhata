<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Home - CanKhata</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<%@ include file="_nav.jspf" %>
<main class="container pb-4">
  <h1 class="h4 mb-3">Welcome, <c:out value="${user.name}"/></h1>

  <div class="row g-3">
    <c:if test="${user.owner}">
      <div class="col-12 col-sm-6 col-lg-4">
        <a class="tile" href="${ctx}/owner/users"><div class="card p-3"><div class="h5 mb-1">👥 Staff</div><div class="text-muted small">Add owners and delivery boys</div></div></a>
      </div>
      <div class="col-12 col-sm-6 col-lg-4">
        <a class="tile" href="${ctx}/owner/customers"><div class="card p-3"><div class="h5 mb-1">🏠 Customers</div><div class="text-muted small">Add, search, edit customers</div></div></a>
      </div>
      <div class="col-12 col-sm-6 col-lg-4">
        <a class="tile" href="${ctx}/owner/routes"><div class="card p-3"><div class="h5 mb-1">🛣️ Routes</div><div class="text-muted small">Streets and delivery boys</div></div></a>
      </div>
      <div class="col-12 col-sm-6 col-lg-4">
        <a class="tile" href="${ctx}/owner/cantypes"><div class="card p-3"><div class="h5 mb-1">🫙 Can types</div><div class="text-muted small">Prices and deposits</div></div></a>
      </div>
      <div class="col-12 col-sm-6 col-lg-4">
        <a class="tile" href="${ctx}/owner/sheet"><div class="card p-3"><div class="h5 mb-1">🗓️ Daily sheet</div><div class="text-muted small">Pending ${stats.pending} &middot; done ${stats.done} &middot; missed ${stats.missed} &middot; cash Rs ${stats.cashText}</div></div></a>
      </div>
      <div class="col-12 col-sm-6 col-lg-4">
        <a class="tile" href="${ctx}/owner/invoices"><div class="card p-3"><div class="h5 mb-1">🧾 Billing</div><div class="text-muted small">Invoices and who owes money</div></div></a>
      </div>
    </c:if>

    <c:if test="${user.deliveryBoy}">
      <div class="col-12 col-sm-6 col-lg-4">
        <a class="tile" href="${ctx}/driver/today"><div class="card p-3"><div class="h5 mb-1">🚚 Today's deliveries</div><div class="text-muted small">Mark delivered, empties, cash</div></div></a>
      </div>
    </c:if>

    <c:if test="${user.customer}">
      <div class="col-12 col-sm-6 col-lg-4">
        <a class="tile" href="${ctx}/customer/home"><div class="card p-3"><div class="h5 mb-1">🫙 My cans and bills</div><div class="text-muted small">Cans, balance, skip a day</div></div></a>
      </div>
    </c:if>
  </div>
  <c:if test="${user.owner}">
    <p class="small text-center mt-4 mb-0"><a class="text-muted" href="${ctx}/owner/monitor">Server monitor</a></p>
  </c:if>
</main>
</body>
</html>

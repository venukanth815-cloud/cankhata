<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Billing - CanKhata</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<%@ include file="_nav.jspf" %>
<main class="container pb-4">
  <c:if test="${saved}"><div class="alert alert-success py-2">Saved.</div></c:if>
  <c:if test="${not empty formError}"><div class="alert alert-danger py-2"><c:out value="${formError}"/></div></c:if>
  <h1 class="h4 mb-3">Billing</h1>
  <c:if test="${not empty created}"><div class="alert alert-success py-2">${created} new invoice(s) created.</div></c:if>
  <div class="row g-3">
    <div class="col-12 col-lg-7">
      <div class="card p-3">
        <form method="get" action="${ctx}/owner/invoices" class="d-flex gap-2 mb-3">
          <input class="form-control" type="month" name="month" value="${month}" required aria-label="Month">
          <button class="btn btn-ck" type="submit">Show</button>
        </form>
        <form method="post" action="${ctx}/owner/invoices" class="mb-3">
          <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="month" value="${month}">
          <button class="btn btn-outline-secondary" type="submit">Generate invoices for ${month}</button>
        </form>
        <c:forEach var="i" items="${invoices}">
          <div class="d-flex justify-content-between border-bottom py-2">
            <a href="${ctx}/owner/customer?id=${i.customer_id}"><c:out value="${i.name}"/></a>
            <span>Rs ${i.totalText} <span class="badge ${i.status == 'PAID' ? 'bg-success' : 'bg-warning text-dark'}">${i.status}</span></span>
          </div>
        </c:forEach>
        <c:if test="${empty invoices}"><p class="text-muted mb-0">No invoices for this month.</p></c:if>
      </div>
    </div>
    <div class="col-12 col-lg-5">
      <div class="card p-3">
        <h2 class="h6">Who owes money</h2>
        <c:forEach var="o" items="${owing}">
          <div class="d-flex justify-content-between border-bottom py-2">
            <a href="${ctx}/owner/customer?id=${o.id}"><c:out value="${o.name}"/></a><strong>Rs ${o.owedText}</strong>
          </div>
        </c:forEach>
        <c:if test="${empty owing}"><p class="text-muted mb-0">Nobody owes anything.</p></c:if>
      </div>
    </div>
  </div>
</main>
</body>
</html>

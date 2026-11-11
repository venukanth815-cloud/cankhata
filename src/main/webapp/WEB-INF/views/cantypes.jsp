<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Can types - CanKhata</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<%@ include file="_nav.jspf" %>
<main class="container pb-4">
  <h1 class="h4 mb-3">Can types and prices</h1>
  <c:if test="${saved}"><div class="alert alert-success py-2">Saved.</div></c:if>
  <c:if test="${not empty formError}"><div class="alert alert-danger py-2"><c:out value="${formError}"/></div></c:if>

  <div class="row g-3">
    <div class="col-12 col-lg-4">
      <div class="card p-3">
        <h2 class="h6 mb-3">Add a can type</h2>
        <form method="post" action="${ctx}/owner/cantypes">
          <input type="hidden" name="_csrf" value="${csrf}">
          <div class="mb-2"><label class="form-label" for="name">Name</label>
            <input class="form-control" id="name" name="name" maxlength="50" placeholder="20 Litre Can" required></div>
          <div class="mb-2"><label class="form-label" for="price">Price per refill (Rs)</label>
            <input class="form-control" id="price" name="price" inputmode="decimal" placeholder="40" required></div>
          <div class="mb-3"><label class="form-label" for="deposit">Can deposit (Rs)</label>
            <input class="form-control" id="deposit" name="deposit" inputmode="decimal" placeholder="250" required></div>
          <button class="btn btn-ck w-100" type="submit">Add</button>
        </form>
      </div>
    </div>
    <div class="col-12 col-lg-8">
      <div class="card p-3">
        <c:forEach var="t" items="${types}">
          <form method="post" action="${ctx}/owner/cantypes" class="row g-2 align-items-end border-bottom pb-3 mb-3">
            <input type="hidden" name="_csrf" value="${csrf}">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="id" value="${t.id}">
            <div class="col-12 col-md-4"><label class="form-label small">Name</label>
              <input class="form-control" name="name" maxlength="50" value="<c:out value='${t.name}'/>" required></div>
            <div class="col-6 col-md-3"><label class="form-label small">Price (Rs)</label>
              <input class="form-control" name="price" inputmode="decimal" value="${t.priceText}" required></div>
            <div class="col-6 col-md-3"><label class="form-label small">Deposit (Rs)</label>
              <input class="form-control" name="deposit" inputmode="decimal" value="${t.depositText}" required></div>
            <div class="col-12 col-md-2"><button class="btn btn-ck w-100" type="submit">Save</button></div>
          </form>
        </c:forEach>
        <c:if test="${empty types}"><p class="text-muted mb-0">No can types yet.</p></c:if>
      </div>
    </div>
  </div>
</main>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Routes - CanKhata</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<%@ include file="_nav.jspf" %>
<main class="container pb-4">
  <h1 class="h4 mb-3">Routes</h1>
  <c:if test="${saved}"><div class="alert alert-success py-2">Saved.</div></c:if>
  <c:if test="${not empty formError}"><div class="alert alert-danger py-2"><c:out value="${formError}"/></div></c:if>

  <div class="row g-3">
    <div class="col-12 col-lg-4">
      <div class="card p-3">
        <h2 class="h6 mb-3">Add a route</h2>
        <form method="post" action="${ctx}/owner/routes">
          <input type="hidden" name="_csrf" value="${csrf}">
          <div class="mb-2"><label class="form-label" for="name">Route name</label>
            <input class="form-control" id="name" name="name" maxlength="100" placeholder="Anna Nagar East" required></div>
          <div class="mb-3"><label class="form-label" for="boyId">Delivery boy</label>
            <select class="form-select" id="boyId" name="boyId">
              <option value="">Not assigned yet</option>
              <c:forEach var="b" items="${boys}"><option value="${b.id}"><c:out value="${b.name}"/></option></c:forEach>
            </select></div>
          <button class="btn btn-ck w-100" type="submit">Add route</button>
        </form>
      </div>
    </div>
    <div class="col-12 col-lg-8">
      <div class="card p-3">
        <c:forEach var="r" items="${routes}">
          <form method="post" action="${ctx}/owner/routes" class="row g-2 align-items-end border-bottom pb-3 mb-3">
            <input type="hidden" name="_csrf" value="${csrf}">
            <input type="hidden" name="action" value="assign">
            <input type="hidden" name="id" value="${r.id}">
            <div class="col-12 col-md-5"><div class="fw-semibold"><c:out value="${r.name}"/></div></div>
            <div class="col-8 col-md-5">
              <select class="form-select" name="boyId" aria-label="Delivery boy">
                <option value="">Not assigned</option>
                <c:forEach var="b" items="${boys}"><option value="${b.id}" ${b.id == r.deliveryBoyId ? 'selected' : ''}><c:out value="${b.name}"/></option></c:forEach>
              </select></div>
            <div class="col-4 col-md-2"><button class="btn btn-ck w-100" type="submit">Save</button></div>
          </form>
        </c:forEach>
        <c:if test="${empty routes}"><p class="text-muted mb-0">No routes yet.</p></c:if>
      </div>
    </div>
  </div>
</main>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Customers - CanKhata</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<%@ include file="_nav.jspf" %>
<main class="container pb-4">
  <h1 class="h4 mb-3">Customers</h1>
  <c:if test="${saved}"><div class="alert alert-success py-2">Saved.</div></c:if>
  <c:if test="${not empty formError}"><div class="alert alert-danger py-2"><c:out value="${formError}"/></div></c:if>

  <div class="row g-3">
    <div class="col-12 col-lg-4">
      <div class="card p-3">
        <h2 class="h6 mb-3">${form.id > 0 ? 'Edit customer' : 'Add a customer'}</h2>
        <form method="post" action="${ctx}/owner/customers">
          <input type="hidden" name="_csrf" value="${csrf}">
          <c:if test="${form.id > 0}"><input type="hidden" name="id" value="${form.id}"></c:if>
          <div class="mb-2"><label class="form-label" for="name">Name</label>
            <input class="form-control" id="name" name="name" maxlength="100" value="<c:out value='${form.name}'/>" required></div>
          <div class="mb-2"><label class="form-label" for="phone">Mobile number</label>
            <input class="form-control" id="phone" name="phone" inputmode="numeric" maxlength="15" value="<c:out value='${form.phone}'/>" required></div>
          <div class="mb-2"><label class="form-label" for="address">Address</label>
            <input class="form-control" id="address" name="address" maxlength="255" value="<c:out value='${form.address}'/>" required></div>
          <div class="mb-2"><label class="form-label" for="landmark">Landmark (optional)</label>
            <input class="form-control" id="landmark" name="landmark" maxlength="150" value="<c:out value='${form.landmark}'/>"></div>
          <div class="mb-2"><label class="form-label" for="routeId">Route</label>
            <select class="form-select" id="routeId" name="routeId">
              <option value="">No route</option>
              <c:forEach var="r" items="${routes}"><option value="${r.id}" ${r.id == form.routeId ? 'selected' : ''}><c:out value="${r.name}"/></option></c:forEach>
            </select></div>
          <div class="row g-2 mb-2">
            <div class="col-6"><label class="form-label" for="rate">Special rate (Rs)</label>
              <input class="form-control" id="rate" name="rate" inputmode="decimal" placeholder="optional" value="<c:out value='${form_rate}'/>"></div>
            <div class="col-6"><label class="form-label" for="credit">Credit limit (Rs)</label>
              <input class="form-control" id="credit" name="credit" inputmode="decimal" placeholder="0" value="<c:out value='${form_credit}'/>"></div>
          </div>
          <c:if test="${form.id > 0}">
            <div class="form-check mb-3"><input class="form-check-input" type="checkbox" id="active" name="active" ${form.active ? 'checked' : ''}>
              <label class="form-check-label" for="active">Active</label></div>
          </c:if>
          <c:if test="${not (form.id > 0)}"><input type="hidden" name="active" value="on"></c:if>
          <button class="btn btn-ck w-100" type="submit">${form.id > 0 ? 'Save changes' : 'Add customer'}</button>
          <c:if test="${form.id > 0}"><a class="btn btn-link w-100" href="${ctx}/owner/customers">Cancel</a></c:if>
        </form>
      </div>
    </div>

    <div class="col-12 col-lg-8">
      <div class="card p-3">
        <form method="get" action="${ctx}/owner/customers" class="row g-2 mb-3">
          <div class="col-12 col-md-6"><input class="form-control" name="q" placeholder="Search name, phone or address" value="<c:out value='${q}'/>" maxlength="100"></div>
          <div class="col-8 col-md-4">
            <select class="form-select" name="route" aria-label="Route">
              <option value="">All routes</option>
              <c:forEach var="r" items="${routes}"><option value="${r.id}" ${r.id == routeFilter ? 'selected' : ''}><c:out value="${r.name}"/></option></c:forEach>
            </select></div>
          <div class="col-4 col-md-2"><button class="btn btn-ck w-100" type="submit">Search</button></div>
        </form>
        <div class="text-muted small mb-2">${total} customer(s)</div>

        <div class="d-md-none">
          <c:forEach var="cu" items="${customers}">
            <div class="user-card">
              <div class="fw-semibold"><c:out value="${cu.name}"/> <c:if test="${not cu.active}"><span class="badge bg-secondary">Inactive</span></c:if></div>
              <div class="small"><c:out value="${cu.phone}"/> &middot; <c:out value="${cu.routeName}"/></div>
              <div class="small text-muted"><c:out value="${cu.address}"/></div>
              <a class="small" href="${ctx}/owner/customers?edit=${cu.id}">Edit</a> &middot; <a class="small" href="${ctx}/owner/customer?id=${cu.id}">Account</a>
            </div>
          </c:forEach>
        </div>
        <div class="d-none d-md-block table-responsive">
          <table class="table align-middle mb-0">
            <thead><tr><th>Name</th><th>Phone</th><th>Route</th><th>Address</th><th></th></tr></thead>
            <tbody>
              <c:forEach var="cu" items="${customers}">
                <tr>
                  <td><c:out value="${cu.name}"/> <c:if test="${not cu.active}"><span class="badge bg-secondary">Inactive</span></c:if></td>
                  <td><c:out value="${cu.phone}"/></td>
                  <td><c:out value="${cu.routeName}"/></td>
                  <td><c:out value="${cu.address}"/></td>
                  <td class="text-nowrap"><a href="${ctx}/owner/customers?edit=${cu.id}">Edit</a> &middot; <a href="${ctx}/owner/customer?id=${cu.id}">Account</a></td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
        <c:if test="${empty customers}"><p class="text-muted mb-0">No customers found.</p></c:if>

        <c:if test="${pages > 1}">
          <nav class="mt-3 d-flex justify-content-between align-items-center">
            <c:url var="prevUrl" value="/owner/customers"><c:param name="q" value="${q}"/><c:param name="route" value="${routeFilter}"/><c:param name="page" value="${page - 1}"/></c:url>
            <c:url var="nextUrl" value="/owner/customers"><c:param name="q" value="${q}"/><c:param name="route" value="${routeFilter}"/><c:param name="page" value="${page + 1}"/></c:url>
            <a class="btn btn-outline-secondary ${page <= 1 ? 'disabled' : ''}" href="${prevUrl}">Previous</a>
            <span class="small text-muted">Page ${page} of ${pages}</span>
            <a class="btn btn-outline-secondary ${page >= pages ? 'disabled' : ''}" href="${nextUrl}">Next</a>
          </nav>
        </c:if>
      </div>
    </div>
  </div>
</main>
</body>
</html>

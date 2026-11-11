<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Staff - CanKhata</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<%@ include file="_nav.jspf" %>
<main class="container pb-4">
  <h1 class="h4 mb-3">Staff</h1>

  <c:if test="${created}"><div class="alert alert-success py-2">User created.</div></c:if>
  <c:if test="${resetDone}"><div class="alert alert-success py-2">Password reset. Tell the person their new password.</div></c:if>
  <c:if test="${not empty formError}"><div class="alert alert-danger py-2"><c:out value="${formError}"/></div></c:if>

  <div class="row g-3">
    <%-- Create form --%>
    <div class="col-12 col-lg-4">
      <div class="card p-3">
        <h2 class="h6 mb-3">Add a user</h2>
        <form method="post" action="${ctx}/owner/users">
          <input type="hidden" name="_csrf" value="${csrf}">
          <div class="mb-2">
            <label class="form-label" for="name">Name</label>
            <input class="form-control" id="name" name="name" maxlength="100" value="<c:out value='${form_name}'/>" required>
          </div>
          <div class="mb-2">
            <label class="form-label" for="phone">Mobile number</label>
            <input class="form-control" id="phone" name="phone" inputmode="numeric" maxlength="15" value="<c:out value='${form_phone}'/>" required>
          </div>
          <div class="mb-2">
            <label class="form-label" for="email">Email (optional)</label>
            <input class="form-control" id="email" name="email" type="email" maxlength="150" value="<c:out value='${form_email}'/>">
          </div>
          <div class="mb-2">
            <label class="form-label" for="role">Role</label>
            <select class="form-select" id="role" name="role" required>
              <option value="DELIVERY_BOY" ${form_role == 'DELIVERY_BOY' || empty form_role ? 'selected' : ''}>Delivery boy</option>
              <option value="OWNER" ${form_role == 'OWNER' ? 'selected' : ''}>Owner</option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-label" for="password">Password</label>
            <input class="form-control" id="password" name="password" type="password" autocomplete="new-password" maxlength="72" required>
            <div class="form-text">At least 8 characters, with a letter and a digit.</div>
          </div>
          <button class="btn btn-ck w-100" type="submit">Create user</button>
        </form>
      </div>
    </div>

    <%-- List --%>
    <div class="col-12 col-lg-8">
      <div class="card p-3">
        <form method="get" action="${ctx}/owner/users" class="d-flex gap-2 mb-3">
          <input class="form-control" name="q" placeholder="Search name, phone or email" value="<c:out value='${q}'/>" maxlength="100">
          <button class="btn btn-ck" type="submit">Search</button>
        </form>
        <div class="text-muted small mb-2">${total} user(s)</div>

        <%-- Phones: stacked cards. Wider screens: a table. --%>
        <div class="d-md-none">
          <c:forEach var="u" items="${users}">
            <div class="user-card">
              <div class="fw-semibold"><c:out value="${u.name}"/> <span class="badge bg-secondary"><c:out value="${u.role.label}"/></span></div>
              <div class="small"><c:out value="${u.phone}"/></div>
              <c:if test="${not empty u.email}"><div class="small text-muted"><c:out value="${u.email}"/></div></c:if>
              <form method="post" action="${ctx}/owner/users" class="d-flex gap-1 mt-1">
                <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="action" value="reset"><input type="hidden" name="userId" value="${u.id}">
                <input class="form-control form-control-sm" type="password" name="password" autocomplete="new-password" placeholder="New password" maxlength="72" required>
                <button class="btn btn-sm btn-outline-secondary text-nowrap" type="submit">Reset</button>
              </form>
            </div>
          </c:forEach>
        </div>
        <div class="d-none d-md-block table-responsive">
          <table class="table align-middle mb-0">
            <thead><tr><th>Name</th><th>Role</th><th>Phone</th><th>Email</th><th>Reset password</th></tr></thead>
            <tbody>
              <c:forEach var="u" items="${users}">
                <tr>
                  <td><c:out value="${u.name}"/></td>
                  <td><c:out value="${u.role.label}"/></td>
                  <td><c:out value="${u.phone}"/></td>
                  <td><c:out value="${u.email}"/></td>
                  <td><form method="post" action="${ctx}/owner/users" class="d-flex gap-1 ">
                <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="action" value="reset"><input type="hidden" name="userId" value="${u.id}">
                <input class="form-control form-control-sm" type="password" name="password" autocomplete="new-password" placeholder="New password" maxlength="72" required>
                <button class="btn btn-sm btn-outline-secondary text-nowrap" type="submit">Reset</button>
              </form></td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
        <c:if test="${empty users}"><p class="text-muted mb-0">No users found.</p></c:if>

        <c:if test="${pages > 1}">
          <nav class="mt-3 d-flex justify-content-between align-items-center">
            <c:url var="prevUrl" value="/owner/users"><c:param name="q" value="${q}"/><c:param name="page" value="${page - 1}"/></c:url>
            <c:url var="nextUrl" value="/owner/users"><c:param name="q" value="${q}"/><c:param name="page" value="${page + 1}"/></c:url>
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

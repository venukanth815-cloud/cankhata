<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Change password - CanKhata</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<%@ include file="_nav.jspf" %>
<main class="container narrow-wrap pb-4">
  <h1 class="h4 mb-3 text-center">Change my password</h1>
  <c:if test="${saved}"><div class="alert alert-success py-2">Password changed.</div></c:if>
  <c:if test="${not empty formError}"><div class="alert alert-danger py-2"><c:out value="${formError}"/></div></c:if>
  <div class="card p-3">
    <form method="post" action="${ctx}/account/password">
      <input type="hidden" name="_csrf" value="${csrf}">
      <div class="mb-2">
        <label class="form-label" for="oldPassword">Current password</label>
        <input class="form-control" id="oldPassword" name="oldPassword" type="password" autocomplete="current-password" maxlength="72" required>
      </div>
      <div class="mb-2">
        <label class="form-label" for="newPassword">New password</label>
        <input class="form-control" id="newPassword" name="newPassword" type="password" autocomplete="new-password" maxlength="72" required>
        <div class="form-text">At least 8 characters, with a letter and a digit.</div>
      </div>
      <div class="mb-3">
        <label class="form-label" for="confirm">Repeat new password</label>
        <input class="form-control" id="confirm" name="confirm" type="password" autocomplete="new-password" maxlength="72" required>
      </div>
      <button class="btn btn-ck w-100" type="submit">Change password</button>
    </form>
  </div>
</main>
</body>
</html>

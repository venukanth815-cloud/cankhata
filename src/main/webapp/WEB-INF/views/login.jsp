<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Login - CanKhata</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<div class="container px-3">
  <div class="login-wrap">
    <div class="text-center mb-4">
      <h1 class="h2 mb-1">💧 CanKhata</h1>
      <p class="text-muted mb-0">Water can delivery and deposit ledger</p>
    </div>
    <div class="card p-4">
      <h2 class="h5 mb-3">Login</h2>
      <c:if test="${not empty error}">
        <div class="alert alert-danger py-2" role="alert"><c:out value="${error}"/></div>
      </c:if>
      <form method="post" action="${ctx}/login" autocomplete="on">
        <div class="mb-3">
          <label class="form-label" for="login">Phone or email</label>
          <input class="form-control" id="login" name="login" value="<c:out value='${login}'/>"
                 inputmode="text" autocomplete="username" maxlength="150" required autofocus>
        </div>
        <div class="mb-3">
          <label class="form-label" for="password">Password</label>
          <input class="form-control" id="password" name="password" type="password"
                 autocomplete="current-password" maxlength="100" required>
        </div>
        <button class="btn btn-ck w-100" type="submit">Login</button>
      </form>
    </div>
  </div>
</div>
</body>
</html>

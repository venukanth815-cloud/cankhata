<%@ page contentType="text/html;charset=UTF-8" session="false" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="status" value="${not empty errorStatus ? errorStatus : (not empty requestScope['jakarta.servlet.error.status_code'] ? requestScope['jakarta.servlet.error.status_code'] : 500)}"/>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Error ${status} - CanKhata</title>
  <link rel="stylesheet" href="${ctx}/assets/vendor/bootstrap.min.css">
  <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body>
<div class="container login-wrap">
  <div class="card p-4 text-center">
    <h1 class="display-4">${status}</h1>
    <p class="lead">
      <c:choose>
        <c:when test="${not empty errorMessage}"><c:out value="${errorMessage}"/></c:when>
        <c:when test="${status == 404}">We could not find that page.</c:when>
        <c:otherwise>Something went wrong. Please try again.</c:otherwise>
      </c:choose>
    </p>
    <a class="btn btn-ck" href="${ctx}/home">Go to home</a>
  </div>
</div>
</body>
</html>

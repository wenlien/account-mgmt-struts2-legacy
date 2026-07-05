<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Login - Anycompany Business Capital (ABC)</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <div class="app-header app-header--login">
        <div class="header-left">
            <img src="${pageContext.request.contextPath}/images/abc-logo.svg"
                 alt="ABC" class="header-logo" style="width:7.5%; height:auto;"/>
            <span class="header-title header-title--login">Anycompany Business Capital (ABC)</span>
        </div>
    </div>
    <div class="main-content" style="max-width:400px; margin:60px auto;">
        <h2>Login</h2>
        <% if (request.getParameter("error") != null) { %>
            <p style="color:red;">Invalid username or password.</p>
        <% } %>
        <% if (request.getParameter("logout") != null) { %>
            <p style="color:green;">You have been logged out.</p>
        <% } %>
        <form action="login" method="post" class="account-form" style="display:block;">
            <p>
                <label for="username">Username</label><br/>
                <input type="text" id="username" name="username" size="30"/>
            </p>
            <p>
                <label for="password">Password</label><br/>
                <input type="password" id="password" name="password" size="30"/>
            </p>
            <input type="submit" value="Login" class="header-logout-btn" style="width:100%; margin-top:10px;"/>
        </form>
    </div>
</body>
</html>

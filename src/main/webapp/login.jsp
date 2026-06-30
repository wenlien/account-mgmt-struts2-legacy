<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Login - Account Management</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <h1>Login</h1>
    <% if (request.getParameter("error") != null) { %>
        <p style="color:red;">Invalid username or password.</p>
    <% } %>
    <% if (request.getParameter("logout") != null) { %>
        <p style="color:green;">You have been logged out.</p>
    <% } %>
    <form action="login" method="post" class="account-form">
        <p>
            <label for="username">Username</label>
            <input type="text" id="username" name="username" />
        </p>
        <p>
            <label for="password">Password</label>
            <input type="password" id="password" name="password" />
        </p>
        <input type="submit" value="Login" />
    </form>
</body>
</html>

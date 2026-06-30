<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Account List</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <h1>Account List</h1>
    <table class="account-table">
        <tr>
            <th>Account No</th>
            <th>Owner</th>
            <th>Balance</th>
            <th>Status</th>
            <th>Opened</th>
            <th>Transactions</th>
            <th>Edit</th>
            <th>Delete</th>
        </tr>
        <c:forEach items="${accountList}" var="acct">
            <tr>
                <td>${acct.accountNo}</td>
                <td>${acct.ownerName}</td>
                <td>${acct.balance}</td>
                <td>${acct.status}</td>
                <td>${acct.openedDate}</td>
                <td><a href="transactions?accountNo=${acct.accountNo}">View</a></td>
                <td><a href="editAccount?accountNo=${acct.accountNo}">Edit</a></td>
                <td><a href="deleteAccount?accountNo=${acct.accountNo}">Delete</a></td>
            </tr>
        </c:forEach>
    </table>
    <br>
    <a href="addAccount">Add New Account</a>
    <form action="logout" method="post" style="display:inline; margin-left:20px;">
        <input type="submit" value="Logout" />
    </form>
</body>
</html>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Transactions</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <h1>Transactions for Account ${accountNo}</h1>

    <s:if test="hasActionErrors()">
        <div style="color:red;">
            <s:actionerror/>
        </div>
    </s:if>

    <h2>Deposit</h2>
    <form action="deposit" method="post" class="account-form">
        <input type="hidden" name="accountNo" value="${accountNo}"/>
        <s:textfield name="amount" label="%{getText('label.amount')}"/>
        <input type="submit" value="Deposit">
    </form>

    <h2>Withdraw</h2>
    <form action="withdraw" method="post" class="account-form">
        <input type="hidden" name="accountNo" value="${accountNo}"/>
        <s:textfield name="amount" label="%{getText('label.amount')}"/>
        <input type="submit" value="Withdraw">
    </form>

    <h2>History</h2>
    <table class="account-table">
        <tr>
            <th>Tx ID</th>
            <th>Type</th>
            <th>Amount</th>
            <th>Created At</th>
        </tr>
        <c:forEach items="${transactionList}" var="tx">
            <tr>
                <td>${tx.txId}</td>
                <td>${tx.type}</td>
                <td>${tx.amount}</td>
                <td>${tx.createdAt}</td>
            </tr>
        </c:forEach>
    </table>
    <br>
    <a href="accountList">Back to Account List</a>
</body>
</html>

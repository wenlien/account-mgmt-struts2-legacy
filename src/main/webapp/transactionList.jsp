<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ page import="com.example.accountmgmt.security.SecurityUtil" %>
<% boolean isAdmin = SecurityUtil.isAdmin(); %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Transactions</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <jsp:include page="header.jsp"/>
    <div class="main-content">
    <h1>Transactions for Account ${accountNo}</h1>

    <s:if test="hasActionErrors()">
        <div style="color:red;">
            <s:actionerror/>
        </div>
    </s:if>

    <%-- item1：admin 只能檢視紀錄，不可代做交易 → 交易表單只對帳戶擁有者本人（非 admin）顯示 --%>
    <% if (isAdmin) { %>
    <p style="color:#555;"><em>Admin is view-only for transactions. Deposit / withdraw / transfer must be performed by the account owner.</em></p>
    <% } else { %>
    <h2>Deposit</h2>
    <form action="deposit" method="post" class="account-form">
        <input type="hidden" name="accountNo" value="${accountNo}"/>
        <label>Amount:</label> <input type="text" name="depositAmount" size="12"/>
        <input type="submit" value="Deposit">
    </form>

    <h2>Withdraw</h2>
    <form action="withdraw" method="post" class="account-form">
        <input type="hidden" name="accountNo" value="${accountNo}"/>
        <label>Amount:</label> <input type="text" name="withdrawAmount" size="12"/>
        <input type="submit" value="Withdraw">
    </form>

    <h2>Transfer</h2>
    <form action="transfer" method="post" class="account-form">
        <input type="hidden" name="accountNo" value="${accountNo}"/>
        <label>To Account:</label> <input type="text" name="toAccountNo" size="10"/>
        <label>Amount:</label> <input type="text" name="transferAmount" size="12"/>
        <label>Transaction Note:</label> <input type="text" name="transferNote" size="8" maxlength="7"/>
        <input type="submit" value="Transfer">
    </form>
    <% } %>

    <h2>Transaction History</h2>
    <table class="account-table">
        <tr>
            <th>Tx ID</th>
            <th>Type</th>
            <th>Amount</th>
            <th>Balance After</th>
            <th>Transfer To</th>
            <th>Transaction Note</th>
            <th>Created At</th>
        </tr>
        <c:forEach items="${transactionList}" var="tx">
            <c:if test="${tx.type != 'STATUS'}">
                <tr>
                    <td>${tx.txId}</td>
                    <td>${tx.type}</td>
                    <td>${tx.amount}</td>
                    <td>${tx.balanceAfter}</td>
                    <td>${tx.targetAccountNo}</td>
                    <td><c:out value="${tx.note}"/></td>
                    <td>${tx.createdAt}</td>
                </tr>
            </c:if>
        </c:forEach>
    </table>

    <h2>Status Change History</h2>
    <table class="account-table">
        <tr>
            <th>Tx ID</th>
            <th>From Status</th>
            <th>To Status</th>
            <th>Status Note</th>
            <th>Created At</th>
        </tr>
        <c:forEach items="${transactionList}" var="tx">
            <c:if test="${tx.type == 'STATUS'}">
                <tr>
                    <td>${tx.txId}</td>
                    <td>${tx.fromStatus}</td>
                    <td>${tx.toStatus}</td>
                    <td>${tx.note}</td>
                    <td>${tx.createdAt}</td>
                </tr>
            </c:if>
        </c:forEach>
    </table>
    <br>
    <a href="accountList">Back to Account List</a>
    </div><!-- .main-content -->
</body>
</html>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Close Account</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <jsp:include page="header.jsp"/>
    <div class="main-content">
    <h1>Close Account</h1>

    <s:if test="hasActionErrors()">
        <div style="color:red;">
            <s:actionerror/>
        </div>
    </s:if>

    <s:if test="hasActionMessages()">
        <div style="color:green;">
            <s:actionmessage/>
        </div>
    </s:if>

    <%-- 單一帳號模式（從 accountList 的 Close 連結進來） --%>
    <c:if test="${account != null}">
        <p>Account No: ${account.accountNo}</p>
        <p>Owner: ${account.ownerName}</p>
        <p>Balance: ${account.balance}（關戶前餘額須為 0）</p>
        <p>Status: ${account.status}</p>

        <form action="closeAccount" method="post" class="account-form">
            <input type="hidden" name="accountNos" value="${account.accountNo}"/>
            <p>
                <label>Close Note（必填）:</label><br/>
                <textarea name="closeNote" rows="3" cols="40"></textarea>
            </p>
            <input type="submit" value="Close Account">
        </form>
    </c:if>

    <%-- 批次模式（從 accountList 勾選多筆進來） --%>
    <c:if test="${account == null && closeTargets != null}">
        <p>Selected accounts to close:</p>
        <table class="account-table">
            <tr><th>Account No</th><th>Owner</th><th>Balance</th><th>Status</th></tr>
            <c:forEach items="${closeTargets}" var="acct">
                <tr>
                    <td>${acct.accountNo}</td>
                    <td>${acct.ownerName}</td>
                    <td>${acct.balance}</td>
                    <td>${acct.status}</td>
                </tr>
            </c:forEach>
        </table>
        <form action="closeAccounts" method="post" class="account-form">
            <c:forEach items="${closeTargets}" var="acct">
                <input type="hidden" name="accountNos" value="${acct.accountNo}"/>
            </c:forEach>
            <p>
                <label>Close Note（統一填寫，必填）:</label><br/>
                <textarea name="closeNote" rows="3" cols="40"></textarea>
            </p>
            <input type="submit" value="Close All Selected">
        </form>
    </c:if>

    <br>
    <a href="accountList">Back to Account List</a>
    </div><!-- .main-content -->
</body>
</html>

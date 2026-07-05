<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Freeze Account</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <jsp:include page="header.jsp"/>
    <div class="main-content">
    <h1>Freeze Account</h1>

    <s:if test="hasActionErrors()">
        <div style="color:red;">
            <s:actionerror/>
        </div>
    </s:if>

    <%-- 單一帳號模式（從 accountList 的 Freeze 進來） --%>
    <c:if test="${account != null}">
        <p>Account No: ${account.accountNo}</p>
        <p>Owner: ${account.ownerName}</p>
        <p>Status: ${account.status}</p>

        <form action="freezeAccount" method="post" class="account-form">
            <input type="hidden" name="accountNo" value="${account.accountNo}"/>
            <p>
                <label>Freeze Note（必填）:</label><br/>
                <textarea name="freezeNote" rows="3" cols="40"></textarea>
            </p>
            <input type="submit" value="Freeze Account">
        </form>
    </c:if>

    <%-- 批次模式（從 accountList 勾選多筆進來） --%>
    <c:if test="${account == null && freezeTargets != null}">
        <p>Selected accounts to freeze:</p>
        <table class="account-table">
            <tr><th>Account No</th><th>Owner</th><th>Status</th></tr>
            <c:forEach items="${freezeTargets}" var="acct">
                <tr>
                    <td>${acct.accountNo}</td>
                    <td>${acct.ownerName}</td>
                    <td>${acct.status}</td>
                </tr>
            </c:forEach>
        </table>
        <form action="freezeSelected" method="post" class="account-form">
            <c:forEach items="${freezeTargets}" var="acct">
                <input type="hidden" name="accountNos" value="${acct.accountNo}"/>
            </c:forEach>
            <p>
                <label>Freeze Note（統一填寫，必填）:</label><br/>
                <textarea name="freezeNote" rows="3" cols="40"></textarea>
            </p>
            <input type="submit" value="Freeze All Selected">
        </form>
    </c:if>

    <br>
    <a href="accountList">Back to Account List</a>
    </div><!-- .main-content -->
</body>
</html>

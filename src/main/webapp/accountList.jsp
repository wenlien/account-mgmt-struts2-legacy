<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ page import="com.example.accountmgmt.security.SecurityUtil" %>
<% boolean isAdmin = SecurityUtil.isAdmin(); %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Anycompany Business Capital (ABC)</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <jsp:include page="header.jsp"/>
    <div class="main-content">

    <h1><%= isAdmin ? "All Accounts (Admin)" : "My Accounts" %></h1>

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

    <form action="batchCloseForm" method="post">
    <table class="account-table">
        <tr>
            <% if (isAdmin) { %><th><input type="checkbox" onclick="toggleAll(this)"/></th><% } %>
            <th>Account No</th>
            <th>Owner</th>
            <th>Balance</th>
            <th>Status</th>
            <th>Opened</th>
            <th>Closed</th>
            <th>Close Note</th>
            <th>Transactions</th>
            <% if (isAdmin) { %><th>Actions</th><% } %>
        </tr>
        <c:forEach items="${accountList}" var="acct">
            <tr>
                <% if (isAdmin) { %>
                <td>
                    <c:if test="${acct.status != 'CLOSED'}">
                        <input type="checkbox" name="accountNos" value="${acct.accountNo}"/>
                    </c:if>
                </td>
                <% } %>
                <%-- 點擊帳號 → 進入該帳戶交易畫面 --%>
                <td><a href="transactions?accountNo=${acct.accountNo}">${acct.accountNo}</a></td>
                <%-- 點擊 owner name → 進入帳號編輯畫面（admin 中央管理；一般 user 無編輯權，顯示純文字） --%>
                <td>
                    <c:choose>
                        <c:when test="<%= isAdmin %>">
                            <c:if test="${not empty acct.ownerUsername}"><a href="editOwnerForm?targetUsername=${acct.ownerUsername}">${acct.ownerDisplayName}</a></c:if>
                            <c:if test="${empty acct.ownerUsername}">${acct.ownerDisplayName}</c:if>
                        </c:when>
                        <c:otherwise>${acct.ownerDisplayName}</c:otherwise>
                    </c:choose>
                </td>
                <td>${acct.balance}</td>
                <td>${acct.status}</td>
                <td>${acct.openedDate}</td>
                <td>${acct.closedDate}</td>
                <td>${acct.closeNote}</td>
                <td><a href="transactions?accountNo=${acct.accountNo}">View</a></td>
                <%-- item2：一般 user 無 Actions 欄（改密碼在 header）；owner 名稱/密碼由 admin 中央管理（item3/4） --%>
                <% if (isAdmin) { %>
                <%-- item7：Actions 改下拉選單，選取後導向對應操作 --%>
                <td>
                    <select class="action-select" onchange="if(this.value){window.location.href=this.value;}">
                        <option value="">Actions…</option>
                        <c:if test="${not empty acct.ownerUsername}">
                            <option value="editOwnerForm?targetUsername=${acct.ownerUsername}">Edit Owner (name/phone/address/pwd)</option>
                        </c:if>
                        <c:if test="${acct.status == 'ACTIVATED'}">
                            <option value="freezeAccountForm?accountNo=${acct.accountNo}">Freeze</option>
                            <option value="closeAccountForm?accountNo=${acct.accountNo}">Close</option>
                        </c:if>
                        <c:if test="${acct.status == 'FROZEN'}">
                            <option value="activateAccountForm?accountNo=${acct.accountNo}">Activate</option>
                            <option value="closeAccountForm?accountNo=${acct.accountNo}">Close</option>
                        </c:if>
                    </select>
                </td>
                <% } %>
            </tr>
        </c:forEach>
    </table>
    <% if (isAdmin) { %>
    <br>
    <%-- item7：批次 freeze / activate / close（同一組勾選，用 formaction 導向不同 action） --%>
    <%-- item2：批次凍結先進表單頁填 freeze note --%>
    <input type="submit" formaction="batchFreezeForm" value="Freeze Selected"/>
    <input type="submit" formaction="batchActivateForm" value="Activate Selected"/>
    <input type="submit" value="Close Selected Accounts"/>
    <% } %>
    </form>

    <% if (isAdmin) { %>
    <br>
    <a href="addAccount"><button type="button">Open New Account</button></a>
    <% } %>

    <script>
    function toggleAll(source) {
        var checkboxes = document.getElementsByName('accountNos');
        for (var i = 0; i < checkboxes.length; i++) {
            checkboxes[i].checked = source.checked;
        }
    }
    </script>
    </div><!-- .main-content -->
</body>
</html>

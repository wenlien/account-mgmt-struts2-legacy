<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Edit Account</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <jsp:include page="header.jsp"/>
    <div class="main-content">
    <h1>Edit Account</h1>

    <s:if test="hasActionErrors()">
        <div style="color:red;">
            <s:actionerror/>
        </div>
    </s:if>

    <form action="updateAccount" method="post" class="account-form">
        <s:hidden name="account.accountNo" value="%{account.accountNo}"/>
        <p>Account No: <s:property value="account.accountNo"/></p>
        <p>Opened: <s:property value="account.openedDate"/>（開戶日不可變更）</p>
        <p>Status: <s:property value="account.status"/>（狀態請用清單頁的 Freeze / Activate / Close）</p>
        <s:textfield name="account.ownerName" value="%{account.ownerName}" label="%{getText('label.ownerName')}" size="40"/>
        <input type="submit" value="Save Changes">
    </form>
    <br>
    <a href="accountList">Back to Account List</a>
    </div><!-- .main-content -->
</body>
</html>

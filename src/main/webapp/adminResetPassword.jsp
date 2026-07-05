<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Reset User Password</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <jsp:include page="header.jsp"/>
    <div class="main-content" style="max-width:480px;">
    <h1>Reset User Password</h1>
    <p>重設使用者 <strong><s:property value="targetUsername"/></strong> 的密碼（admin 操作，無需舊密碼）。</p>

    <s:if test="hasActionErrors()">
        <div style="color:red;"><s:actionerror/></div>
    </s:if>

    <form action="resetPassword" method="post" class="account-form" style="display:block;">
        <input type="hidden" name="targetUsername" value="<s:property value='targetUsername'/>"/>
        <p>
            <label for="newPassword">New Password</label><br/>
            <input type="password" id="newPassword" name="newPassword" size="30"/>
            <span style="color:#888;">（至少 8 字元）</span>
        </p>
        <p>
            <label for="confirmNewPassword">Confirm New Password</label><br/>
            <input type="password" id="confirmNewPassword" name="confirmNewPassword" size="30"/>
        </p>
        <input type="submit" value="Reset Password" class="header-logout-btn" style="margin-top:10px;"/>
    </form>
    <br>
    <a href="accountList">Back to Account List</a>
    </div><!-- .main-content -->
</body>
</html>

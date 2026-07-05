<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Change Password</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <jsp:include page="header.jsp"/>
    <div class="main-content" style="max-width:400px;">
    <h1>Change Password</h1>

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

    <form action="changePassword" method="post" class="account-form" style="display:block;">
        <p>
            <label for="oldPassword">Current Password</label><br/>
            <input type="password" id="oldPassword" name="oldPassword" size="30"/>
        </p>
        <p>
            <label for="newPassword">New Password</label><br/>
            <input type="password" id="newPassword" name="newPassword" size="30"/>
            <span style="color:#888;">（至少 8 字元）</span>
        </p>
        <p>
            <label for="confirmNewPassword">Confirm New Password</label><br/>
            <input type="password" id="confirmNewPassword" name="confirmNewPassword" size="30"/>
        </p>
        <input type="submit" value="Change Password" class="header-logout-btn" style="margin-top:10px;"/>
    </form>
    <br>
    <a href="accountList">Back to Account List</a>
    </div><!-- .main-content -->
</body>
</html>

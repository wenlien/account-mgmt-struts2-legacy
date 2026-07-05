<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Open Account</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <jsp:include page="header.jsp"/>
    <div class="main-content">
    <h1>Open New Account</h1>

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

    <form action="openAccount" method="post" class="account-form">
        <p>
            <label>Owner Name:</label>
            <input type="text" name="ownerName" size="40"
                   value="<s:property value='ownerName'/>"/>
        </p>
        <fieldset style="margin:12px 0; padding:10px; border:1px solid #ccc;">
            <legend>Login Account（同時建立系統登入帳號）</legend>
            <p>
                <label>Username:</label>
                <input type="text" name="username" size="30"
                       value="<s:property value='username'/>"/>
            </p>
            <p>
                <label>Password:</label>
                <input type="password" name="password" size="30"/>
                <span style="color:#888;">（至少 8 字元）</span>
            </p>
            <p>
                <label>Confirm Password:</label>
                <input type="password" name="confirmPassword" size="30"/>
                <span style="color:#888;">（再輸入一次）</span>
            </p>
        </fieldset>
        <p>
            <label>Phone <span style="color:#d13212;">*</span>:</label>
            <input type="text" name="phone" size="20" placeholder="0912-345-678"
                   value="<s:property value='phone'/>"/>
            <span style="color:#888;">（必填，作為身分識別）</span>
        </p>
        <p>
            <label>Address <span style="color:#d13212;">*</span>:</label>
            <input type="text" name="address" size="50" placeholder="Address"
                   value="<s:property value='address'/>"/>
            <span style="color:#888;">（必填）</span>
        </p>
        <p>
            <label>Account Category（可多選）:</label><br/>
            <input type="checkbox" name="categories" value="TWD"/> A — 台幣帳戶
            &nbsp; Initial Deposit: <input type="text" name="twdDeposit" size="10" placeholder="0.00"/><br/>
            <input type="checkbox" name="categories" value="FOREIGN"/> B — 外幣帳戶
            &nbsp; Initial Deposit: <input type="text" name="foreignDeposit" size="10" placeholder="0.00"/>
        </p>
        <p>（帳號由系統自動產生，開戶後一律為 ACTIVATED）</p>
        <input type="submit" value="Open Account">
    </form>
    <br>
    <a href="accountList">Back to Account List</a>
    </div><!-- .main-content -->
</body>
</html>

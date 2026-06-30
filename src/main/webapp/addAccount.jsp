<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Add Account</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <h1>Add Account</h1>
    <form action="saveAccount" method="post" class="account-form">
        <s:textfield name="account.accountNo" label="%{getText('label.accountNo')}" size="20"/>
        <s:textfield name="account.ownerName" label="%{getText('label.ownerName')}" size="40"/>
        <s:textfield name="account.balance" label="%{getText('label.balance')}" value="0.00"/>
        <s:select name="account.status" label="%{getText('label.status')}"
                  list="{'ACTIVE','FROZEN','CLOSED'}"/>
        <input type="submit" value="Add Account">
    </form>
    <br>
    <a href="accountList">Back to Account List</a>
</body>
</html>

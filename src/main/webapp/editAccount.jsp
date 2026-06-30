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
    <h1>Edit Account</h1>
    <form action="updateAccount" method="post" class="account-form">
        <s:hidden name="account.accountNo" value="%{account.accountNo}"/>
        <p>Account No: <s:property value="account.accountNo"/></p>
        <s:textfield name="account.ownerName" value="%{account.ownerName}" label="%{getText('label.ownerName')}" size="40"/>
        <s:textfield name="account.balance" value="%{account.balance}" label="%{getText('label.balance')}"/>
        <s:select name="account.status" label="%{getText('label.status')}"
                  list="{'ACTIVE','FROZEN','CLOSED'}" value="%{account.status}"/>
        <input type="submit" value="Save Changes">
    </form>
    <br>
    <a href="accountList">Back to Account List</a>
</body>
</html>

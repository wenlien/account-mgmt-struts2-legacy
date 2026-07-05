<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="com.example.accountmgmt.security.SecurityUtil" %>
<% boolean __isAdmin = SecurityUtil.isAdmin(); %>
<div class="app-header">
    <div class="header-left">
        <img src="${pageContext.request.contextPath}/images/abc-logo.svg"
             alt="ABC" class="header-logo" style="width:7.5%; height:auto;"/>
        <span class="header-title">Anycompany Business Capital (ABC)</span>
    </div>
    <div class="header-right">
        <c:if test="${pageContext.request.remoteUser != null}">
            <span class="header-user">
                Logged in as: <strong>${pageContext.request.remoteUser}</strong>
                (<%= __isAdmin ? "ADMIN" : "USER" %>)
            </span>
            <a href="${pageContext.request.contextPath}/accountList" class="header-link">Accounts</a>
            <% if (__isAdmin) { %>
                <a href="${pageContext.request.contextPath}/auditLog" class="header-link">Audit Log</a>
            <% } else { %>
                <a href="${pageContext.request.contextPath}/changePasswordForm" class="header-link">Change Password</a>
            <% } %>
            <form action="${pageContext.request.contextPath}/logout" method="post" class="header-logout-form">
                <input type="submit" value="Logout" class="header-logout-btn"/>
            </form>
        </c:if>
    </div>
</div>

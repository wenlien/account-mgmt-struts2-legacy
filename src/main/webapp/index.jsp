<%@ page language="java" %><%
    // 首頁 redirect 到帳戶列表（Struts2 action）
    response.sendRedirect(request.getContextPath() + "/accountList.action");
%>

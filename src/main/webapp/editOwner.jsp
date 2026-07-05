<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Edit Owner Name</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <jsp:include page="header.jsp"/>
    <div class="main-content" style="max-width:480px;">
    <h1>Edit Owner Name</h1>
    <p>使用者 <strong><s:property value="targetUsername"/></strong> 的顯示名稱（owner name）。
       此為中央管理：更新後，該使用者名下所有帳戶的 owner 都會一併變更。</p>

    <s:if test="hasActionErrors()">
        <div style="color:red;"><s:actionerror/></div>
    </s:if>

    <form action="updateOwner" method="post" class="account-form" style="display:block;">
        <input type="hidden" name="targetUsername" value="<s:property value='targetUsername'/>"/>
        <p>
            <label for="displayName">Owner Name</label><br/>
            <input type="text" id="displayName" name="displayName"
                   value="<s:property value='displayName'/>" size="40"/>
        </p>
        <p>
            <label for="phone">Phone <span style="color:#d13212;">*</span></label><br/>
            <input type="text" id="phone" name="phone"
                   value="<s:property value='phone'/>" size="20"/>
            <span style="color:#888;">（套用該使用者名下所有帳戶）</span>
        </p>
        <p>
            <label for="address">Address <span style="color:#d13212;">*</span></label><br/>
            <input type="text" id="address" name="address"
                   value="<s:property value='address'/>" size="50"/>
        </p>
        <fieldset style="margin:12px 0; padding:10px; border:1px solid #ccc;">
            <legend>Reset Password（選填，留空則不變更）</legend>
            <p>
                <label for="newPassword">New Password</label><br/>
                <input type="password" id="newPassword" name="newPassword" size="30"/>
                <span style="color:#888;">（至少 8 字元）</span>
            </p>
            <p>
                <label for="confirmNewPassword">Confirm New Password</label><br/>
                <input type="password" id="confirmNewPassword" name="confirmNewPassword" size="30"/>
            </p>
        </fieldset>
        <input type="submit" value="Save" class="header-logout-btn" style="margin-top:10px;"/>
    </form>

    <%-- 就地呈現該使用者的 profile 變更歷史（owner name / 密碼 / 電話 / 地址）。
         完整全站稽核見 Audit Log 頁（auditLog）。 --%>
    <hr style="margin:24px 0;"/>
    <h2 style="font-size:1.1em;">Profile Change History</h2>
    <p style="color:#888; font-size:0.9em;">此使用者的 owner name / 密碼 / 電話 / 地址 變更紀錄（新到舊）。完整全站稽核請見
        <a href="auditLog">Audit Log</a>。</p>
    <c:choose>
        <c:when test="${empty profileChanges}">
            <p style="color:#888;">（尚無變更紀錄）</p>
        </c:when>
        <c:otherwise>
            <table class="account-table">
                <tr>
                    <th>Time</th>
                    <th>By (actor)</th>
                    <th>Action</th>
                    <th>Result</th>
                    <th>Detail</th>
                </tr>
                <c:forEach items="${profileChanges}" var="log">
                    <tr>
                        <td>${log.createdAt}</td>
                        <td>${log.actor}</td>
                        <td>${log.action}</td>
                        <td>
                            <c:choose>
                                <c:when test="${log.success}"><span style="color:green;">OK</span></c:when>
                                <c:otherwise><span style="color:#d13212;">FAIL ${log.errorCode}</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td><c:out value="${log.detail}"/></td>
                    </tr>
                </c:forEach>
            </table>
        </c:otherwise>
    </c:choose>

    <br>
    <a href="accountList">Back to Account List</a>
    </div><!-- .main-content -->
</body>
</html>

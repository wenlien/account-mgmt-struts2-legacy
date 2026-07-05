<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Audit Log</title>
    <link rel="stylesheet" type="text/css" href="css/style.css">
</head>
<body>
    <jsp:include page="header.jsp"/>
    <div class="main-content">
    <h1>Audit Log</h1>

    <%-- 過濾器（#9）：任一欄位留空表示不套用該條件 --%>
    <form action="auditLog" method="get" id="auditForm" class="account-form" style="margin-bottom:16px;">
        <label>Actor:</label>
        <input type="text" name="actor" value="${actor}" size="12"/>
        <label>Action:</label>
        <select name="action">
            <option value="">(any)</option>
            <c:forEach var="a" items="OPEN_ACCOUNT,DEPOSIT,WITHDRAW,TRANSFER,FREEZE,ACTIVATE,CLOSE,UPDATE_OWNER,CHANGE_PASSWORD">
                <option value="${a}" ${action == a ? 'selected' : ''}>${a}</option>
            </c:forEach>
        </select>
        <label>Account:</label>
        <input type="text" name="targetAccountNo" value="${targetAccountNo}" size="8"/>
        <label>Result:</label>
        <select name="success">
            <option value="" ${success == null || success == '' ? 'selected' : ''}>(any)</option>
            <option value="true" ${success == 'true' ? 'selected' : ''}>Success</option>
            <option value="false" ${success == 'false' ? 'selected' : ''}>Failure</option>
        </select>
        <label>Error Code:</label>
        <input type="text" name="errorCode" value="${errorCode}" size="6" placeholder="E2001"/>
        <label>From:</label>
        <input type="date" name="fromDate" value="${fromDate}"/>
        <label>To:</label>
        <input type="date" name="toDate" value="${toDate}"/>
        <br/>
        <label>Detail contains:</label>
        <input type="text" name="detailQuery" value="${detailQuery}" size="24" placeholder="text 或 regex"/>
        <select name="detailMode">
            <option value="text" ${detailMode != 'regex' ? 'selected' : ''}>plain text</option>
            <option value="regex" ${detailMode == 'regex' ? 'selected' : ''}>regex</option>
        </select>
        <br/>
        <%-- 排序：下拉選單（欄位 + 方向）；也可直接點欄位標題（見下方表頭） --%>
        <label>Sort by:</label>
        <select name="sortBy" id="sortBy">
            <option value="" ${empty sortBy ? 'selected' : ''}>(default: time desc)</option>
            <option value="id" ${sortBy == 'id' ? 'selected' : ''}>ID</option>
            <option value="createdAt" ${sortBy == 'createdAt' ? 'selected' : ''}>Time</option>
            <option value="actor" ${sortBy == 'actor' ? 'selected' : ''}>Actor</option>
            <option value="action" ${sortBy == 'action' ? 'selected' : ''}>Action</option>
            <option value="targetAccountNo" ${sortBy == 'targetAccountNo' ? 'selected' : ''}>Account</option>
            <option value="success" ${sortBy == 'success' ? 'selected' : ''}>Result</option>
            <option value="errorCode" ${sortBy == 'errorCode' ? 'selected' : ''}>Error Code</option>
        </select>
        <select name="sortDir" id="sortDir">
            <option value="asc" ${sortDir != 'desc' ? 'selected' : ''}> asc</option>
            <option value="desc" ${sortDir == 'desc' ? 'selected' : ''}>desc</option>
        </select>
        <input type="submit" value="Filter"/>
        <a href="auditLog" style="margin-left:8px;">Reset</a>
    </form>

    <script>
    // 點欄位標題排序：第一次該欄位 asc，再次點同欄位切 desc（沿用目前所有過濾條件一起送出）。
    function setSort(col) {
        var sb = document.getElementById('sortBy');
        var sd = document.getElementById('sortDir');
        if (sb.value === col) {
            sd.value = (sd.value === 'asc') ? 'desc' : 'asc';
        } else {
            sb.value = col;
            sd.value = 'asc';
        }
        document.getElementById('auditForm').submit();
    }
    </script>

    <s:if test="hasActionErrors()">
        <div style="color:red;"><s:actionerror/></div>
    </s:if>

    <table class="account-table">
        <tr>
            <th><a href="#" onclick="setSort('id');return false;">ID</a>${sortBy == 'id' ? (sortDir == 'desc' ? ' ▼' : ' ▲') : ''}</th>
            <th><a href="#" onclick="setSort('createdAt');return false;">Time</a>${sortBy == 'createdAt' ? (sortDir == 'desc' ? ' ▼' : ' ▲') : ''}</th>
            <th><a href="#" onclick="setSort('actor');return false;">Actor</a>${sortBy == 'actor' ? (sortDir == 'desc' ? ' ▼' : ' ▲') : ''}</th>
            <th><a href="#" onclick="setSort('action');return false;">Action</a>${sortBy == 'action' ? (sortDir == 'desc' ? ' ▼' : ' ▲') : ''}</th>
            <th><a href="#" onclick="setSort('targetAccountNo');return false;">Account</a>${sortBy == 'targetAccountNo' ? (sortDir == 'desc' ? ' ▼' : ' ▲') : ''}</th>
            <th><a href="#" onclick="setSort('success');return false;">Result</a>${sortBy == 'success' ? (sortDir == 'desc' ? ' ▼' : ' ▲') : ''}</th>
            <th><a href="#" onclick="setSort('errorCode');return false;">Error Code</a>${sortBy == 'errorCode' ? (sortDir == 'desc' ? ' ▼' : ' ▲') : ''}</th>
            <th>Detail</th>
        </tr>
        <c:forEach items="${auditLogs}" var="log">
            <tr>
                <td>${log.id}</td>
                <td>${log.createdAt}</td>
                <td>${log.actor}</td>
                <td>${log.action}</td>
                <td>${log.targetAccountNo}</td>
                <td style="color:${log.success ? '#155724' : '#721c24'};">
                    ${log.success ? 'OK' : 'FAIL'}
                </td>
                <td>${log.errorCode}</td>
                <td><c:out value="${log.detail}"/></td>
            </tr>
        </c:forEach>
    </table>
    <c:if test="${empty auditLogs}">
        <p>No audit records match the filter.</p>
    </c:if>
    </div><!-- .main-content -->
</body>
</html>

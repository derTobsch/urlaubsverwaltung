<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@taglib prefix="uv" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html>

<head>
    <script type="text/javascript">
        window.uv = window.uv ? window.uv : {}
        window.uv.departmentsJson = JSON.parse('<c:out value="${departmentsJson}" />'.replace(/&#034;/g, '"'));
    </script>
    <script defer src="<spring:url value='/assets/npm.sigma.min.js' />"></script>
    <script defer src="<spring:url value='/assets/department_graph.min.js' />"></script>
    <uv:head/>
    <spring:url var="URL_PREFIX" value="/web"/>
</head>

<body>

<uv:menu/>

<div class="print-info--only-landscape">
    <h4><spring:message code="print.info.landscape"/></h4>
</div>

<div class="content print--only-landscape">
    <div class="container">
        <div class="row">
            <div class="col-xs-12">
                <spring:message code="action.department.graph"/>
                <div id="graph-container" style="width: 1000px; height: 750px"></div>
            </div>
        </div>
    </div>
</div>

</body>

</html>

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
    <script defer src="<spring:url value='/assets/npm.echarts.min.js' />"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/echarts/4.2.1/echarts.min.js"/>
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

<script type="text/javascript">
    window.uv = window.uv ? window.uv : {}
    window.uv.departmentsJson = JSON.parse('<c:out value="${departmentsJson}" />'.replace(/&#034;/g, '"'));

    let container = document.getElementById('graph-container');

    var echart = echarts.init(container);

    const createNode = (id, label, color) => {
        return {
            id: id,
            name: label,
            itemStyle: {
                normal: {
                    color: color
                }
            },
            attributes: {},
            draggable: true
        }
    };

    const createLink = (id, label, sourceId, targetId) => {
        return {
            id: id,
            name: label,
            source: sourceId,
            target: targetId,
            lineStyle: {
                normal: {}
            }
        }
    };

    var graph = {
        nodes: [],
        links: []
    };

    window.uv.departmentsJson.forEach((dep, i) => {
        graph.nodes.push(createNode('dep_' + i, dep.name, '#f00'));

        dep.departmentHeads.forEach((head, j) => {
            graph.nodes.push(createNode('dep_head_' + i + '_' + j, head.niceName, '#00f'));
            graph.links.push(createLink('edge_dep_head_' + i + '_' + j, 'Department Head', 'dep_' + i, 'dep_head_' + i + '_' + j));
        });

        dep.members.forEach((member, j) => {
            graph.nodes.push(createNode('dep_member_' + i + '_' + j, member.niceName, '#0f0'));
            graph.links.push(createLink('edge_dep_member_' + i + '_' + j, 'Department Member', 'dep_' + i, 'dep_member_' + i + '_' + j));
        });
    });

    // graph.nodes.forEach(function (node) {
    //     node.itemStyle = null;
    //     node.symbolSize = 10;
    //     node.value = node.symbolSize;
    //     node.category = node.attributes.modularity_class;
    //     // Use random x, y
    //     node.x = node.y = null;
    //     node.draggable = true;
    // });

    var option = {
        title: {
            text: 'Les Miserables',
            subtext: 'Default layout',
            top: 'bottom',
            left: 'right'
        },
        tooltip: {},
        legend: [{
            // selectedMode: 'single',
            // data: categories.map(function (a) {
            //     return a.name;
            // })
        }],
        animation: false,
        series : [
            {
                name: 'Department Members',
                type: 'graph',
                layout: 'force',
                data: graph.nodes,
                links: graph.links,
                // categories: categories,
                roam: true,
                label: {
                    normal: {
                        position: 'right'
                    }
                },
                force: {
                    repulsion: 100
                }
            }
        ]
    };

    echart.setOption(option);
</script>

</body>

</html>

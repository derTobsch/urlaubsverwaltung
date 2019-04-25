import sigma from "sigma"

var s = new sigma('graph-container');

window.uv.departmentsJson.forEach((dep, i) => {
  s.graph.addNode({
    // Main attributes:
    id: 'dep_' + i,
    label: dep.name,
    // Display attributes:
    x: Math.random(),
    y: Math.random(),
    size: 1,
    color: '#f00'
  });

  dep.departmentHeads.forEach((head, j) => {
    s.graph.addNode({
      // Main attributes:
      id: 'dep_head_' + i + '_' + j,
      label: head.niceName,
      // Display attributes:
      x: Math.random(),
      y: Math.random(),
      size: 1,
      color: '#00f'
    }).addEdge({
      id: 'edge_dep_head_' + i + '_' + j,
      label: "Department Head",
      // Reference extremities:
      source: 'dep_' + i,
      target: 'dep_head_' + i + '_' + j
    });
  });

  dep.members.forEach((member, j) => {
    s.graph.addNode({
      // Main attributes:
      id: 'dep_member_' + i + '_' + j,
      label: member.niceName,
      // Display attributes:
      x: Math.random(),
      y: Math.random(),
      size: 1,
      color: '#0f0'
    }).addEdge({
      id: 'edge_dep_member_' + i + '_' + j,
      label: "Department Member",
      // Reference extremities:
      source: 'dep_' + i,
      target: 'dep_member_' + i + '_' + j
    });
  });
});

s.refresh();

import React, { useEffect, useCallback } from 'react';
import ReactFlow, {
  MiniMap,
  Background,
  useNodesState,
  useEdgesState
} from 'reactflow';
import 'reactflow/dist/style.css';

function createFlowElements(data) {
    let nodes = [];
    let edges = [];
    let levelWidths = {};
    let maxDefinedLevel = 0; // Track the maximum defined level
  
    // First, determine the maximum level that has been explicitly defined
    data.states.forEach(state => {
      if (state.menu && state.menu.level !== undefined) {
        maxDefinedLevel = Math.max(maxDefinedLevel, state.menu.level);
      }
    });
  
    // Calculate positions based on levels
    data.states.forEach(state => {
      // Assign a default level if menu is null or level is not defined
      const level = state.menu && state.menu.level !== undefined ? state.menu.level : maxDefinedLevel + 1;
      if (!levelWidths[level]) levelWidths[level] = 0;
  
      const nodePositionX = levelWidths[level] * 300; // Increased spacing between nodes horizontally
      const nodePositionY = level * 200; // Increased vertical gap to prevent overlap
      
      if (state.menu){
        nodes.push({
          id: state.id,
          type: 'default',
          position: { x: nodePositionX, y: nodePositionY },
          data: { label: (
            <>
              <strong>{state.menu.description}</strong>
              <ul>
                {state.menu && state.menu.options ? state.menu.options.map(opt => (
                  <div key={opt.key}>{opt.key === 0 ? 'empty': opt.key}: {opt.value}</div>
                  
                )) : <span>No Options</span>}
              </ul>
            </>
          )},
          style: { width: 250 } // Set a specific width for each node box
      })};



      levelWidths[level] += 1; // Increment to space out nodes at the same level
    });
  
    // Add edges based on transitions
    data.states.forEach(state => {
      state.transition_map.forEach(transition => {
        if (transition.dest_state_id === transition.source_state_id || transition.input === "9" ) return;
        edges.push({
          id: `e${transition.source_state_id}-${transition.dest_state_id}`,
          source: transition.source_state_id,
          target: transition.dest_state_id,
          label: transition.input,
          style: { stroke: '#000' }
        });
      });
    });
  
    return { nodes, edges };
  }

export default function ViewIVR() {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  const fetchFSMData = useCallback(async () => {
    try {
      const response = await fetch('http://127.0.0.1:8000/getFSM');
      const data = await response.json();
      console.log(data);
      const { nodes, edges } = createFlowElements(data);
      setNodes(nodes);
      setEdges(edges);
    } catch (error) {
      console.error('Failed to fetch FSM data:', error);
    }
  }, [setNodes, setEdges]);

  useEffect(() => {
    fetchFSMData();
  }, [fetchFSMData]);

  return (
    <div style={{ width: '100vw', height: '100vh' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        fitView
      >
        <MiniMap />
        <Background variant="dots" gap={12} size={1} />
      </ReactFlow>
    </div>
  );
}

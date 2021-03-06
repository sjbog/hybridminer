package ee.ut;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.NodeID;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramImpl;
import org.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.*;
import org.processmining.models.graphbased.directed.bpmn.elements.Event;
import org.processmining.plugins.converters.BPMNUtils;
import org.processmining.plugins.converters.ConverterException;
import org.processmining.processtree.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProcessTree2BPMNConverter {

	private static final String PROCESS_TREE_INTERNAL_NODE = "Process tree internal node";

	private String currentLabel ="";

	private Map<UUID, Swimlane> orgIdMap = new HashMap<UUID, Swimlane>();

	private Map<BPMNNode, Node > conversionMap = new HashMap<BPMNNode, Node>();

	public Object[] convertToBPMN(ProcessTree tree, boolean simplify) {

		BPMNDiagram bpmnDiagram = new BPMNDiagramImpl("BPMN diagram for "
				+ tree.getName());

		// Convert originators
		convertOriginators(tree, bpmnDiagram);

		// Convert Process tree to a BPMN diagram
		convert(tree, bpmnDiagram);

		//Simplify BPMN diagram
		if(simplify) {
			BPMNUtils.simplifyBPMNDiagram( null, bpmnDiagram );
		}

		Map<NodeID, UUID> idMap = retrieveIdMap();
		return new Object[] {bpmnDiagram, idMap};
	}

	/**
	 * Constructs IdMap from ConversionMap
	 *
	 * @return
	 */
	private Map<NodeID, UUID> retrieveIdMap() {
		Map<NodeID, UUID> idMap = new HashMap<NodeID, UUID>();
		for(BPMNNode bpmnNode : conversionMap.keySet()) {
			idMap.put(bpmnNode.getId(), conversionMap.get(bpmnNode).getID());
		}
		for(UUID org: orgIdMap.keySet()){
			idMap.put(orgIdMap.get(org).getId(), org);
		}
		return idMap;
	}

	/**
	 * Convert originators
	 *
	 * @param tree
	 * @param bpmnDiagram
	 */
	private void convertOriginators(ProcessTree tree, BPMNDiagram bpmnDiagram) {
		// Add lanes
		for(Originator originator : tree.getOriginators()) {
			Swimlane lane = bpmnDiagram.addSwimlane(originator.getName(), null, SwimlaneType.LANE);
			lane.setPartitionElement(originator.getID().toString());
			orgIdMap.put(originator.getID(), lane);
		}
	}

	/**
	 * Converts process tree to BPMN diagram
	 *
	 * @param tree
	 * @param bpmnDiagram
	 * @return
	 */
	private void convert(ProcessTree tree, BPMNDiagram bpmnDiagram) {

		// Create initial elements
		org.processmining.models.graphbased.directed.bpmn.elements.Event startEvent
				= bpmnDiagram.addEvent("Start", Event.EventType.START, null, null, true, null);
		org.processmining.models.graphbased.directed.bpmn.elements.Event endEvent
				= bpmnDiagram.addEvent("End", Event.EventType.END, null, null, true, null);
		Activity rootActivity =
				bpmnDiagram.addActivity(PROCESS_TREE_INTERNAL_NODE, false, false, false, false, false);
		bpmnDiagram.addFlow(startEvent, rootActivity, "");
		bpmnDiagram.addFlow(rootActivity, endEvent, "");

		conversionMap.put(rootActivity, tree.getRoot());
		expandNodes(tree, bpmnDiagram);
	}

	/**
	 * Expand all activities which correspond to the internal process tree nodes
	 *
	 * @param tree
	 * @param bpmnDiagram
	 */
	private void expandNodes( ProcessTree tree, BPMNDiagram bpmnDiagram) {
		Activity activity = takeFirstInternalActivity();
		if (activity != null) {
			if (PROCESS_TREE_INTERNAL_NODE.equals(activity.getLabel())) {
				Node treeNode = conversionMap.get(activity);
				if (treeNode instanceof Task ) {
					expandTask(activity, (Task) treeNode, tree, bpmnDiagram);
				} else if (treeNode instanceof org.processmining.processtree.Event ) {
					expandEvent(activity, ( org.processmining.processtree.Event ) treeNode, tree, bpmnDiagram);
				} else if (treeNode instanceof Block ) {
					expandBlock(activity, (Block) treeNode, tree, bpmnDiagram);
				}
			}
		}
	}

	/**
	 * Take first internal activity from conversion map
	 *
	 * @return
	 */
	private Activity takeFirstInternalActivity() {
		Activity activity = null;
		if (!conversionMap.isEmpty()) {
			for (BPMNNode bpmnNode : conversionMap.keySet()) {
				if(bpmnNode instanceof Activity) {
					if(PROCESS_TREE_INTERNAL_NODE.equals(bpmnNode.getLabel())) {
						activity = (Activity)bpmnNode;
					}
				}
			}
		}
		return activity;
	}

	/**
	 * Expand activity which corresponds to the tree internal node
	 *
	 * @param activity
	 * @param blockNode
	 * @param tree
	 * @param bpmnDiagram
	 */
	@SuppressWarnings("incomplete-switch")
	private void expandBlock(Activity activity, Block blockNode, ProcessTree tree, BPMNDiagram bpmnDiagram) {
		switch(tree.getType(blockNode)) {
			case XOR: {
				expandGate(activity, blockNode, tree, bpmnDiagram, Gateway.GatewayType.DATABASED);
				break;
			}
			case OR: {
				expandGate(activity, blockNode, tree, bpmnDiagram, Gateway.GatewayType.INCLUSIVE);
				break;
			}
			case AND : {
				expandGate(activity, blockNode, tree, bpmnDiagram, Gateway.GatewayType.PARALLEL);
				break;
			}
			case SEQ : {
				expandSequence(activity, blockNode, tree, bpmnDiagram);
				break;
			}
			case DEF : {
				expandGate(activity, blockNode, tree, bpmnDiagram, Gateway.GatewayType.EVENTBASED);
				break;
			}
			case LOOPXOR : {
				expandLoop(activity, blockNode, tree, bpmnDiagram, false);
				break;
			}
			case LOOPDEF : {
				expandLoop(activity, blockNode, tree, bpmnDiagram, true);
				break;
			}
			case PLACEHOLDER : {
				expandPlaceholder(activity, blockNode, tree, bpmnDiagram);
			}
		}
		conversionMap.remove(activity);
		expandNodes(tree, bpmnDiagram);
	}

	/**
	 * Expand activity which corresponds to the task
	 *
	 * @param activity
	 * @param taskNode
	 * @param tree
	 * @param bpmnDiagram
	 */
	private void expandTask(Activity activity, Task taskNode, ProcessTree tree, BPMNDiagram bpmnDiagram) {

		// Delete activity and corresponding incoming and outgoing flows
		BPMNNode source = deleteIncomingFlow(activity, bpmnDiagram);
		BPMNNode target = deleteOutgoingFlow(activity, bpmnDiagram);
		bpmnDiagram.removeActivity(activity);


		// Add new task
		String label = BPMNUtils.EMPTY;
		if (taskNode.getName() != null && !taskNode.getName().isEmpty()
				&& !(taskNode instanceof Task.Automatic)) {
			label = taskNode.getName();
		}
		Activity task = bpmnDiagram.addActivity(label, false, false, false, false, false);

		bpmnDiagram.addFlow(source, task, currentLabel);
		bpmnDiagram.addFlow(task, target, "");
		conversionMap.remove(activity);
		conversionMap.put(task, taskNode);
		if (taskNode instanceof Task.Manual ) {
			Task.Manual manualTask = (Task.Manual ) taskNode;
			Collection<Originator> originators = manualTask.getOriginators();
			if (originators.size() == 1) {
				Originator originator = originators.iterator().next();
				Swimlane lane = orgIdMap.get(originator.getID());
				task.setParentSwimlane(lane);
			}
		}

		expandNodes(tree, bpmnDiagram);
	}

	/**
	 * Expand activity which corresponds to the event
	 *
	 * @param activity
	 * @param eventNode
	 * @param tree
	 * @param bpmnDiagram
	 */
	private void expandEvent(Activity activity, org.processmining.processtree.Event eventNode, ProcessTree tree, BPMNDiagram bpmnDiagram) {

		// Delete activity and corresponding incoming and outgoing flows
		BPMNNode source = deleteIncomingFlow(activity, bpmnDiagram);
		BPMNNode target = deleteOutgoingFlow(activity, bpmnDiagram);
		bpmnDiagram.removeActivity(activity);

		org.processmining.models.graphbased.directed.bpmn.elements.Event.EventTrigger eventTrigger = null;
		if(eventNode instanceof org.processmining.processtree.Event.TimeOut ) {
			eventTrigger = org.processmining.models.graphbased.directed.bpmn.elements.Event.EventTrigger.TIMER;
		} else if(eventNode instanceof org.processmining.processtree.Event.Message ) {
			eventTrigger = org.processmining.models.graphbased.directed.bpmn.elements.Event.EventTrigger.SIGNAL;
		}
		// Add new event
		org.processmining.models.graphbased.directed.bpmn.elements.Event event
				= bpmnDiagram.addEvent(eventNode.getMessage(), org.processmining.models.graphbased.directed.bpmn.elements.Event.EventType.INTERMEDIATE, eventTrigger,
				org.processmining.models.graphbased.directed.bpmn.elements.Event.EventUse.CATCH, true, null);

		bpmnDiagram.addFlow(source, event, currentLabel);
		// Add child
		if(eventNode.getChildren().size() != 1) {
			throw new ConverterException("Event node must have one children");
		}
		Node child = eventNode.getChildren().get(0);
		Activity newActivity = bpmnDiagram.addActivity(PROCESS_TREE_INTERNAL_NODE, false, false, false, false, false);
		bpmnDiagram.addFlow(event, newActivity, "");
		bpmnDiagram.addFlow(newActivity, target, "");
		conversionMap.put(newActivity, child);
		conversionMap.put(event, eventNode);
		conversionMap.remove(activity);
		expandNodes(tree, bpmnDiagram);
	}

	/**
	 * Expand gate node
	 *
	 * @param activity
	 * @param blockNode
	 * @param tree
	 * @param bpmnDiagram
	 * @param gatewayType
	 */
	private void expandGate(Activity activity, Block blockNode, ProcessTree tree,
							BPMNDiagram bpmnDiagram, Gateway.GatewayType gatewayType) {

		// Delete activity and corresponding incoming and outgoing flows
		BPMNNode source = deleteIncomingFlow(activity, bpmnDiagram);
		BPMNNode target = deleteOutgoingFlow(activity, bpmnDiagram);
		bpmnDiagram.removeActivity(activity);

		Gateway split = bpmnDiagram.addGateway("", gatewayType);
		if(gatewayType.equals( Gateway.GatewayType.EVENTBASED)) {
			gatewayType = Gateway.GatewayType.DATABASED;
		}
		conversionMap.put(split, blockNode);
		Gateway join = bpmnDiagram.addGateway("", gatewayType);
		bpmnDiagram.addFlow(source, split, currentLabel);
		bpmnDiagram.addFlow(join, target, "");

		// Add new activities
		for(Node child : blockNode.getChildren()) {
			Activity newActivity  = bpmnDiagram.addActivity(PROCESS_TREE_INTERNAL_NODE, false,
					false, false, false, false);
			bpmnDiagram.addFlow(split, newActivity, "");
			bpmnDiagram.addFlow(newActivity, join, "");
			conversionMap.put(newActivity, child);
		}
	}

	/**
	 * Expand sequence node
	 *
	 * @param activity
	 * @param blockNode
	 * @param tree
	 * @param bpmnDiagram
	 */
	private void expandSequence( Activity activity, Block blockNode, ProcessTree tree, BPMNDiagram bpmnDiagram) {

		// Delete activity and corresponding incoming and outgoing flows
		BPMNNode source = deleteIncomingFlow(activity, bpmnDiagram);
		BPMNNode target = deleteOutgoingFlow(activity, bpmnDiagram);
		bpmnDiagram.removeActivity(activity);

		BPMNNode prevNode = source;
		// Add new activities
		for(Node child : blockNode.getChildren()) {
			Activity newActivity  = bpmnDiagram.addActivity(PROCESS_TREE_INTERNAL_NODE, false,
					false, false, false, false);
			bpmnDiagram.addFlow(prevNode, newActivity, currentLabel);
			conversionMap.put(newActivity, child);
			prevNode = newActivity;
		}
		bpmnDiagram.addFlow(prevNode, target, "");
	}

	/**
	 * Expand loop
	 *
	 * @param activity
	 * @param blockNode
	 * @param tree
	 * @param bpmnDiagram
	 * @param gatewayType
	 * @param isDeferred
	 */
	private void expandLoop(Activity activity, Block blockNode, ProcessTree tree,
							BPMNDiagram bpmnDiagram, boolean isDeferred) {

		// Delete activity and corresponding incoming and outgoing flows
		BPMNNode source = deleteIncomingFlow(activity, bpmnDiagram);
		BPMNNode target = deleteOutgoingFlow(activity, bpmnDiagram);
		bpmnDiagram.removeActivity(activity);

		Gateway xorJoin = bpmnDiagram.addGateway("", Gateway.GatewayType.DATABASED);
		Gateway xorSplit = null;
		if (isDeferred) {
			xorSplit = bpmnDiagram.addGateway("", Gateway.GatewayType.EVENTBASED);
		} else {
			xorSplit = bpmnDiagram.addGateway("", Gateway.GatewayType.DATABASED);
		}

		conversionMap.put(xorSplit, blockNode);
		bpmnDiagram.addFlow(source, xorJoin, currentLabel);

		// Add loop body
		if(blockNode.getChildren().size() != 3) {
			throw new ConverterException("Loop node must have three children");
		}
		Node child1 = blockNode.getChildren().get(0);
		Activity newActivity1 = bpmnDiagram.addActivity(PROCESS_TREE_INTERNAL_NODE, false, false,
				false, false, false);
		bpmnDiagram.addFlow(xorJoin, newActivity1, "");
		bpmnDiagram.addFlow(newActivity1, xorSplit, "");
		conversionMap.put(newActivity1, child1);

		Node child2 = blockNode.getChildren().get(1);
		Activity newActivity2 = bpmnDiagram.addActivity(PROCESS_TREE_INTERNAL_NODE, false, false,
				false, false, false);
		bpmnDiagram.addFlow(xorSplit, newActivity2, "");
		bpmnDiagram.addFlow(newActivity2, xorJoin, "");
		conversionMap.put(newActivity2, child2);

		Node child3 = blockNode.getChildren().get(2);
		Activity newActivity3 = bpmnDiagram.addActivity(PROCESS_TREE_INTERNAL_NODE, false, false,
				false, false, false);
		bpmnDiagram.addFlow(xorSplit, newActivity3, "");
		bpmnDiagram.addFlow(newActivity3, target, "");
		conversionMap.put(newActivity3, child3);
	}

	/**
	 * Expand placeholder
	 *
	 * @param activity
	 * @param blockNode
	 * @param tree
	 * @param bpmnDiagram
	 */
	private void expandPlaceholder(Activity activity, Block blockNode, ProcessTree tree,
								   BPMNDiagram bpmnDiagram) {

		// Delete activity and corresponding incoming and outgoing flows
		BPMNNode source = deleteIncomingFlow(activity, bpmnDiagram);
		BPMNNode target = deleteOutgoingFlow(activity, bpmnDiagram);
		bpmnDiagram.removeActivity(activity);

		Gateway split = bpmnDiagram.addGateway("Placeholder", Gateway.GatewayType.DATABASED);
		Gateway join = bpmnDiagram.addGateway("", Gateway.GatewayType.DATABASED);
		bpmnDiagram.addFlow(source, split, currentLabel);
		bpmnDiagram.addFlow(join, target, "");

		// Add new activities
		int childNum = 1;
		for(Node child : blockNode.getChildren()) {
			Activity newActivity  = bpmnDiagram.addActivity(PROCESS_TREE_INTERNAL_NODE, false,
					false, false, false, false);
			String label = "";
			if(childNum == 1) {
				label = "This subprocess could be replaced by one of the alternatives";
			} else {
				label = "Alternative " + childNum;
			}
			bpmnDiagram.addFlow(split, newActivity, label);
			bpmnDiagram.addFlow(newActivity, join, "");
			conversionMap.put(newActivity, child);
			childNum++;
		}
	}

	/**
	 * Delete incoming flow of the activity, if activity has more than one incoming flow,
	 * exception will be generated
	 *
	 * @param activity
	 * @param bpmnDiagram
	 * @return not null source node of the deleted incoming flow
	 */
	private BPMNNode deleteIncomingFlow(Activity activity, BPMNDiagram bpmnDiagram) {
		Collection<BPMNEdge<? extends BPMNNode, ? extends BPMNNode> > incomingFlows
				= bpmnDiagram.getInEdges(activity);
		BPMNNode source = null;
		if (incomingFlows.size() == 1) {
			BPMNEdge<? extends BPMNNode, ? extends BPMNNode> incomingFlow = incomingFlows.iterator().next();
			source = incomingFlow.getSource();
			currentLabel = incomingFlow.getLabel();
			bpmnDiagram.removeEdge(incomingFlow);
		} else {
			throw new ConverterException("Expanded activity has many incomng control flows");
		}

		return source;
	}

	/**
	 * Delete outgoing flow of the activity, if activity has more than one outgoing flow,
	 * exception will be generated
	 *
	 * @param activity
	 * @param bpmnDiagram
	 * @return not null target node of the deleted outgoing flow
	 */
	private BPMNNode deleteOutgoingFlow(Activity activity, BPMNDiagram bpmnDiagram) {
		Collection<BPMNEdge<? extends BPMNNode, ? extends BPMNNode>> outgoingFlows
				= bpmnDiagram.getOutEdges(activity);
		BPMNNode target = null;
		if (outgoingFlows.size() == 1) {
			BPMNEdge<? extends BPMNNode, ? extends BPMNNode> outgoingFlow = outgoingFlows.iterator().next();
			target = outgoingFlow.getTarget();
			bpmnDiagram.removeEdge(outgoingFlow);
		} else {
			throw new ConverterException("Expanded activity has many outgoing control flows");
		}

		return target;
	}
}
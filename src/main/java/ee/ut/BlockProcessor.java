package ee.ut;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMin;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.Task;

import java.io.PrintStream;
import java.util.*;

public class BlockProcessor {

	public XLog log;
	public PrintStream printStream;
	public Map< String, Set< String > > successors;

	public MiningParameters inductiveMinerParams;
	public XEventClassifier xEventClassifier;

	public BlockProcessor( XLog xLog, Map< String, Set< String > > successors ) {
		this.log	= xLog;
		this.printStream = System.out;
		this.successors	= successors;
		this.xEventClassifier = LogProcessor.defaultXEventClassifier;

		//	Inductive Miner - incompleteness
		this.inductiveMinerParams = new MiningParametersIMin( );
		this.inductiveMinerParams.setClassifier( this.xEventClassifier );
	}

	/**
	 * Detect Block structure
	 * IF AND/Parallel Gateway is present - isolate potential events
	 * ELSE add Block's events to processed
	 * TODO: validate if "start events" block structure is the same as whole log's
	 */
	public List< Set< String >> FindParallelBranches( HashSet< String > processedEvents, HashSet< String > edgeEvents ) throws Exception {
		XLog xLog = XLogReader.filterSkipEvents( this.log, processedEvents );
		Block treeRoot = this.MineProcessTreeRoot( xLog );
		if ( treeRoot instanceof Block.Seq )
			treeRoot = FindFirstBlock( treeRoot );

		Block treeRootEdgeEvents = ( Block ) IMProcessTree.mineProcessTree(
				XLogReader.filterByEvents( this.log, edgeEvents )
				, this.inductiveMinerParams
		).getRoot( );

		printStream.println( "Edge tree:\t" + treeRootEdgeEvents.toString() );
		printStream.println( "Whole tree:\t" + treeRoot.toString() );

		if ( this.IsParallelGatewayPresent( treeRootEdgeEvents ) ) {

			List< Set< String > > eventsList;

			if ( treeRootEdgeEvents instanceof Block.And ) {
				eventsList = new BranchProcessor( xLog, printStream ).ExtractParallelBranches( );
				this.updateFringeEvents( processedEvents, edgeEvents, eventsList );
			}
			else {
				eventsList = this.FindParallelBranches( treeRoot );
				this.updateFringeEvents( processedEvents, edgeEvents, GetNodeTasks( treeRoot ) );
			}
			return eventsList;
		}

		this.updateFringeEvents( processedEvents, edgeEvents, GetNodeTasks( treeRoot ) );
		return null;
	}


	public boolean IsParallelGatewayPresent( HashSet< String > events ) {
		for ( Node node : IMProcessTree.mineProcessTree(
				XLogReader.filterByEvents( this.log, events )
				, this.inductiveMinerParams
		).getNodes( ) ) {
			if ( node instanceof Block.And )
				return true;
		}
		return false;
	}

	public boolean IsParallelGatewayPresent( Node xNode ) {
		for ( Node node : xNode.getProcessTree().getNodes( ) ) {
			if ( node instanceof Block.And )
				return true;
		}
		return false;
	}

	public Block MineProcessTreeRoot( XLog xLog ) throws Exception {
		Node node = IMProcessTree.mineProcessTree( xLog, this.inductiveMinerParams ).getRoot( );
		if ( ! ( node instanceof Block ) )
			throw new Exception( "Tree root is not a Block" );
		return ( Block ) node;
	}
	public Block FindFirstBlock( XLog xLog ) throws Exception {
		return FindFirstBlock( MineProcessTreeRoot( xLog ) );
	}

	public Block FindFirstBlock( Node node ) throws Exception {
		if ( ! ( node instanceof Block ) )
			throw new Exception( "Tree root is not a Block\n" + node.getProcessTree().toString() );

		if ( ( ( Block ) node ).getChildren( ).isEmpty( ) )
			throw new Exception( "Tree doesn't have children" + node.getProcessTree().toString() );

		Node firstChild = ( ( Block ) node ).getChildren( ).iterator( ).next( );

		if ( ! ( firstChild instanceof Block ) )
			throw new Exception( "First child is not a Block\n" + node.getProcessTree().toString() );

		return ( Block ) firstChild;
	}

	public void updateFringeEvents( HashSet< String > processedEvents, HashSet< String > edgeEvents, List< Set< String > > eventsList ) {
		HashSet< String > allEvents = new HashSet< >(  );

		for ( Set< String > events : eventsList )
			allEvents.addAll( events );

		this.updateFringeEvents( processedEvents, edgeEvents, allEvents );
	}

	public void updateFringeEvents( HashSet< String > processedEvents, HashSet< String > edgeEvents, Set< String > events ) {
		processedEvents.addAll( events );
//			Add BLock successors
		edgeEvents.addAll( GetSuccessors( events, successors ) );
		edgeEvents.removeAll( events );
	}

	public static Set< String > GetSuccessors( Collection< String > events, Map< String, Set< String > > successors ) {
		HashSet< String > result = new HashSet<>( );

		if ( events == null )
			return result;

		for ( String event : events )
			if ( successors.containsKey( event ) )
				result.addAll( successors.get( event ) );

		return result;
	}

	public static Set< String > GetNodeTasks( Node node ) {
		HashSet< String > result = new HashSet<>( );

		if ( node instanceof Task.Manual ) {
			result.add( node.getName( ) );
			return result;
		}
		LinkedList< Node > fringe = new LinkedList<>( );
		fringe.addAll( ( ( Block ) node ).getChildren( ) );

		while ( ! fringe.isEmpty( ) ) {
			node = fringe.pop( );

			if ( node instanceof Task.Manual )
				result.add( node.getName( ) );
			else if ( node instanceof Block )
				fringe.addAll( ( ( Block ) node ).getChildren( ) );
		}
		return result;
	}

	public static List< Set< String > > ExtractParallelBranches( Node node ) {
		List< Set< String > > result = new LinkedList<>( );

		if ( ! ( node instanceof Block.And ) )
			return result;

		for ( Node child : ( ( Block ) node ).getChildren( ) )
			result.add( GetNodeTasks( child ) );

		return result;
	}

	public List< Set< String > > FindParallelBranches( Block block ) throws Exception {
		LinkedList< Node > fringe = new LinkedList<>( );
		fringe.add( block );

		List< Set< String > > eventsOfParallelBranches = new LinkedList<>(  );

		while ( ! fringe.isEmpty( ) ) {
			Node node = fringe.pop( );

//			Block.Seq - could have multiple Parallel gateways and block size could be wrong
//			Example: Seq( S1, And( A1, B1, ... ), A5, S2, And( D1, E1, ... ), End )
			if ( node instanceof Block.Seq ) {
				eventsOfParallelBranches.addAll(
						new ContextAnalysis(
								XLogReader.filterByEvents( this.log, GetNodeTasks( node ) )
						).FindParallelBranches( )
				);
			}

			else if ( node instanceof Block.And ) {
				eventsOfParallelBranches.addAll(
						new BranchProcessor(
								XLogReader.filterByEvents( this.log, GetNodeTasks( node ) )
								, printStream
						).ExtractParallelBranches()
				);
			}

//			Block.Xor, Block.Or, ...
			else if ( node instanceof Block )
				fringe.addAll( ( ( Block ) node ).getChildren( ) );
		}

		return eventsOfParallelBranches;
	}
}
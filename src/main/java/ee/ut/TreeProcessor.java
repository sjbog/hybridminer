package ee.ut;

import ee.ut.classifiers.HeuristicEventsClassifier;
import org.deckfour.xes.classification.XEventAndClassifier;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventLifeTransClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.processmining.contexts.cli.CLIContext;
import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIM;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.Task;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

public class TreeProcessor {

	public String traceStartPseudoEvent = "__start__";
	public String traceEndPseudoEvent = "__end__";

//	Inductive Miner - incompleteness
	public MiningParameters inductiveMinerParams = new MiningParametersIM( );
	public static XEventClassifier defaultXEventClassifier = new XEventAndClassifier( new XEventNameClassifier(), new XEventLifeTransClassifier() );
	public XEventClassifier xEventClassifier = defaultXEventClassifier;
	public XLogInfo logInfo;
	public Map< String, Set< String > > predecessors;
	public Map< String, Set< String > > successors;
	public PrintStream printStream = System.out;
	public PluginContext pluginContext;

	//	Holds events which for sure belong to the branch
	public List< Set< String > > parallelBranches;
	//	Holds set of possible parallelBranches
	public Map< String, Set< Integer > > eventToBranch;

	public XLog log;
	public String SublogNamePrefix	= "Block";
	public Map< String, TreeProcessor > childBlocks = new HashMap<>(  );

	public TreeProcessor( XLog log ) {
		this.log = log;
		this.pluginContext = new CLIPluginContext( new CLIContext(), "Hybrid Miner" );
		inductiveMinerParams.setClassifier( xEventClassifier );
	}

	public TreeProcessor( XLog log, PrintStream printStream ) {
		this.log = log;
		this.pluginContext = new CLIPluginContext( new CLIContext(), "Hybrid Miner" );

		if ( printStream == null )
			printStream = System.out;

		this.printStream = printStream;
		inductiveMinerParams.setClassifier( xEventClassifier );
	}

	public void mine( ) {
		analyzeSuccPred( log );
		LinkedList< String > fringe = walkPreorder( traceStartPseudoEvent, successors );
		HashSet< String > processed	= new HashSet<>();

		printStream.println( "Predecessors:" );
		for ( Map.Entry< String, Set< String >> entry : predecessors.entrySet() )
			printStream.println( "\t" + entry.getKey() + ": " + entry.getValue().toString() );

		printStream.println( "Successors:" );
		for ( Map.Entry< String, Set< String >> entry : successors.entrySet() )
			printStream.println( "\t" + entry.getKey() + ": " + entry.getValue().toString() );

		while ( ! fringe.isEmpty() ) {
			String event	= fringe.pop();
			if ( processed.contains( event ) )
				continue;

			if ( successors.getOrDefault( event, new HashSet<>(  ) ).size() > 1 ) {
				Set< String > events = successors.get( event );
//				for ( String xEvent : new HashSet<String>( successors.get( event ) ) )
//					events.addAll( predecessors.getOrDefault( xEvent, new HashSet<>(  ) ) );

				XLog startEventsLog = XLogReader.filterByEvents( log, events );

				XAttributeLiteralImpl nameAttr = ( XAttributeLiteralImpl ) startEventsLog.getAttributes( ).get( "concept:name" );
				nameAttr.setValue( String.format( "%s Start Events", nameAttr.getValue( ) ) );

//				XLogWriter.saveXesGz( startEventsLog, "output/startEvents" );

				if ( divideLogIntoSublogs( event ) ) {
					for ( Set< String > branchEvents : parallelBranches )
						processed.addAll( branchEvents );
				}
			}
			processed.add( event );
		}

		XAttributeLiteralImpl nameAttr = ( XAttributeLiteralImpl ) log.getAttributes( ).get( "concept:name" );
		nameAttr.setValue( String.format( "%s Root", nameAttr.getValue( ) ) );
	}

	public String fetchName( XEvent event ) {
		return logInfo.getEventClasses().getClassOf( event ).toString();
	}

	public static String fetchName( XEvent event, XLogInfo logInfo ) {
		return logInfo.getEventClasses().getClassOf( event ).toString();
	}

	public static LinkedList< String > walkPreorder( String startEvent, Map< String, Set< String >> successors ) {
		LinkedList< String > result	= new LinkedList<>(  );
		LinkedList< String > fringe	= new LinkedList<>( Arrays.asList( startEvent ) );
		HashSet< String > processed	= new HashSet<>();

		while ( ! fringe.isEmpty() ) {
			String event	= fringe.pop();
			if ( processed.contains( event ))
				continue;
			result.add( event );
			processed.add( event );
			fringe.addAll( successors.getOrDefault( event, new HashSet<>( ) ) );
		}
		return result;
	}
	
	public boolean divideLogIntoSublogs( String splitEvent ) {
		analyzeSuccPred( log );

		parallelBranches = findANDBranches(
				XLogReader.filterByEvents( log, successors.get( splitEvent ) )
		);
		if ( parallelBranches == null ) return false;

		parallelBranches	= findBranchEvents( log, parallelBranches );
		printStream.println( "Found parallel branches:" );

		for ( int i = 0, andBranchesSize = parallelBranches.size( ) ; i < andBranchesSize ; i++ ) {
			printStream.println( String.format( "%s: %s", i, parallelBranches.get( i ) ) );

			if ( parallelBranches.get( i ).size() > 1 ) {
				String name	= String.format( "%s_%d", SublogNamePrefix, childBlocks.size( ) );
				XLog sublog	= XLogReader.filterRemoveByEvents( log, parallelBranches.get( i ), name );

				childBlocks.put( name, new TreeProcessor( sublog, printStream ) );
				childBlocks.get( name ).SublogNamePrefix	= name;
				childBlocks.get( name ).mine();

				XAttributeLiteralImpl nameAttr = ( XAttributeLiteralImpl ) sublog.getAttributes( ).get( "concept:name" );
				nameAttr.setValue( String.format( "%s %s", nameAttr.getValue( ), name ) );
			}
		}
		return true;
	}

	public void analyzeSuccPred( XLog log ) {
		successors = new HashMap<>( );
		predecessors = new HashMap<>( );
		successors.put( traceStartPseudoEvent, new HashSet<>( ) );
		predecessors.put( traceEndPseudoEvent, new HashSet<>( ) );
		logInfo = XLogInfoImpl.create( log, xEventClassifier );

		for ( XTrace trace : log ) {
			if ( trace.size( ) < 1 )
				continue;

			String prevEvent, currEvent = traceStartPseudoEvent;

//			successors.get( traceStartPseudoEvent ).add( currEvent );

			for ( int i = 0, size = trace.size( ) ; i < size ; i++ ) {
				prevEvent = currEvent;
				currEvent = fetchName( trace.get( i ) );

				successors.putIfAbsent( prevEvent, new HashSet<>( ) );
				successors.get( prevEvent ).add( currEvent );

				predecessors.putIfAbsent( currEvent, new HashSet<>( ) );
				predecessors.get( currEvent ).add( prevEvent );
			}
			predecessors.get( traceEndPseudoEvent ).add( currEvent );
		}
	}

	public List< Set< String > > findANDBranches( XLog log ) {
		Node treeRoot = IMProcessTree.mineProcessTree( log, inductiveMinerParams ).getRoot( );

		if ( ! ( treeRoot instanceof Block.And ) ) {
			return null;
		}
		return (( Block ) treeRoot ).getChildren( ).stream( ).map( TreeProcessor:: getNodeTasks ).collect( Collectors.toCollection( ArrayList::new ) );
	}

	public static Set< String > getNodeTasks( Node node ) {
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

	public List< Set< String > > findBranchEvents( XLog log, List< Set< String > > branches ) {
		eventToBranch = new HashMap<>( );

//		Fill start events
		for ( int i1 = 0, branchesSize = branches.size( ) ; i1 < branchesSize ; i1++ ) {
			for ( String event : branches.get( i1 ) ) {
				eventToBranch.put( event, new HashSet<>( Arrays.asList( i1 ) ) );
			}
		}
		for ( XTrace trace : log ) {
			Set< Integer > openedBranches = new HashSet<>( );
			String event;

			for ( int i = 0, size = trace.size( ) ; i < size ; i++ ) {
				event = fetchName( trace.get( i ) );

				if ( ! eventToBranch.containsKey( event ) ) {
					eventToBranch.put( event, new HashSet<>( openedBranches ) );
					continue;
				}

				if ( eventToBranch.get( event ).size( ) == 1 ) {
					openedBranches.addAll( eventToBranch.get( event ) );
					continue;
				}

				eventToBranch.get( event ).retainAll( openedBranches );
			}
		}
		printStream.println( "Events to branches:" );
		printStream.println( eventToBranch );
//		Tail events - belong to all branches
		return	processTailEvents( branches );
	}

	public List< Set< String >> processTailEvents( List< Set< String >> branches ) {
		Set< String > tailEvents = new HashSet<>( );
		int branchesSize	= branches.size();

//		Remove events which belong to all parallelBranches or belong to none
		for ( String event : new HashSet<>( eventToBranch.keySet( ) ) ) {
			if ( eventToBranch.get( event ).size( ) == branchesSize ) {
				tailEvents.add( event );
				eventToBranch.remove( event );
			}
			else if ( eventToBranch.get( event ).isEmpty( ) )
				eventToBranch.remove( event );
		}

		eventToBranch.putAll(
				new HeuristicEventsClassifier( xEventClassifier, pluginContext )
					.mapEventsToBranches(
							log, eventToBranch
							, findParallelEvents( eventToBranch, tailEvents, predecessors )
					)
		);

//		tail events which have all the predecessors in the same branch should also belong to that branch
		for ( String event : tailEvents ) {
			if ( eventToBranch.containsKey( event ))
				continue;

			Set< Integer >predecessorBranches	= new HashSet<>(  );
			for ( String predecessor : predecessors.getOrDefault( event, new HashSet<>(  ) ))
				predecessorBranches.addAll( eventToBranch.getOrDefault( predecessor, new HashSet<>(  ) ) );

			if ( predecessorBranches.size() == 1 )
				eventToBranch.put( event, predecessorBranches );
		}
		for ( String event : eventToBranch.keySet() )
			branches.get( eventToBranch.get( event ).iterator().next() ).add( event );

		return branches;
	}

	/**
	 * Find all predecessor events of known branched events - those are also branched events
	 */
	public static Set< String > findParallelEvents( Map< String, Set< Integer > > eventToBranch, Set< String > tailEvents, Map< String, Set< String > > predecessors ) {
		Set< String > processedEvents = new HashSet<>();
		LinkedList< String > fringe = new LinkedList<>( eventToBranch.keySet() );

		while ( ! fringe.isEmpty( ) ) {
			String event = fringe.pop( );
			Set< String > predecessorEvents = predecessors.getOrDefault( event, new HashSet<>( ) );

			processedEvents.add( event );
			predecessorEvents.removeAll( processedEvents );
			fringe.addAll( predecessorEvents );
		}
		processedEvents.retainAll( tailEvents );
		return processedEvents;
	}

	public ProcessTree toProcessTree() {
		ProcessTree result = IMProcessTree.mineProcessTree( log, inductiveMinerParams );
		if ( result.getRoot( ) instanceof Block )
			ProcessTreeIterator( ( Block ) result.getRoot( ), childBlocks );
		return result;
	}

//	Recursively iterates over Block's children, replacing childBlocks' keys with IMProcessTree
	public static void ProcessTreeIterator( Block root, Map< String, TreeProcessor > sublogs ) {
		int i = 0;
		for (Iterator< Node > nodeIterator = root.iterator(); nodeIterator.hasNext( ); i ++ ) {
			Node node = nodeIterator.next();
			String nodeName	= node.getName();

//			Remove +complete suffix
			if ( nodeName.endsWith( "+complete" ))
				nodeName	= nodeName.substring( 0, nodeName.indexOf( "+complete" ) );

			if ( node instanceof Task.Manual && sublogs.containsKey( nodeName ) ) {
				( ( Block ) node.getParents().toArray()[ 0 ] ).swapChildAt(
						sublogs.get( nodeName ).toProcessTree( ).getRoot( ), i
				);
			}
			else if ( node instanceof Block && ( ( Block ) node ).numChildren() > 0 ) {
				ProcessTreeIterator( ( Block ) node, sublogs );
			}
		}
	}

	public Map< String, XLog > getChildLogs() {
		HashMap< String, XLog > result = new HashMap<>();

		for( String name : this.childBlocks.keySet() ) {
			result.put( name, childBlocks.get( name ).log );
			result.putAll( childBlocks.get( name ).getChildLogs() );
		}
		return	result;
	}
}

package ee.ut;

import ee.ut.classifiers.HeuristicEventsClassifier;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.cli.CLIContext;
import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMin;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;

import java.io.PrintStream;
import java.util.*;

public class BranchProcessor {
	public XLog log;
	public PrintStream printStream = System.out;
	public PluginContext pluginContext;
	public XEventClassifier xEventClassifier;
	public MiningParameters inductiveMinerParams;

	public ContextAnalysis contextAnalysis;
	public List< Set< String > > parallelBranches;
	public Map< String, Set< Integer > > eventToBranch;

	public BranchProcessor( XLog xLog ) {
		this.log = xLog;
		this.pluginContext = new CLIPluginContext( new CLIContext( ), "Hybrid Miner" );
		this.xEventClassifier = LogProcessor.defaultXEventClassifier;

		//	Inductive Miner - incompleteness
		this.inductiveMinerParams = new MiningParametersIMin( );
		this.inductiveMinerParams.setClassifier( this.xEventClassifier );
	}

	public BranchProcessor( XLog xLog, PrintStream ps ) {
		this( xLog );
		if ( ps != null )
			this.printStream = ps;
	}

	public List< Set< String > > ExtractParallelBranches() {
		this.contextAnalysis = new ContextAnalysis( this.log );
		this.contextAnalysis.AnalyzeSuccPred( );

//		Find start events
		this.parallelBranches = BlockProcessor.ExtractParallelBranches(
				IMProcessTree.mineProcessTree(
						XLogReader.filterByEvents(
								this.log
								, this.contextAnalysis.successors.get( this.contextAnalysis.traceStartPseudoEvent )
						), this.inductiveMinerParams
				).getRoot( )
		);
		return this.ExtendAllBranchEvents( this.parallelBranches );
	}

	public List< Set< String > > ExtendAllBranchEvents( List< Set< String > > branchStartEvents ) {
		if ( this.contextAnalysis == null ) {
			this.contextAnalysis = new ContextAnalysis( this.log );
			this.contextAnalysis.AnalyzeSuccPred( );
		}
		this.eventToBranch = new HashMap<>( );

//		Fill start events
		for ( int i = 0, size = branchStartEvents.size( ) ; i < size ; i++ )
			for ( String event : branchStartEvents.get( i ) )
				eventToBranch.put( event, new HashSet<>( Arrays.asList( i ) ) );

//		Mark events with a single branch opened
		for ( XTrace trace : log ) {
			Set< Integer > openedBranches = new HashSet<>( );
			String event;

			for ( int i = 0, size = trace.size( ) ; i < size ; i++ ) {
				event = this.contextAnalysis.fetchName( trace.get( i ) );

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
		processTailEvents( branchStartEvents.size() );

		for ( String event : eventToBranch.keySet() )
			branchStartEvents.get( eventToBranch.get( event ).iterator().next() ).add( event );

		return branchStartEvents;
	}

	public void processTailEvents( int branchesSize ) {
		Set< String > tailEvents = new HashSet<>( );

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
								, findParallelEvents( eventToBranch, tailEvents, this.contextAnalysis.predecessors )
						)
		);

//		tail events which have all the predecessors in the same branch should also belong to that branch
		for ( String event : tailEvents ) {
			if ( eventToBranch.containsKey( event ))
				continue;

			Set< Integer >predecessorBranches	= new HashSet<>(  );
			for ( String predecessor : this.contextAnalysis.predecessors.getOrDefault( event, new HashSet<>(  ) ))
				predecessorBranches.addAll( eventToBranch.getOrDefault( predecessor, new HashSet<>(  ) ) );

			if ( predecessorBranches.size() == 1 )
				eventToBranch.put( event, predecessorBranches );
		}
	}

	/**
	 * Find all predecessor events of known branched events - those are also branched events
	 */
	public static Set< String > findParallelEvents( Map< String, Set< Integer > > eventToBranch, Set< String > tailEvents, Map< String, Set< String > > predecessors ) {
		Set< String > processedEvents = new HashSet<>();
		LinkedList< String > fringe = new LinkedList<>( eventToBranch.keySet( ) );

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
}

package ee.ut;

import org.deckfour.xes.classification.XEventAndClassifier;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventLifeTransClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.io.PrintStream;
import java.util.*;

public class ContextAnalysis {

	public XLog log;
	public XLogInfo logInfo;
	public PrintStream printStream;

	public Map< String, Set< String > > predecessors, successors;
	public String traceStartPseudoEvent = "__start__";
	public String traceEndPseudoEvent = "__end__";

	public static XEventClassifier defaultXEventClassifier = new XEventAndClassifier( new XEventNameClassifier( ), new XEventLifeTransClassifier( ) );
	public XEventClassifier xEventClassifier = defaultXEventClassifier;

	public ContextAnalysis( XLog xLog ) {
		this.log = xLog;
		this.printStream = System.out;
	}
	public ContextAnalysis( XLog xLog, PrintStream ps ) {
		this.log = xLog;
		if ( ps != null )
			this.printStream = ps;
	}

	public void AnalyzeSuccPred() {
		this.successors = new HashMap<>( );
		this.predecessors = new HashMap<>( );
		this.logInfo = XLogInfoImpl.create( log, xEventClassifier );

		this.successors.put( traceStartPseudoEvent, new HashSet< String >( ) );
		this.predecessors.put( traceEndPseudoEvent, new HashSet< String >( ) );

		for ( XTrace trace : log ) {
			if ( trace.size( ) < 1 )
				continue;

			String prevEvent, currEvent = traceStartPseudoEvent;

			for ( int i = 0, size = trace.size( ) ; i < size ; i++ ) {
				prevEvent = currEvent;
				currEvent = fetchName( trace.get( i ) );

				successors.putIfAbsent( prevEvent, new HashSet< String >( ) );
				successors.get( prevEvent ).add( currEvent );

				predecessors.putIfAbsent( currEvent, new HashSet< String >( ) );
				predecessors.get( currEvent ).add( prevEvent );
			}
			predecessors.get( traceEndPseudoEvent ).add( currEvent );

			successors.putIfAbsent( currEvent, new HashSet< String >( ) );
			successors.get( currEvent ).add( traceEndPseudoEvent );
		}
	}

	public String fetchName( XEvent event ) {
		return logInfo.getEventClasses( ).getClassOf( event ).toString( );
	}

	public List< Set< String > > FindParallelBranches( ) throws Exception {
		this.AnalyzeSuccPred();

		List< Set< String > > result, parallelBranches = new LinkedList<>( );
		HashSet< String > processedEvents = new HashSet<>( );
		HashSet< String > edgeEvents = new HashSet<>( successors.get( traceStartPseudoEvent ) );

//		100 => counter to protect against unlimited loops
		for ( int i = 0; ! edgeEvents.isEmpty() && i < 100; i++ ) {

			if ( edgeEvents.size( ) > 1 )
				edgeEvents.remove( traceEndPseudoEvent );

//			Straight Sequence - only 1 immediate successor
			if ( edgeEvents.size( ) == 1 ) {
				String event = edgeEvents.iterator( ).next( );
				processedEvents.add( event );

				edgeEvents.clear( );
				if ( event.equals( traceEndPseudoEvent ) )
					break;

				edgeEvents.addAll( successors.get( event ) );
				continue;
			}

//			There is more than 1 immediate successor
			BlockProcessor blockProcessor = new BlockProcessor( this.log, this.successors );
			blockProcessor.printStream = printStream;

			try {
//				processedEvents and edgeEvents are updated by ref
				result = blockProcessor.FindParallelBranches( processedEvents, edgeEvents );
			} catch ( Exception e ) {
				result = null;
			}

			if ( result == null || result.isEmpty( ) ) {
//				Force further analysis
				blockProcessor.updateFringeEvents( processedEvents, edgeEvents, new HashSet< >( edgeEvents ) );
				continue;
			}

//			Capture tail events, since BlockProcessor will handle only first child Block
//			Example: Seq( S1, And( A1, B1, ... ), A5, S2, And( D1, E1, ... ),End)
//			result	= new BranchProcessor( this.log ).ExtendAllBranchEvents( result );
			parallelBranches.addAll( result );
		}
		return parallelBranches;
	}
}

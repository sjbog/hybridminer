import ee.ut.StructuredSequences;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.util.*;

public class Main {

	static List< String > Find_start_nodes (
			Map< String, Set< String > > predecessors,
			Map< String, Set< String > > successors,
			int threshold,
			Set< String > structured_events )	{

		List< String > start_nodes	= new LinkedList<> (  );

		for	( String event : structured_events )	{

			int	predecessors_count	= predecessors.getOrDefault ( event, new HashSet< String > () ).size ();

			if	( predecessors_count == 0	|| predecessors_count > threshold )	{
				start_nodes.add ( event );
			}

//			Check if there is a start node before
			else	{
				boolean	has_start_node	= false;

				for ( String predecessor : predecessors.get ( event ) )	{

					has_start_node	|= ( threshold >= successors.get ( predecessor ).size () );
				}

				if	( ! has_start_node )	{
					start_nodes.add ( event );
				}
			}
		}

		return	start_nodes;
	}

	static void Group_structured_events (
			Map< String, Set< String > > predecessors,
			Map< String, Set< String > > successors,
			int threshold	)	{

		Set < String >	structured_events	= new HashSet<> ( );

		for	( Map.Entry< String, Set< String > > entry : predecessors.entrySet () )	{

			if ( threshold >= entry.getValue ().size () )	{
				structured_events.add ( entry.getKey () );
			}
		}

		for	( Map.Entry< String, Set< String > > entry : successors.entrySet () )	{

			if ( threshold >= entry.getValue ().size () )	{
				structured_events.add ( entry.getKey () );
			}
		}

		System.out.println ( structured_events );
		System.out.println ( );

		Map < String, Set < String > >	groups	= new HashMap<> ();

		for ( String node : Find_start_nodes ( predecessors, successors, threshold, structured_events ) )	{

			groups.put ( node, new HashSet< String > (  ) );
		}


		System.out.println ( groups.keySet () );
	}


	public static void main ( String args[] ) {

		try {
			XLog log = ee.ut.XLogReader.openLog ( "data/L1.mxml" );
//			XLog log = XLogReader.openLog ( "data/financial_log.mxml.gz" );

//			last 20 traces
//			List< XTrace >	log_chunk	= log.subList ( log.size ( ) - 20, log.size () );
			List< XTrace >	log_chunk	= log.subList ( log.size ( ) - 200, log.size () );
//
//			List< Set< String > > events_result = ee.ut.Graph.contextAnalysis ( log_chunk, 4, 4 );
//			List< List< List<String> > > events_result2 = ee.ut.XLogReader.divideLog ( log_chunk, events_result.get ( 0 ), events_result.get ( 1 ) );
//
//			System.out.println ( events_result2.get ( 0 ));
//			System.out.println ();
//			System.out.println ( events_result2.get ( 1 ));


			StructuredSequences structured_sequences	= StructuredSequences.from_traces ( log_chunk, 3 );

			System.out.println ();

			for ( Set < String > key : structured_sequences )	{

				System.out.println ( String.format ( "%s %s", key.hashCode (), key ) );

//				for ( List<String> row : structured_sequences.get ( key ) ) {
//					System.out.println ( "\t" + row );
//				}
			}
			structured_sequences.filter_log ( log_chunk );

		} catch ( Exception e ) {
			e.printStackTrace ( );
		}

	}

	static Map< String, Set<String> > filterByThreshold ( Map< String, Set<String> > dataSet, int threshold )	{
		Map<String, Set<String>> result	= new HashMap<> ();

		for ( String key : dataSet.keySet () )	{
			if ( dataSet.get ( key ).size () <= threshold )	{
				result.put ( key, dataSet.get ( key ) );
			}
		}
		return	result;
	}

}
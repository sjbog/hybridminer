package ee.ut;

import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.model.XTrace;

import java.util.*;

public class Graph {
	/**
	 * Builds a HashMap of immediate successors and predecessors
	 *
	 * { "node_a" : set ( "node_b", "node_c" ) }
	 *
	 */
	public static Object[] Build_degrees_graph ( List< XTrace > traces_list )	{

		XTrace			trace;
		XExtendedEvent curr_event, next_event;

		Map< String, Set< String > > predecessors = new HashMap<> ( );
		Map< String, Set< String > > successors   = new HashMap<> ( );


		for	( int trace_i = 0, traces_len = traces_list.size () ; trace_i < traces_len ; ++ trace_i )	{

			trace	= traces_list.get ( trace_i );
//			System.out.println ( XConceptExtension.instance ().extractName ( trace ) );

			for	( int event_i = 0, events_len = trace.size () -1 ; event_i < events_len ; ++ event_i )	{

				curr_event	= XExtendedEvent.wrap ( trace.get ( event_i ) );
				next_event	= XExtendedEvent.wrap ( trace.get ( event_i +1 ) );

				String curr_event_name = curr_event.getName ( ) + "#" + curr_event.getTransition ( );
				String next_event_name = next_event.getName ( ) + "#" + next_event.getTransition ( );

				if	( ! successors.containsKey ( curr_event_name ) )	{
					successors.put ( curr_event_name, new HashSet< String > ( ) );
				}
				if	( ! predecessors.containsKey ( next_event_name ) )	{
					predecessors.put ( next_event_name, new HashSet< String > ( ) );
				}

				successors	.get ( curr_event_name ).add ( next_event_name );
				predecessors.get ( next_event_name ).add ( curr_event_name );

//				String	name		= curr_event.getName ( );
//				Date	timestamp	= curr_event.getTimestamp ( );
//				String	event_type	= curr_event.getTransition ( );
//				String	resource	= curr_event.getResource ( );

//				System.out.println ( String.format ( "\t[ %s : %s ] %s\t: %s", event_type, timestamp, resource, name ) );
			}
		}
		return new Object[]{ predecessors, successors };
	}

	public static Map< String, String > Find_structured_events ( List< XTrace > traces_list, int threshold )	{

		HashMap< String, Set< String > >	predecessors, successors;

		Object[]	tmp	= Build_degrees_graph ( traces_list );

		predecessors	= ( HashMap< String, Set< String > > ) tmp [ 0 ];
		successors		= ( HashMap< String, Set< String > > ) tmp [ 1 ];


		Map< String, String >	structured_events	= new HashMap<> ( );

		for	( Map.Entry< String, Set< String > > entry : predecessors.entrySet () )	{

			if ( threshold >= entry.getValue ().size () )	{

				int	successors_count	= successors.getOrDefault ( entry.getKey (), new HashSet< String > () ).size ();

				if ( successors_count > threshold	|| successors_count == 0 ) {
					structured_events.put ( entry.getKey ( ), "end_node" );
				}	else	{
					structured_events.put ( entry.getKey ( ), "" );
				}
			}
		}

		for	( Map.Entry< String, Set< String > > entry : successors.entrySet () )	{

			if ( threshold >= entry.getValue ().size () )	{

				int	predecessors_count	= predecessors.getOrDefault ( entry.getKey (), new HashSet< String > () ).size ();

				if ( predecessors_count > threshold	|| predecessors_count == 0 ) {
					structured_events.put ( entry.getKey ( ), "start_node" );
				}	else	{
					structured_events.put ( entry.getKey ( ), "" );
				}
			}
		}

		return	structured_events;
	}


	public static List< Set<String> > contextAnalysis ( List< XTrace > log, int predecessors_threshold, int successors_threshold )	{

		HashMap< String, Set< String > >	predecessors, successors;

		Object[]	tmp	= Build_degrees_graph ( log );

		predecessors	= ( HashMap< String, Set< String > > ) tmp [ 0 ];
		successors		= ( HashMap< String, Set< String > > ) tmp [ 1 ];

		Set< String >	distinct_events	= new HashSet<> ( predecessors.keySet () );
		distinct_events.addAll ( successors.keySet () );

		Set< String >	structured_events	= new HashSet<> (  );
		Set< String >	unstructured_events	= new HashSet<> ( );

		for ( String event : distinct_events )	{

			int predecessors_count	= 0;
			int successors_count	= 0;

			if ( predecessors.containsKey ( event ) )	{
				predecessors_count	= predecessors.get ( event ).size ();
			}
			if ( successors.containsKey ( event ) )	{
				successors_count	=successors.get ( event ).size ();
			}

			if	( predecessors_count <= predecessors_threshold
					|| successors_count <= successors_threshold )	{

				structured_events.add ( event );
			}
			else {
				unstructured_events.add ( event );
			}
		}

		List< Set<String> >	result	= new LinkedList<> (  );
		result.add ( structured_events );
		result.add ( unstructured_events );

		return	result;
	}
}

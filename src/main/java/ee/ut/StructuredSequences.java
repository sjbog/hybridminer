package ee.ut;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;

import java.util.*;

public class StructuredSequences extends LinkedList< Set< String > > {

	public Map< String, String > structured_events;

	public StructuredSequences ( Map< String, String > structured_events ) {
		super ();
		this.structured_events = structured_events;
	}

	public static StructuredSequences from_traces ( List< XTrace > traces_list, int threshold )	{
		StructuredSequences	ss	= new StructuredSequences ( Graph.Find_structured_events ( traces_list, threshold ) );

		for ( XTrace trace : traces_list ) {
//			System.out.println ( "Trace name : " + XConceptExtension.instance ( ).extractName ( trace ) );
			ss.feed ( trace );
		}

		return	ss;
	}

	public int Find_intersecting_key ( Set< String >	sequence_set ) {

		LinkedList< Integer >	keys	= new LinkedList<> (  );
		Set< String >	sequence_copy, entry;

		for ( int i = 0, size = this.size (); i < size ; ++ i )	{

			entry	= this.get ( i );
			sequence_copy	= new HashSet<> ( sequence_set );

//			intersection update
			sequence_copy.retainAll ( entry );

			if ( ! sequence_copy.isEmpty () )	{
				keys.add ( i );
			}
		}

		if ( keys.isEmpty ( ) )			return -1;
		else if ( keys.size () == 1 )	return keys.pop ();

		return this.Merge_keys ( keys );
	}

	public int Merge_keys ( LinkedList< Integer > keys )	{

		Collections.sort ( keys );
		int key	= keys.pollFirst ();

		while	( ! keys.isEmpty () )	{

			int old_key	= keys.pop ();

			this.get ( key ).addAll ( this.remove ( old_key ) );
		}

		return	key;
	}

	/**
	 * Returns key
	 */
	public void put ( Collection< String > sequence ) {

		Set< String >	sequence_set	= new HashSet<> ( sequence );
		int key = this.Find_intersecting_key ( sequence_set );

		if ( key == -1 )	{

			this.add ( sequence_set );
		}
		else {
			this.get ( key ).addAll ( sequence_set );
		}
	}

	public void feed ( XTrace trace )	{

		Set < String > sequence = new HashSet<> ( );
		String name, node_type, seq_name;

		for ( XEvent event : trace ) {

			XExtendedEvent extended_event = XExtendedEvent.wrap ( event );
			name		= extended_event.getName ( ) + "#" + extended_event.getTransition ( ) ;
			node_type	= structured_events.getOrDefault ( name, "unstructured" );

			switch ( node_type ) {
				case "start_node":
					if ( sequence.isEmpty ( ) )	sequence.add ( name );
					else {
						this.put ( sequence );

						sequence = new HashSet<> ( );
						sequence.add ( name );
					}
					break;

				case "end_node":
					sequence.add ( name );
					this.put ( sequence );
					sequence = new HashSet<> ( );
					break;

				case "":
					sequence.add ( name );
					break;


				case "unstructured":
				default:
					if ( !sequence.isEmpty ( ) ) {
						this.put ( sequence );
						sequence = new HashSet<> ( );
					}
					break;
			}
		}

		if ( !sequence.isEmpty ( ) ) {
			this.put ( sequence );
		}
	}

	public Map< String, Integer > get_events_map ()	{

		Map< String, Integer > events_map = new HashMap<> ( );

		for ( int i = 0, size = this.size (); i < size ; i ++ )	{
			for ( String name : this.get ( i ) ) {
				events_map.put ( name, i );
			}
		}

		return events_map;
	}

	public void filter_log ( List< XTrace > traces_list )	{

//			ArrayList< List< XTrace >>	structured_traces	= new ArrayList<> (  );

		Map< String, Integer > events_map = this.get_events_map ( );
		System.out.println ();
		System.out.println ( events_map );
		System.out.println ();


		for ( XTrace trace : traces_list )	{

			ArrayList< List< String > > curr_trace_structures	= new ArrayList<> (  );

			System.out.println ( "Trace name : " + XConceptExtension.instance ( ).extractName ( trace ) );

			for ( int i = 0; i < this.size () ; i ++ )	{
				curr_trace_structures.add ( new ArrayList< String > (  ) );
			}

			for ( XEvent event : trace ) {

				XExtendedEvent extended_event = XExtendedEvent.wrap ( event );
				String	name		= extended_event.getName ( ) + "#" + extended_event.getTransition ( ) ;

				if ( structured_events.containsKey ( name )) {

					int bucket_index = events_map.get ( name );

//					if ( curr_trace_structures.get ( bucket_index ).isEmpty () )	{

						System.out.println ( String.format ( "\t[ %s ] %s", bucket_index, name ));
//					}
					curr_trace_structures.get ( bucket_index ).add ( name );
				}
				else	{
					System.out.println ( "\t" + name );
				}
			}

		}


//				XExtendedEvent prefixEvent = new XExtendedEvent (
//						new XEventImpl ( ( XAttributeMap ) new XExtendedEvent ( trace.get ( 0 ) ).getAttributes ().deepcopy () )
//				);
//				XExtendedEvent suffixEvent = ( XExtendedEvent ) prefixEvent.deepcopy ();
//
//				prefixEvent.setName ( "Prefix event" );
//				suffixEvent.setName ( "Suffix event" );
//
//				XExtendedEvent event	= new XExtendedEvent ( trace.get ( 0 ) );
//				while ( prefixSet.contains ( event.getName () )	&& trace.size () > 0 ) {
//					trace.remove ( 0 );
//					event	= new XExtendedEvent ( trace.get ( 0 ) );
//				}
//				int trace_len = trace.size ( );
//				if ( trace_len > 0 ) {
//					event = new XExtendedEvent ( trace.get ( trace_len - 1 ) );
//				}
//				while ( suffixSet.contains ( event.getName () )	&& trace.size () > 0 )	{
//					trace.remove ( trace_len - 1 );
//					trace_len --;
//					event	= new XExtendedEvent ( trace.get ( trace_len -1 ) );
//				}
//				trace.add ( 0, prefixEvent );
//				trace.add ( suffixEvent );
//
//				trace_len = trace.size ( );

//			}
	}
}

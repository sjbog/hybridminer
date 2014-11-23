package hybridminer_refactor;


import ee.ut.XLogReader;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.io.PrintStream;
import java.util.*;

public class LogProcessor {

	HashMap<String, HashMap<String, Double>>	predecessors	= new HashMap<>();
	HashMap<String, HashMap<String, Double>>	successors		= new HashMap<>();
	HashMap<String, Double>	events_counter	= new HashMap<>();

	HashSet<String> analyzed_events	= new HashSet<> ( );
	ArrayList<String>	groupAdded	= new ArrayList<> ( );

	HashMap< Integer, Set< String > > procedural_event_groups = new HashMap<> ( );

	PrintStream print_out = System.out ;

	public boolean flag = false;
	public String	trace_start_pseudo_name	= "__start__";
	public String	trace_end_pseudo_name	= "__end__";

	public static XFactory factory = XFactoryRegistry.instance ( ).currentDefault();

	public static XLog slice_last_n ( XLog log, int n ) {
		List< XTrace > tmp_sub_log = log.subList ( log.size ( ) - n, log.size ( ) );

		log	= factory.createLog ( );
		log.addAll ( tmp_sub_log );

		return log;
	}

	public static String fetch_name ( XExtendedEvent event )	{
		return	String.format ( "%s#%s" , event.getName (), event.getTransition () );
	}

	public LogProcessor ( String file_path ) {

		XLog log;
		flag = true;

		try {

			log = XLogReader.openLog ( file_path );
			log = slice_last_n ( log, 20 );

			print_out = new PrintStream (".\\output_mod\\output.txt");

		} catch ( Exception e ) {
			e.printStackTrace ( );
			return;
		}

		analyze_events ( log );
		group_events ();
		split_log ( log );
	}

	public void update_event_counters ( String curr_event_name, String next_event_name )	{

		events_counter.put ( curr_event_name, events_counter.getOrDefault ( curr_event_name, 0.0 ) + 1.0 );

		if	( ! successors.containsKey ( curr_event_name ) )	{
			successors.put ( curr_event_name, new HashMap<String, Double> (  ) );
		}

		if	( ! predecessors.containsKey ( next_event_name ) )	{
			predecessors.put ( next_event_name, new HashMap<String, Double> ( ) );
		}

		successors	.get ( curr_event_name ).put ( next_event_name,
				successors.get ( curr_event_name ).getOrDefault ( next_event_name, 0.0 ) + 1
		);

		predecessors.get ( next_event_name ).put ( curr_event_name,
				predecessors.get ( next_event_name ).getOrDefault ( curr_event_name, 0.0 ) +1
		);
	}

	public void analyze_events ( XLog log )	{
//		frequencies -> eventFrequencies -> events_counter

		for ( XTrace trace : log ) {

			XExtendedEvent	event	= XExtendedEvent.wrap ( trace.get ( 0 ) );

			String	curr_event_name, next_event_name;

//			Add trace start pseudo event
			curr_event_name	= trace_start_pseudo_name;
			next_event_name	= fetch_name ( event );
			update_event_counters ( curr_event_name, next_event_name );


			for ( int i = 1, size = trace.size () ; i < size ; i ++ )	{

				curr_event_name	= next_event_name;

				event	= XExtendedEvent.wrap ( trace.get ( i ) );
				next_event_name	= fetch_name ( event );

				update_event_counters ( curr_event_name, next_event_name );
			}

//			Add trace end pseudo event
			curr_event_name	= next_event_name;
			next_event_name	= trace_end_pseudo_name;
			update_event_counters ( curr_event_name, next_event_name );
		}

		events_counter.put ( trace_start_pseudo_name, ( double ) log.size () );
		events_counter.put ( trace_end_pseudo_name,   ( double ) log.size () );

		print_out.println ( events_counter );
	}

	public int groupID = 1;
	public int level = 1;
	public int maximumSuccessors = 4;
	public int maximumPredecessors = 4;

	HashMap<String,XLog> sublogsProcedural = new HashMap <>();
	HashMap<Integer,XLog> sublogsDeclarative = new HashMap<>();

	HashMap< String, Integer > eventToGroup = new HashMap<> ( );

	public static XConceptExtension xConceptExtentionInstance	= XConceptExtension.instance();


	public void group_events () {

//		visit all start events
//		build groups of events
		for ( String event_name : successors.get ( trace_start_pseudo_name ).keySet ( ) ) {
			visit ( event_name, new HashSet<> ( ) );
		}

		print_out.println ( "Groups of structured events:" );
		print_out.println ( procedural_event_groups );
	}

	protected void visit ( String event_name, HashSet< String > value ) {
		if ( ! analyzed_events.contains ( event_name ) ) {
			analyzed_events.add ( event_name );

			if ( successors.containsKey ( event_name ) ) {

				if ( successors.get ( event_name ).size ( ) <= maximumSuccessors ) {

					for ( String succe : successors.get ( event_name ).keySet ( ) ) {

						if ( predecessors.containsKey ( succe ) || succe.equals ( trace_end_pseudo_name ) ) {

							if ( predecessors.get ( succe ).size ( ) <= maximumPredecessors ) {
								value.add ( event_name );
								value.add ( succe );
							} else {
								visit ( succe, new HashSet< String > ( ) );
							}
							//if(!succe.equals("end")){
							visit ( succe, value );
							//}
						}
					}

				} else {
					for ( String succe : successors.get ( event_name ).keySet ( ) ) {
						visit ( succe, new HashSet< String > ( ) );
					}
				}
			}

		}
		boolean found = false;

		/*
		found = ( groupAdded.intersection ( value ).size () > 0 )
		 */

		for ( String el : value ) {
			if ( groupAdded.contains ( el ) ) {
				found = true;
				break;
			}
		}
		if ( ! groupAdded.contains ( event_name ) && ! found && ! value.isEmpty ( ) ) {
			groupAdded.addAll ( value );
			procedural_event_groups.put ( groupID, value );
			groupID++;
		}
		groupAdded.add ( event_name );
	}


	public XTrace filter_trace ( XTrace trace )	{
//				Trace 1
//				s1, s1, un, s1, un2 ->
// 					structured		: [ s1, s1 ], [ s1 ]
//					unstructured	: [ un ], [ un2 ]

//				Trace 2
//				s1, s2, s1, un, un2 ->
// 					structured		: [ s1 ], [ s2 ], [ s1 ]
//					unstructured	: [ un, un2 ]

//				T1)		  s1	s1		un		s1		un2
//				id1		: [ s1, s1 ]	[		s1 ]	[]
//				id2		: [],	[] 		[]		[]		[]
//				id_un	: [],	[ 		un ]	[		un2 ]

//				T2)		  s1		s2		s1		un		un2
//				id1		: [ s1 ]	[		s1 ]	[]		[]
//				id2		: [			s2 ]	[]		[]		[]
//				id_un	: []		[]		[ 		un, 	un2 ]

		XExtendedEvent	event;
		String	event_name, event_groupName ;

		int prev_event_groupId = -1, event_groupId ;

		XTrace filtered_trace	= factory.createTrace ( trace.getAttributes () );

		Map < String, List < XTrace >  >	procedural_traces	= new HashMap<> (  );

//			fill with empty initial array
		for ( int group_id : this.procedural_event_groups.keySet () )	{

			List < XTrace > v	= new ArrayList<> ( );
			v.add ( factory.createTrace ( trace.getAttributes () ) );

			procedural_traces.put ( String.format ( "P%d.%d", level, group_id ), v );
		}


		for ( int i = 0, size = trace.size () ; i < size ; i ++ )	{
			event	= XExtendedEvent.wrap ( trace.get ( i ) );
			event_name	= fetch_name ( event );

			event_groupId	= eventToGroup.getOrDefault ( event_name, -1 );

			for ( String group_name : procedural_traces.keySet () )
				procedural_traces.get ( group_name ).add ( factory.createTrace ( trace.getAttributes () ) );

			if	( event_groupId != -1 )	{

				event_groupName	= String.format ( "P%d.%d", level, event_groupId );

				procedural_traces.get ( event_groupName ).remove ( procedural_traces.get ( event_groupName ).size ( ) - 1 );
				procedural_traces.get ( event_groupName )
//						get last item
						.get ( procedural_traces.get ( event_groupName ).size ( ) - 1 )
						.add ( ( XExtendedEvent ) event.clone () );

				if ( prev_event_groupId != event_groupId ) {

					event.setName ( event_groupName );
					filtered_trace.add ( event );
				}
			}
//				unstructured event
			else	{
				filtered_trace.add ( event );
			}

			prev_event_groupId	= event_groupId;
		}

		for ( String group_name : procedural_traces.keySet () )	{
			XTrace	v;

			for ( int i = 0, len = procedural_traces.get ( group_name ).size (); i < len ; i ++ )	{

				v	= procedural_traces.get ( group_name ).get ( i );

				if	( v.size () > 0 )	{
					this.sublogsProcedural.get ( group_name ).add ( v );
				}
			}
		}

		return filtered_trace;
	}

	public void split_log ( XLog log )	{

		XLog filtered_log = factory.createLog ( log.getAttributes () );

//		map events to procedural group ID
		for ( int group_id : procedural_event_groups.keySet ( ) ) {
			sublogsProcedural.put (
					String.format ( "P%d.%d", level, group_id ),
					factory.createLog ( log.getAttributes () )
			);

			for ( String event_name : procedural_event_groups.get ( group_id ) ) {
				eventToGroup.put ( event_name, group_id );
			}
		}

		for ( XTrace trace : log ) {
			filtered_log.add ( filter_trace ( trace ) );
		}

		print_out.println (  );
		print_out.println ( "root log" );
		print_log ( filtered_log, print_out );

		for ( String group_name : sublogsProcedural.keySet ()  )	{

			print_out.println (  );
			print_out.println ( group_name );
			print_log ( sublogsProcedural.get ( group_name ), print_out );
		}
	}

	static public void print_log ( XLog log, PrintStream out )	{
		for ( XTrace trace : log )	{
			out.println ( "Trace name : " + xConceptExtentionInstance.extractName ( trace ) );

			for ( XEvent event : trace ) {

				XExtendedEvent extended_event = XExtendedEvent.wrap ( event );
				out.println ( String.format ( "\t[ %s # %s, %s ]\t%s"
						, extended_event.getResource ( )
						, extended_event.getTransition ( )
						, extended_event.getAttributes ().get ( "time:timestamp" )
						, extended_event.getName ()
				));
			}
		}
	}

}

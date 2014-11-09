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

	HashMap< Integer, Set< String > >	groupsOfEvents	= new HashMap<> ( );

	PrintStream print_out = System.out ;

	public boolean flag;
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
//			log = slice_last_n ( log, 200 );

			print_out = new PrintStream (".\\output_mod\\output.txt");

		} catch ( Exception e ) {
			e.printStackTrace ( );
			return;
		}

		analyze_events ( log );
		group_events ();
		sx ( log );
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

	HashMap<Integer,XLog> sublogsProcedural = new HashMap <>();
	HashMap<Integer,XLog> sublogsDeclarative = new HashMap<>();

	Set< String > proceduralEvents = new HashSet<> ( );

	public static XConceptExtension xConceptExtentionInstance	= XConceptExtension.instance();


	public void group_events () {

//		visit all start events
//		build groups of events
		for ( String event_name : successors.get ( trace_start_pseudo_name ).keySet ( ) ) {
			visit ( event_name, new HashSet< String > ( ) );
		}

		for ( int groupId : groupsOfEvents.keySet ( ) ) {
			sublogsProcedural.put ( groupId, factory.createLog ( ) );
			proceduralEvents.addAll ( groupsOfEvents.get ( groupId ) );
		}

		print_out.println ( "Groups of structured events:" );
		print_out.println ( groupsOfEvents );
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
			groupsOfEvents.put ( groupID, value );
			groupID++;
		}
		groupAdded.add ( event_name );
	}

	public void sx ( XLog log )	{

		int traceId = 1;

		XLog declarativeLog = factory.createLog();
		Integer maxDecTraceSize = 0;
		int declIndex = 1;
		boolean declActive = false;
		XTrace cTrace = null;
		XEvent rootEvent = null;
		XTrace decTrace = null;
		XEvent decEvent = null;
		XEvent cEvent = null;
		XLog cLog = null;
		XLog root = factory.createLog();
		decTrace = factory.createTrace();
		XConceptExtension.instance ( ).assignName(decTrace, ""+declIndex);

		int subprocessIndex	= -1;
		int currentProcessedGroupId	= -1;

		for ( XTrace trace : log ) {

			XExtendedEvent	event, last = null;
			String	event_name = null;

			XTrace rootTrace	= factory.createTrace();

			int oldProcessedGroupId		= -1;

			boolean added	= false;


			for ( int i = 0, size = trace.size () ; i < size ; i ++ )	{
				event	= XExtendedEvent.wrap ( trace.get ( i ) );
				event_name	= fetch_name ( event );

				last = event;

				if ( proceduralEvents.contains ( event_name ) && ! added ) {
					rootEvent	= factory.createEvent ( );
					declActive	= false;

//					find group index of the event
					for ( int group : groupsOfEvents.keySet ( ) ) {
						if ( groupsOfEvents.get ( group ).contains ( event_name ) ) {
							subprocessIndex = group;
							break;
						}
					}

//					if(!added){
//						System.out.println();
//					}

					xConceptExtentionInstance.assignName ( rootEvent, "P" + level + "." + subprocessIndex );
					rootTrace.add(rootEvent);
					added = true;
				}

				if ( proceduralEvents.contains ( event_name ) && declActive ) {
					declActive = false;
					added = false;
					declarativeLog.add ( decTrace );
					if ( decTrace.size ( ) > maxDecTraceSize ) {
						maxDecTraceSize = decTrace.size ( );
					}
					declIndex++;
					decTrace = factory.createTrace ( );

					xConceptExtentionInstance.assignName ( decTrace, "" + declIndex );
				}


				boolean found = false;

				for ( int group : groupsOfEvents.keySet ( ) ) {
					if ( groupsOfEvents.get ( group ).contains ( event_name ) ) {
						currentProcessedGroupId = group;
						found = true;
//						break;
					}
				}
				if ( ! found ) {
					currentProcessedGroupId = 0;
				}

				if ( ! proceduralEvents.contains ( event_name ) ) {
					//added = false;
					declActive = true;
					decEvent = ( XEvent ) event.clone ( );
					XConceptExtension.instance ( ).assignName ( decEvent, event_name );
					decTrace.add ( decEvent );
					rootEvent = ( XEvent ) event.clone ( );
					rootTrace.add ( rootEvent );
				} else {
					declActive = false;
					if ( ( currentProcessedGroupId != oldProcessedGroupId ) && oldProcessedGroupId != - 1 ) {
						rootEvent = factory.createEvent ( );
						//	added = false;
						for ( int group : groupsOfEvents.keySet ( ) ) {
							if ( groupsOfEvents.get ( group ).contains ( event_name ) ) {
								subprocessIndex = group;
								//		added = true;
//								break;
							}
						}
						//if(!added){
						//	System.out.println();
						//	}
						XConceptExtension.instance ( ).assignName ( rootEvent, "P" + level + "." + subprocessIndex );
						rootTrace.add ( rootEvent );
						added = true;
					}
				}

				if(oldProcessedGroupId!=currentProcessedGroupId){
					if(oldProcessedGroupId!= -1 && oldProcessedGroupId!= 0){
						XTrace toAdd = (XTrace) cTrace.clone();
						XConceptExtension.instance().assignName(toAdd, traceId+"");
						traceId++;
						cLog.add(toAdd);
					}
					cTrace = factory.createTrace();
					cEvent = (XEvent) event.clone();
					cTrace.add(cEvent);
					cLog = sublogsProcedural.get(currentProcessedGroupId);
				}else{
					if(cTrace==null){
						cTrace = factory.createTrace();
					}
					cEvent = (XEvent) event.clone();
					cTrace.add(cEvent);
				}
				oldProcessedGroupId = currentProcessedGroupId;
				//	String beforeEvent;
				//	String afterEvent;
			}
			if(!declActive){
				cTrace = factory.createTrace();
				cEvent = (XEvent) last.clone();
				cTrace.add(cEvent);
				XTrace toAdd = (XTrace) cTrace.clone();
				XConceptExtension.instance().assignName(toAdd, traceId+"");
				traceId++;
				cLog.add(toAdd);
				//cLog.add(cTrace);
			}
			if(!added && !declActive){
				rootEvent = factory.createEvent();
				//	added = true;
				for(int group : groupsOfEvents.keySet()){
					if(groupsOfEvents.get(group).contains(event_name)){
						subprocessIndex = group;
						//		added = false;
					}
				}
				//				if(added){
				//					System.out.println();
				//				}
				XConceptExtension.instance().assignName(rootEvent, "P"+level+"."+subprocessIndex);
				rootTrace.add(rootEvent);
			}
			if(declActive){
				declarativeLog.add(decTrace);
				if(decTrace.size()>maxDecTraceSize){
					maxDecTraceSize = decTrace.size();
				}
			}
			xConceptExtentionInstance.assignName ( rootTrace, xConceptExtentionInstance.extractName ( trace ) );
			root.add(rootTrace);
		}

		print_out.println (  );
		print_out.println ( "root log" );
		print_log ( root, print_out );

//		print_out.println (  );
//		print_out.println ( "clog" );
//		print_log ( cLog, print_out );

		print_out.println (  );
		print_out.println ( "declarativeLog" );
		print_log ( declarativeLog, print_out );
	}

	static public void print_log ( XLog log, PrintStream out )	{
		for ( XTrace trace : log )	{
			out.println ( "Trace name : " + xConceptExtentionInstance.extractName ( trace ) );

			for ( XEvent event : trace ) {

				XExtendedEvent extended_event = XExtendedEvent.wrap ( event );
				out.println ( "\t" + extended_event.getName () );
			}
		}
	}

}

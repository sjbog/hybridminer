package hybridminer_refactor;


import ee.ut.TreeProcessor;
import ee.ut.XLogReader;
import ee.ut.XLogWriter;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.out.XSerializer;
import org.deckfour.xes.out.XesXmlGZIPSerializer;
import org.processmining.contexts.cli.CLIContext;
import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.framework.plugin.GlobalContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.plugins.causalnet.miner.settings.HeuristicsMinerSettings;
import org.processmining.plugins.causalnet.temp.aggregation.AggregationFunction;
import org.processmining.plugins.causalnet.temp.cube.EventCube;
import org.processmining.plugins.causalnet.temp.elements.Dimension;
import org.processmining.plugins.causalnet.temp.elements.Perspective;
import org.processmining.plugins.causalnet.temp.index.InvertedIndex;
import org.processmining.plugins.causalnet.temp.measures.*;
import org.processmining.plugins.heuristicsnet.AnnotatedHeuristicsNet;
import org.processmining.plugins.heuristicsnet.SimpleHeuristicsNet;
import org.processmining.plugins.heuristicsnet.miner.heuristics.converter.HeuristicsNetToFlexConverter;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.FlexibleHeuristicsMiner;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.HeuristicsMiner;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.operators.Operator;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.operators.Stats;

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

	public XSerializer	serializer	= new XesXmlGZIPSerializer ();
	public String	root_log_filename			= "root";
	public String	procedural_log_filename		= "procedural_%d_%d";
	public String	declarative_log_filename	= "declarative_%d_%d";
	public String	output_path	= "./output/";
	public static XFactory factory = XFactoryRegistry.instance( ).currentDefault();


	public LogProcessor ( String file_path ) {

		XLog log;
		this.output_path	= "./output_mod/";
		flag = true;

		try {

			log = XLogReader.openLog( file_path );
			log = XLogReader.sliceLastN( log, 200 );

			print_out = new PrintStream (".\\output_mod\\output.txt");

		} catch ( Exception e ) {
			e.printStackTrace ( );
			return;
		}

		analyze_events ( log );
//		group_events ();
//		log	= split_log ( log );

//		List< Set< String > > AND_groups = find_AND_groups (
//		find_AND_groups (
////				get root event
//				successors.get ( traceStartPseudoEvent ).keySet ( ).iterator ( ).next ( )
//				, log
//		);
//		print_out.println ( AND_groups );
//		System.out.println ( AND_groups );
		find_structure (
//				get root event
				successors.get ( trace_start_pseudo_name ).keySet ().iterator ().next ()
				, log
		);
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
		XLogInfo logInfo	= XLogInfoFactory.createLogInfo( log, TreeProcessor.defaultXEventClassifier );

		for ( XTrace trace : log ) {

			XExtendedEvent	event	= XExtendedEvent.wrap ( trace.get ( 0 ) );

			String	curr_event_name, next_event_name;

//			Add trace start pseudo event
			curr_event_name	= trace_start_pseudo_name;
			next_event_name	= TreeProcessor.fetchName( event, logInfo );
			update_event_counters ( curr_event_name, next_event_name );


			for ( int i = 1, size = trace.size () ; i < size ; i ++ )	{

				curr_event_name	= next_event_name;

				event	= XExtendedEvent.wrap ( trace.get ( i ) );
				next_event_name	= TreeProcessor.fetchName( event, logInfo );

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


	public XTrace filter_trace ( XTrace trace, XLogInfo logInfo )	{

		XTrace 	filtered_trace	= factory.createTrace ( trace.getAttributes () );
		Map < String, XTrace >	procedural_traces	= new HashMap<> (  );

		for	( String key : this.sublogsProcedural.keySet () )	{
			procedural_traces.put ( key,factory.createTrace ( trace.getAttributes () ) );
		}


		for ( int i = 0, size = trace.size (), prev_event_group_id = -1 ; i < size ; i ++ )	{

			XExtendedEvent	event		= XExtendedEvent.wrap ( trace.get ( i ) );
			String			event_name	= TreeProcessor.fetchName( event, logInfo );

			int event_group_id	= eventToGroup.getOrDefault ( event_name, -1 );

//			unstructured event
			if	( event_group_id == -1 )	{
				filtered_trace.add ( event );
			}
//			procedural / structured event
			else	{
				String event_group_name	= String.format ( "P%d.%d", level, event_group_id );

				procedural_traces.get ( event_group_name ).add ( ( XExtendedEvent ) event.clone ( ) );

				if ( prev_event_group_id != event_group_id ) {

					event.setName ( event_group_name );
					filtered_trace.add ( event );
				}
			}

			prev_event_group_id	= event_group_id;
		}

//		add non-empty procedural groups' trace to sublogsProcedural
		for ( String group_name : procedural_traces.keySet () )	{
			XTrace	procedural_group_trace	= procedural_traces.get ( group_name );

			if	( 0 < procedural_group_trace.size () )	{
				this.sublogsProcedural.get ( group_name ).add ( procedural_group_trace );
			}
		}

		return filtered_trace;
	}

	public XLog split_log ( XLog log )	{

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

		XLogInfo logInfo	= XLogInfoFactory.createLogInfo( log, TreeProcessor.defaultXEventClassifier );

		for ( XTrace trace : log ) {
			filtered_log.add ( filter_trace ( trace, logInfo ) );
		}

		print_out.println (  );
		print_out.println ( "root log" );
		XLogWriter.print( filtered_log, print_out );
		XLogWriter.saveXesGz( filtered_log, output_path + this.root_log_filename );

		for ( String group_name : sublogsProcedural.keySet ()  )	{

			print_out.println (  );
			print_out.println ( group_name );
			XLogWriter.print( sublogsProcedural.get( group_name ), print_out );
		}

		return	filtered_log;
	}


	public void find_structure ( String event_name, XLog log ) {

		GlobalContext global_context	= new CLIContext ();
		PluginContext plugin_context	= new CLIPluginContext( global_context, "test label" );

//		XLogInfo	summary_info	= XLogInfoFactory.createLogInfo ( log );

//		PluginManagerImpl.initialize ( plugin_context.getClass () );
//		PluginExecutionResult execution_result = new PluginExecutionResultImpl ( new Class<?>[] { String.class }, new String[] { "Return 1" }, null );
//
//		plugin_context.setFuture ( null );

		FlexibleHeuristicsMiner fhMiner	= new FlexibleHeuristicsMiner( plugin_context, log );
		HeuristicsMiner hMiner	= new HeuristicsMiner( plugin_context, log );

		AnnotatedHeuristicsNet annotatedHeuristicsNet = ( AnnotatedHeuristicsNet ) fhMiner.mine( );
		SimpleHeuristicsNet simpleHeuristicsNet = ( SimpleHeuristicsNet ) hMiner.mine( );

		try {
//			print_out.println( miner.mine( plugin_context, log, summary_info ).toString() );
			Object[] result = HeuristicsNetToFlexConverter.converter( plugin_context, annotatedHeuristicsNet );
			Flex flex		= ( Flex ) result[ 0 ];

			initTables( annotatedHeuristicsNet.getSplit( "13" ), annotatedHeuristicsNet.getInvertedKeys() );
			initTables( annotatedHeuristicsNet.getJoin( "13" ), annotatedHeuristicsNet.getInvertedKeys() );

			Object[] result2 = buildCausalNet( log );
			print_out.println( fhMiner.mine( ) );
			print_out.println( hMiner.mine( ) );
//			print_out.println ( result.toString ( ) );
		}
		catch ( Exception e )	{
			print_out.println ( "Exception : " + e.getMessage () );
			e.printStackTrace (  );
		}

//		.createChildContext ( "child" );
//		73                 PluginExecutionResult executionResult = alphaMiner.invoke(childContext, newLog);
//		74                 executionResult.synchronize();
//		75                 Object[] objects=executionResult.getResults();


//		for ( String group_name : sublogsProcedural.keySet ()  )	{
//
//			print_out.println (  );
//			print_out.println ( group_name );
//			print_log ( sublogsProcedural.get ( group_name ), print_out );
//		}
	}

	public void initTables(Operator op, HashMap<String, String> keys) {

		if ( op == null )	return;

		int elements_size = op.getElements().size();
		int patterns_size = op.getLearnedPatterns().size();

		// ------------------

		int sum = 0;
		ArrayList<Integer> stackC = new ArrayList<>(elements_size);
		for (int i = 0; i < elements_size; i++)
			stackC.add( 0 );

		ArrayList<String> stackP = new ArrayList<String>(patterns_size);
		ArrayList<Integer> stackV = new ArrayList<Integer>(patterns_size);

		for (java.util.Map.Entry<String, Stats > entry : op.getLearnedPatterns().entrySet()) {

			int occurrences = entry.getValue().getOccurrences();

			boolean isInserted = false;
			for (int i = 0; i < stackP.size(); i++) {

				if (occurrences > stackV.get(i)) {

					stackP.add(i, entry.getKey());
					stackV.add(i, occurrences);
					isInserted = true;
					break;
				}
			}
			if (!isInserted) {

				stackP.add(entry.getKey());
				stackV.add(occurrences);
			}

			sum += occurrences;
		}

		Boolean[][] p = new Boolean[elements_size][patterns_size];
		String[][] m = new String[2][patterns_size];

		for (int i = 0; i < stackP.size(); i++) {

			String code = stackP.get(i);
			int occurrences = stackV.get(i);

			float percentage = Math.round((float) occurrences / (float) sum
					* 10000) / 100f;

			for (int j = 0; j < elements_size; j++) {

				if (code.charAt(j) == '1') {

					p[j][i] = true;

					Integer temp = stackC.remove(j);
					temp += occurrences;
					stackC.add(j, temp);
				} else
					p[j][i] = false;
			}
			m[0][i] = " " + String.valueOf(occurrences);
			m[1][i] = percentage + "%";
		}

		// ------------------
		// connections


		ArrayList<Integer> stackI = new ArrayList<Integer>(elements_size);
		for (int i = 0; i < stackC.size(); i++) {

			int value = stackC.get(i);

			boolean isInserted = false;
			for (int j = 0; j < stackI.size(); j++) {

				int temp = stackC.get(stackI.get(j));

				if (value > temp) {

					stackI.add(j, new Integer(i));
					isInserted = true;
					break;
				}
			}
			if (!isInserted) {

				stackI.add(new Integer(i));
			}
		}

		String[][] m3 = new String[3][elements_size];

		for (int i = 0; i < stackC.size(); i++) {

			int element = op.getElements().get(stackI.get(i));
			String elementID = convertID( keys.get( String
					.valueOf( element ) ) );
			int occurrences = stackC.get(stackI.get(i));

			float percentage = Math.round((float) occurrences / (float) sum
					* 10000) / 100f;

			m3[0][i] = elementID;
			m3[1][i] = String.valueOf(occurrences);
			m3[2][i] = percentage + "%";
		}
	}

	public static String convertID(String id) {

		int index = id.indexOf("+");

		if (index == -1) {
			return id;
		}
		else {
			return id.substring(0, index) + " (" + id.substring(index + 1) + ")";
		}
	}

	public Object[] buildCausalNet( XLog log ) {
		HeuristicsMinerSettings settings = new HeuristicsMinerSettings( );
		InvertedIndex index = new InvertedIndex( );
		index.createCompleteIndex(log);

		//CREATING DEFAULT PERSPECTIVE
		Perspective perspective;

		LinkedList<Dimension > modelDimensions = new LinkedList<Dimension>();
		LinkedList<Pair<Measure, AggregationFunction<?>> > measures = new LinkedList<Pair<Measure, AggregationFunction<?>>>();

		Dimension mainDim = index.getDimension("Event:Name");
		if (mainDim != null) {
			Dimension d1 = mainDim.instance();
			d1.addValues(mainDim.getValues());
			modelDimensions.add(d1);
		}

		Dimension secondDim = index.getDimension("Event:Type");
		if(secondDim != null && secondDim.getCardinality() > 1){

			Dimension d2 = secondDim.instance();
			d2.addValues(secondDim.getValues());
			modelDimensions.add(d2);
		}

		measures.add(new Pair<Measure, AggregationFunction<?>>(new EventEntry(), null));
		measures.add(new Pair<Measure, AggregationFunction<?>>(new InstanceEntry(), null));
		measures.add(new Pair<Measure, AggregationFunction<?>>(new EventStart(), null));
		measures.add(new Pair<Measure, AggregationFunction<?>>(new EventEnd(), null));
		measures.add(new Pair<Measure, AggregationFunction<?>>(new EventDirectSuccessor(), null));
		measures.add(new Pair<Measure, AggregationFunction<?>>(new EventIndirectSuccessor(), null));
		measures.add(new Pair<Measure, AggregationFunction<?>>(new EventDirectDependencyMeasure(), null));
		measures.add(new Pair<Measure, AggregationFunction<?>>(new EventLenghtTwoDependencyMeasure(), null));
		measures.add(new Pair<Measure, AggregationFunction<?>>(new EventLongDistanceDependencyMeasure(), null));
		measures.add(new Pair<Measure, AggregationFunction<?>>(new EventLengthTwoLoop(), null));

		perspective =
				new Perspective(modelDimensions, new LinkedList<Dimension>(), measures);

		//BUILDING EVENT CUBE
		EventCube cube;

		cube = new EventCube(index, perspective.getFirstSpace(), perspective.getMeasures());
		cube.processValues(perspective);

		//COMPUTING CAUSAL NET
		String logID = XConceptExtension.instance().extractName(log);

		Object[] cnet = cube.computeCausalNet(logID, perspective, settings);
		Flex flexDiagram = (Flex) cnet[0];

//		context.getFutureResult(0).setLabel(flexDiagram.getLabel());
//		context.getFutureResult(1).setLabel("Start tasks node of " + flexDiagram.getLabel());
//		context.getFutureResult(2).setLabel("End tasks node of " + flexDiagram.getLabel());
//		context.getFutureResult(3).setLabel("Annotations of " + flexDiagram.getLabel());
//
//		context.addConnection(new FlexStartTaskNodeConnection("Start tasks node of " + flexDiagram.getLabel() + " connection", flexDiagram, (StartTaskNodesSet ) cnet[1]));
//		context.addConnection(new FlexEndTaskNodeConnection("End tasks node of " + flexDiagram.getLabel() + " connection", flexDiagram, (EndTaskNodesSet ) cnet[2]));
//		context.addConnection(new CausalNetAnnotationsConnection("Annotations of " + flexDiagram.getLabel()  + " connection", flexDiagram, (CausalNetAnnotations ) cnet[3]));

		return cnet;
	}
}

package ee.ut.classifiers;

import ee.ut.XLogReader;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.heuristicsnet.AnnotatedHeuristicsNet;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.FlexibleHeuristicsMiner;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.HeuristicsMiner;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.operators.Join;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.operators.Stats;
import org.processmining.plugins.heuristicsnet.miner.heuristics.miner.settings.HeuristicsMinerSettings;

import java.util.*;

public class HeuristicEventsClassifier implements EventsClassifier {

	public HeuristicsMinerSettings	hmSettings;
	public HeuristicsMiner	hMiner;
	public PluginContext	pluginContext;

	public HeuristicEventsClassifier( XEventClassifier xEventClassifier, PluginContext pluginContext ) {
		hmSettings	= new HeuristicsMinerSettings( );
		hmSettings.setClassifier( xEventClassifier );
		this.pluginContext	= pluginContext;
	}

	public HeuristicEventsClassifier( HeuristicsMinerSettings hmSettings, PluginContext pluginContext ) {
		this.hmSettings	= hmSettings;
		this.pluginContext	= pluginContext;
	}

	public Map< String, Set< Integer > > mapEventsToBranches(
			XLog log
			, Map< String, Set< Integer > > eventToBranch
			, Set< String > unknownBranchEvents
	) {
		Set< String > events	= new HashSet<>( eventToBranch.keySet() );
		events.addAll( unknownBranchEvents );

		hMiner	= new FlexibleHeuristicsMiner(
				pluginContext
				, XLogReader.filterByEvents( log, events )
				, hmSettings
		);
		HashMap< String, Set< Integer > > result = new HashMap<>( );

		Map< String, String > dependencyMap = buildDependencyMap( hMiner, eventToBranch, unknownBranchEvents );

//		bubble resolve method
		boolean foundMatch	= true;

		while( dependencyMap.size() > 0 && foundMatch ) {
			foundMatch	= false;

			for ( String event : new HashSet<>( dependencyMap.keySet() ) ) {

				if ( eventToBranch.containsKey( dependencyMap.get( event ) ) ) {
					result.put(
							event
							, eventToBranch.get( dependencyMap.get( event ) )
					);
					foundMatch	= true;
					dependencyMap.remove( event );
				}
				else if ( result.containsKey( dependencyMap.get( event ) ) ) {
					result.put(
							event
							, result.get( dependencyMap.get( event ) )
					);
					foundMatch	= true;
					dependencyMap.remove( event );
				}
			}
		}
		return result;
	}

	public static Map< String, String > buildDependencyMap(
			HeuristicsMiner	hMiner
			, Map< String, Set< Integer > > eventToBranch
			, Set< String > unknownBranchEvents
	) {
		AnnotatedHeuristicsNet annotatedHeuristicsNet = ( AnnotatedHeuristicsNet ) hMiner.mine( );
		Map< String, String > dependencyMap	= new HashMap<>(  );

		for ( String event : unknownBranchEvents ) {
			Join join = annotatedHeuristicsNet.getJoin( annotatedHeuristicsNet.getKey( event ).toString() );

			HashMap<Integer, Integer> elementOccurrences	= new HashMap<>(  );
			for ( int i = 0, size = join.getElements().size(); i < size; i ++ )
				elementOccurrences.put( join.getElements().get( i ), 0 );

//			Each pattern is a "binary" string "101001" of elements' presence
			for ( Map.Entry< String, Stats > entry : join.getLearnedPatterns().entrySet() ) {
				for ( int i = 0, element, size = entry.getKey().length() ; i < size ; i ++ ) {
					if ( entry.getKey().charAt( i ) == '1' ) {
						element = join.getElements( ).get( i );
						elementOccurrences.put( element,
								elementOccurrences.get( element ) + entry.getValue( ).getDistinctOccurrences( )
						);
					}
				}
			}

			PriorityQueue<Map.Entry< Integer, Integer>> priorityQueue	= new PriorityQueue<>( elementOccurrences.size(),
					new Comparator<Map.Entry<Integer, Integer>>() {
						public int compare( Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2 ) {
							return (o2.getValue()).compareTo( o1.getValue() );
						}
					}
			);
			priorityQueue.addAll( elementOccurrences.entrySet( ) );

			dependencyMap.put(
					event
					, annotatedHeuristicsNet.getInvertedKeys( ).get(
							priorityQueue.poll( ).getKey( ).toString()
					)
			);
		}
		return dependencyMap;
	}
}

package hybridminer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;


public class FindItemSets {
	Map<String, Integer> activityIntegerMap;
	Map<Integer, String> integerActivityMap;
	List<SortedSet<Integer>> encodedTracesList;
	boolean considerNegation;
	Map<String, Set<Integer>> activityTraceIndicesSetMap;
	Set<Integer> traceIndicesSet;
	

	public Map<Set<String>, Float> findItemSets(XLog log, int itemSetSize, float minSupportPercentage, boolean considerNegation){
		this.considerNegation = considerNegation;
		System.out.println("Calling Encode Log");
		encodeLog(log, considerNegation);
		int noTraces = encodedTracesList.size();

		//		int minNoInstances = (int)Math.round(minSupport*encodedTracesList.size());
		String[] args = new String[2];
		args[0]="-F"+System.getProperty("java.io.tmpdir")+System.getProperty("file.separator")+"DeclareApriori.txt";
		args[1]="-S"+minSupportPercentage;

		TotalSupportTree newAprioriT = new TotalSupportTree(args);
		newAprioriT.setMaxNoLevels(itemSetSize);

		// Read data to be mined from file

		newAprioriT.inputDataSet();
		newAprioriT.createTotalSupportTree();

		//		newAprioriT.outputFrequentSets();


		Map<Set<Integer>, Integer> encodedFrequentItemSetCountMap = newAprioriT.getFrequentSets();
		//		System.out.println("Frequent Item Sets;");
		//		for(Set<Integer> encodedFrequentItemSet : encodedFrequentItemSetCountMap.keySet())
		//			System.out.println(encodedFrequentItemSet+" @ "+encodedFrequentItemSetCountMap.get(encodedFrequentItemSet));

		Map<Set<String>, Float> frequentItemSetSupportMap = new HashMap<Set<String>, Float>();
		Set<String> frequentItemSet;
		float frequentItemSetSupport;
		for(Set<Integer> encodedFrequentItemSet : encodedFrequentItemSetCountMap.keySet()){
			if(encodedFrequentItemSet.size() != itemSetSize)
				continue;
			frequentItemSet = new HashSet<String>();
			for(Integer encodedActivity : encodedFrequentItemSet){
				frequentItemSet.add(integerActivityMap.get(encodedActivity));
			}
			//			System.out.println("F: "+frequentItemSet);

			frequentItemSetSupport = encodedFrequentItemSetCountMap.get(encodedFrequentItemSet)*100.0f/noTraces;
			frequentItemSetSupportMap.put(frequentItemSet, frequentItemSetSupport);
		}

				


		//	noTraces = encodedTracesList.size();
		//	for(String activity : activityTraceIndicesSetMap.keySet()){
		//		System.out.println(activity+" @ "+100*(activityTraceIndicesSetMap.get(activity).size()/(noTraces*1.0)));
		//	}

		//if(input != null && input.getAprioriKnowledgeBasedCriteriaSet().contains(AprioriKnowledgeBasedCriteria.Diversity)){
		//	return pruneDiversity(frequentItemSetSupportMap); 
		//}else{
			System.out.println("No. ItemSets: "+frequentItemSetSupportMap.size());
			for(Set<String> freqItemSet : frequentItemSetSupportMap.keySet()){
				System.out.println(freqItemSet+" @ "+frequentItemSetSupportMap.get(freqItemSet));
			
			}
			return frequentItemSetSupportMap;
	//	}
	}
	
	private Map<Set<String>, Float> pruneDiversity(Map<Set<String>, Float> frequentItemSetSupportMap){
		Map<Set<String>, Float> prunedFrequentItemSetSupportMap = new HashMap<Set<String>, Float>();
		
		StandardModel[] lifeCycleTransitions = StandardModel.values();
		
		Set<String> itemSetWithoutEventTypes = new HashSet<String>();
		for(Set<String> frequentItemSet : frequentItemSetSupportMap.keySet()){
			itemSetWithoutEventTypes.clear();
			for(String item : frequentItemSet){
				int index = -1;
				for(StandardModel transition : lifeCycleTransitions){
					if(item.toLowerCase().contains("-"+transition.toString().toLowerCase())){
						index = item.toLowerCase().indexOf("-"+transition.toString().toLowerCase());
						break;
					}
				}
				if(index != -1)
					itemSetWithoutEventTypes.add(item.substring(0, index));
				else
					itemSetWithoutEventTypes.add(item);
			}
			if(itemSetWithoutEventTypes.size() == frequentItemSet.size())
				prunedFrequentItemSetSupportMap.put(frequentItemSet, frequentItemSetSupportMap.get(frequentItemSet));
		}
		
		System.out.println("Before Diversity Item Set Size: "+frequentItemSetSupportMap.size());
		System.out.println("After Diversity Item Set Size: "+prunedFrequentItemSetSupportMap.size());
		
		System.out.println("No. ItemSets: "+prunedFrequentItemSetSupportMap.size());
		for(Set<String> freqItemSet : prunedFrequentItemSetSupportMap.keySet()){
			System.out.println(freqItemSet+" @ "+prunedFrequentItemSetSupportMap.get(freqItemSet));
		
		}
		
		return prunedFrequentItemSetSupportMap;
	}

	private void encodeLog(XLog log,  boolean considerNegation){
		int activityCount = 1;
		activityIntegerMap = new HashMap<String, Integer>();
		integerActivityMap = new HashMap<Integer, String>();
		activityTraceIndicesSetMap = new HashMap<String, Set<Integer>>();
		traceIndicesSet = new HashSet<Integer>();
		
		System.out.println("Consider Negation: "+considerNegation);


		String activity, negationActivity;
		XAttributeMap eventAttributeMap;
		Set<String> activitySet = new HashSet<String>();

		boolean isConsiderEventTypes = true;
	//	if(input != null){
		//	System.out.println("JC: "+input.getAprioriKnowledgeBasedCriteriaSet());
		//	isConsiderEventTypes = input.getAprioriKnowledgeBasedCriteriaSet().contains(AprioriKnowledgeBasedCriteria.AllActivitiesWithEventTypes);
	//	}
		
		System.out.println("Is Consider Event Types: "+isConsiderEventTypes);
		
		for(XTrace trace : log){
			for(XEvent event : trace){
				eventAttributeMap = event.getAttributes();
				if(isConsiderEventTypes){
					if(eventAttributeMap.get(XLifecycleExtension.KEY_TRANSITION)!=null){
						activity = XConceptExtension.instance().extractName(event)+"-"+eventAttributeMap.get(XLifecycleExtension.KEY_TRANSITION);
					}else{
						activity = XConceptExtension.instance().extractName(event);
					}
				}else{
					activity = XConceptExtension.instance().extractName(event);
				}


				activitySet.add(activity);
				if(considerNegation){
					negationActivity = "NOT-"+activity;
					if(!activityIntegerMap.containsKey(negationActivity)){
						activityIntegerMap.put(negationActivity, activityCount);
						integerActivityMap.put(activityCount, negationActivity);
						activityCount++;
					}
				}

				if(!activityIntegerMap.containsKey(activity)){
					activityIntegerMap.put(activity, activityCount);
					integerActivityMap.put(activityCount, activity);
					activityCount++;
				}


			}
		}

		//		System.out.println("No. Activities: "+activityIntegerMap.size());
		//		System.out.println(activityIntegerMap.keySet());


		encodedTracesList = new ArrayList<SortedSet<Integer>>();
		SortedSet<Integer> encodedTrace;
		Set<String> currentTraceActivitySet = new HashSet<String>();
		Set<String> currentTraceNegativeActivitySet = new HashSet<String>();
		Set<String> currentTraceNegationActivitySet = new HashSet<String>();
		int traceIndex = 0;
		Set<Integer> activityTraceIndicesSet;
		for(XTrace trace : log){
			encodedTrace = new TreeSet<Integer>();
			currentTraceActivitySet.clear();
			currentTraceNegationActivitySet.clear();
			currentTraceNegativeActivitySet.clear();
			for(XEvent event : trace){
				eventAttributeMap = event.getAttributes();
				
				if(isConsiderEventTypes){
					if(eventAttributeMap.get(XLifecycleExtension.KEY_TRANSITION)!=null){
						activity = XConceptExtension.instance().extractName(event)+"-"+eventAttributeMap.get(XLifecycleExtension.KEY_TRANSITION);
					}else{
						activity = XConceptExtension.instance().extractName(event);
					}
				}else{
					activity = XConceptExtension.instance().extractName(event);
				}
				
				
				if(isConsiderEventTypes){
					currentTraceActivitySet.add(activity);
					encodedTrace.add(activityIntegerMap.get(activity));
				}else{
					currentTraceActivitySet.add(activity);
					encodedTrace.add(activityIntegerMap.get(activity));
				}
			}

			for(String act : currentTraceActivitySet){
				if(activityTraceIndicesSetMap.containsKey(act))
					activityTraceIndicesSet = activityTraceIndicesSetMap.get(act);
				else
					activityTraceIndicesSet = new HashSet<Integer>();
				activityTraceIndicesSet.add(traceIndex);
				activityTraceIndicesSetMap.put(act, activityTraceIndicesSet);
			}



			if(considerNegation){
				currentTraceNegationActivitySet.addAll(activitySet);
				currentTraceNegationActivitySet.removeAll(currentTraceActivitySet);

				for(String negActivity : currentTraceNegationActivitySet){
					if(considerNegation)
						encodedTrace.add(activityIntegerMap.get("NOT-"+negActivity));
					currentTraceNegativeActivitySet.add("NOT-"+negActivity);
				}
			}

//			System.out.println("Trace: "+traceIndex+" @ "+currentTraceActivitySet+" @ "+currentTraceNegativeActivitySet);



			for(String act : currentTraceNegativeActivitySet){
				if(activityTraceIndicesSetMap.containsKey(act))
					activityTraceIndicesSet = activityTraceIndicesSetMap.get(act);
				else
					activityTraceIndicesSet = new HashSet<Integer>();
				activityTraceIndicesSet.add(traceIndex);
				activityTraceIndicesSetMap.put(act, activityTraceIndicesSet);
			}


			encodedTracesList.add(encodedTrace);
			traceIndicesSet.add(traceIndex);
			traceIndex++;
		}

		String tempDir = System.getProperty("java.io.tmpdir");
		writeToFile(tempDir, "DeclareApriori.txt", encodedTracesList);

		FileIO io = new FileIO();
		io.writeToFile(tempDir, "ActivityTraceIndicesSetMap.txt", activityTraceIndicesSetMap, " @ ");
		io.writeToFile(tempDir, "IntegerActivityMap.txt", integerActivityMap, " @ ");

		//		System.out.println("No. Distinct Traces: "+encodedTracesList.size());
	}

	public float getSupport(String activity){
		int noTraces = encodedTracesList.size();
		int noSupportingTraces = 0;
		if(activityTraceIndicesSetMap.containsKey(activity))
			noSupportingTraces = activityTraceIndicesSetMap.get(activity).size();
		return (noSupportingTraces*100.0f)/noTraces;
	}

	public Set<Integer> getSupportingTraceIndices(String activity){
		if(activityTraceIndicesSetMap.containsKey(activity))
			return activityTraceIndicesSetMap.get(activity);
		else
			return null;
	}

	public float getSupport(String actA, String actB){
		int noTraces = encodedTracesList.size();
		int noSupportingTraces = 0;



		Set<Integer> actASupportingTraceIndicesSet = new HashSet<Integer>();
		Set<Integer> actBSupportingTraceIndicesSet = new HashSet<Integer>();

		if(activityTraceIndicesSetMap.containsKey(actA))
			actASupportingTraceIndicesSet.addAll(activityTraceIndicesSetMap.get(actA));
		else if(actA.startsWith("NOT-") && !activityTraceIndicesSetMap.containsKey(actA.replaceAll("NOT-", ""))){
			actASupportingTraceIndicesSet.addAll(traceIndicesSet);
		}
		if(activityTraceIndicesSetMap.containsKey(actB))
			actBSupportingTraceIndicesSet.addAll(activityTraceIndicesSetMap.get(actB));
		else if(actB.startsWith("NOT-") && !activityTraceIndicesSetMap.containsKey(actB.replaceAll("NOT-", ""))){
			actBSupportingTraceIndicesSet.addAll(traceIndicesSet);
		}

		System.out.println("Act A: "+actASupportingTraceIndicesSet.size());
		System.out.println("Act B: "+actBSupportingTraceIndicesSet.size());
		actASupportingTraceIndicesSet.retainAll(actBSupportingTraceIndicesSet);
		noSupportingTraces = actASupportingTraceIndicesSet.size();

		float support = (noSupportingTraces*100.0f)/noTraces;
		System.out.println("getSupport: "+actA+","+actB+": "+support);
		return support;
	}

	public Set<Integer> getSupportingTraceIndices(String actA, String actB){
		Set<Integer> actASupportingTraceIndicesSet = new HashSet<Integer>();
		Set<Integer> actBSupportingTraceIndicesSet = new HashSet<Integer>();

		if(activityTraceIndicesSetMap.containsKey(actA))
			actASupportingTraceIndicesSet.addAll(activityTraceIndicesSetMap.get(actA));
		else if(actA.startsWith("NOT-") && !activityTraceIndicesSetMap.containsKey(actA.replaceAll("NOT-", ""))){
			actASupportingTraceIndicesSet.addAll(traceIndicesSet);
		}
		if(activityTraceIndicesSetMap.containsKey(actB))
			actBSupportingTraceIndicesSet.addAll(activityTraceIndicesSetMap.get(actB));
		else if(actB.startsWith("NOT-") && !activityTraceIndicesSetMap.containsKey(actB.replaceAll("NOT-", ""))){
			actBSupportingTraceIndicesSet.addAll(traceIndicesSet);
		}

		actASupportingTraceIndicesSet.retainAll(actBSupportingTraceIndicesSet);
		return actASupportingTraceIndicesSet;
	}

	private void writeToFile(String dir, String fileName,
			List<SortedSet<Integer>> encodedTracesSet) {
		FileOutputStream fos;
		PrintStream ps;

		if (isDirExists(dir)) {
			try {
				fos = new FileOutputStream(dir + System.getProperty("file.separator")  + fileName);
				ps = new PrintStream(fos);

				for (SortedSet<Integer> encodedTrace : encodedTracesSet) {
					Iterator<Integer> it = encodedTrace.iterator();
					while(it.hasNext()){
						ps.print(it.next().toString());
						if(it.hasNext())
							ps.print(" ");
					}
					ps.println();
				}

				ps.close();
				fos.close();
			} catch (FileNotFoundException e) {
				System.err
				.println("File Not Found Exception while creating file: "
						+ dir + System.getProperty("file.separator")  + fileName);
				System.exit(0);
			} catch (IOException e) {
				System.err.println("IO Exception while writing file: " + dir
						+ System.getProperty("file.separator")  + fileName);
				System.exit(0);
			}
		} else {
			System.err.println("Can't create Directory: " + dir);
		}
	}

	private boolean isDirExists(String dir) {
		if (!(new File(dir)).exists()) {
			return new File(dir).mkdirs();
		} else {
			return true;
		}
	}
}

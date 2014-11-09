package hybridminer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;





public class SequenceMiner {
	
	
	
	
	public static StringSequenceDatabaseMaxSP convertIntoStringSequenceDatabaseMaxSP(XLog log){
		
		Map<String, Integer> alphabetMap = new HashMap<String, Integer>();
		SequenceDatabase sequenceDB = new SequenceDatabase();
		int traceId = 0;
		for (XTrace trace : log) {
			Sequence sequence = new Sequence(traceId);

			for (XEvent event : trace) {
				String eventLabel = XConceptExtension.instance().extractName(event);
				Integer index = alphabetMap.get(eventLabel);
				if (index==null){
					index = alphabetMap.size();
					alphabetMap.put(eventLabel, index);
				}
				List<Integer> itemset = new ArrayList<Integer>();
				itemset.add(index);
				sequence.addItemset(itemset);			
			}
			sequenceDB.addSequence(sequence);
			System.out.println(sequence);
			traceId++;
		}
		StringSequenceDatabaseMaxSP sSequenceDB  = new StringSequenceDatabaseMaxSP(alphabetMap, sequenceDB);
		return sSequenceDB;
	}
	
	
	
}

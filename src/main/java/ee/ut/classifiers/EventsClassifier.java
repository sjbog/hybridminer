package ee.ut.classifiers;

import org.deckfour.xes.model.XLog;

import java.util.Map;
import java.util.Set;

public interface EventsClassifier {
	public Map< String, Set< Integer > >	mapEventsToBranches(
			XLog log
			, Map< String, Set< Integer > >	eventToBranch
			, Set< String >	unknownBranchEvents
	);
}

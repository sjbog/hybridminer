package hybridminer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class StringSequenceDatabaseMaxSP {

	private Map<String, Integer> alphabetMap;
	private SequenceDatabase sequenceDB;
	
	
	public StringSequenceDatabaseMaxSP() {
		alphabetMap = new HashMap<String, Integer>();
		sequenceDB = null;
	}


	public StringSequenceDatabaseMaxSP(Map<String, Integer> alphabetMap, SequenceDatabase sequenceDB) {
		this.alphabetMap = alphabetMap;
		this.sequenceDB = sequenceDB;
	}


	public Map<String, Integer> getAlphabetMap() {
		return alphabetMap;
	}


	public SequenceDatabase getSequenceDB() {
		return sequenceDB;
	}
	
	public String getMappedString(int index){
		String searchedString = null;
		for (Iterator iterator = alphabetMap.keySet().iterator(); iterator.hasNext() && searchedString==null;) {
			String mappedString = (String) iterator.next();
			Integer mappedIndex = alphabetMap.get(mappedString);
			if (mappedIndex.intValue()==index)
				searchedString = mappedString;
			
		}
		return searchedString;
	}
	
	
	

}

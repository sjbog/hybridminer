package hybridminer_old;

/* ---------------------------------------------------------------------- */
/*                                                                        */

/* APRIORI-T SORTED AND PRUNED APPLICATION */
/*                                                                        */
/* Frans Coenen */
/*                                                                        */
/* 11 July 2002 */
/*                                                                        */
/* Department of Computer Science */
/* The University of Liverpool */
/*                                                                        */
/* ---------------------------------------------------------------------- */

import java.io.IOException;

public class AprioriTsortedPrunedApp {

	// ------------------- FIELDS ------------------------

	// None

	// ---------------- CONSTRUCTORS ---------------------

	// None

	// ------------------ METHODS ------------------------

	public static void main(String[] args) throws IOException {

		// Create instance of class TotalSupportTree

		TotalSupportTree newAprioriT = new TotalSupportTree(args);

		// Read data to be mined from file

		newAprioriT.inputDataSet();

		// Reorder and prune input data according to frequency of single 
		// attributes

		newAprioriT.idInputDataOrdering();
		newAprioriT.recastInputDataAndPruneUnsupportedAtts();
		newAprioriT.setNumOneItemSets();
		//newAprioriT.outputDataArray();	

		// Mine data and produce T-tree

		double time1 = System.currentTimeMillis();
		newAprioriT.createTotalSupportTree();
		newAprioriT.outputDuration(time1, System.currentTimeMillis());

		// Output 

		//newAprioriT.outputTtree();	
		newAprioriT.outputNumFreqSets();
		newAprioriT.outputNumUpdates();
		newAprioriT.outputStorage();
		newAprioriT.outputFrequentSets();

		// Generate ARS

		newAprioriT.generateARs();
		newAprioriT.getCurrentRuleListObject().outputNumRules();
		newAprioriT.getCurrentRuleListObject().outputRulesWithReconversion();

		// End
		System.exit(0);
	}
}

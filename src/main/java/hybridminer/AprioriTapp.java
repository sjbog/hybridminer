package hybridminer;

/* ----------------------------------------------------------- */
/*                                                             */
/* APRIORI-T APPLICATION */
/*                                                             */
/* Frans Coenen */
/*                                                             */
/* 12 July 2002 */
/*                                                             */
/* Department of Computer Science */
/* The University of Liverpool */
/*                                                             */
/* ----------------------------------------------------------- */

import java.io.IOException;

public class AprioriTapp {

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

		// Mine data and produce T-tree

		double time1 = System.currentTimeMillis();
		newAprioriT.createTotalSupportTree();
		newAprioriT.outputDuration(time1, System.currentTimeMillis());

		// Output 
		newAprioriT.outputTtree();
		newAprioriT.outputNumFreqSets();
		newAprioriT.outputNumUpdates();
		newAprioriT.outputStorage();
		newAprioriT.outputFrequentSets();

		// Generate ARs

		newAprioriT.generateARs();
		newAprioriT.getCurrentRuleListObject().outputRules();

		// End
		System.exit(0);
	}
}

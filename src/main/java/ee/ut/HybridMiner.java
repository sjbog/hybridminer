package ee.ut;

import org.deckfour.xes.model.XLog;

import java.io.PrintStream;

public class HybridMiner {

	public static void main ( String args[] ) {
		String	outputPath	= "./output/";
		PrintStream	printStream;
		XLog log;

		try {
			log = XLogReader.openLog ( "data/L1.mxml" );
//			log = XLogReader.openLog ( "data/financial_log.mxml.gz" );

//			log = XLogReader.sliceLastN( log, 200 );

			printStream	= new PrintStream( outputPath + "output.txt" );

		} catch ( Exception e ) {
			System.out.println( "Exception : " + e.getMessage( ) );
			e.printStackTrace( );
			return;
		}

		TreeProcessor treeProcessor = new TreeProcessor( log, printStream );
		treeProcessor.mine( );

		for ( String name : treeProcessor.sublogs.keySet() )
			XLogWriter.saveXesGz( treeProcessor.sublogs.get( name ), outputPath + name );

		XLogWriter.saveXesGz( treeProcessor.log, outputPath + "root" );
	}
}

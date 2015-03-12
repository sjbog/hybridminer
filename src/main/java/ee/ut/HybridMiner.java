package ee.ut;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;
import org.processmining.processtree.ProcessTree;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

public class HybridMiner {

	public static void main ( String args[] ) {
		String	outputPath	= "./output/";
		PrintStream	printStream;
		XLog log;

		try {
			if ( args.length == 0 )
				log = XLogReader.openLog ( "data/L1.mxml" );
			else
				log = XLogReader.openLog ( args[ 0 ] );

//			log = XLogReader.openLog ( "data/provaH.xes" );
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

		XLogWriter.saveXesGz( treeProcessor.log, outputPath + "root" );
		for ( Map.Entry< String, XLog > entry : treeProcessor.getChildLogs().entrySet() )
			XLogWriter.saveXesGz( entry.getValue(), outputPath + entry.getKey() );

		printStream.println( treeProcessor.toProcessTree() );

		BPMNDiagram bpmn = ( BPMNDiagram ) new ProcessTree2BPMNConverter( ).convertToBPMN(
				treeProcessor.toProcessTree( ), true
		)[ 0 ];
		try {
			new BpmnExportPlugin().export( treeProcessor.pluginContext, bpmn, new File( outputPath + "model.bpmn" ) );
		} catch ( IOException e ) {
			printStream.println( e.getMessage( ) );
//			e.printStackTrace( );
		}
	}

	@Plugin (name = "Mine process tree with Hybrid Miner", returnLabels = { "Process Tree" }, returnTypes = { ProcessTree.class }, parameterLabels = { "Log" }, userAccessible = true)
	@UITopiaVariant (affiliation = UITopiaVariant.EHV, author = "Bogdan Sem", email = "bogdan89@ut.ee")
	@PluginVariant (variantLabel = "Mine a Process Tree, dialog", requiredParameterLabels = { 0 })
	public ProcessTree mineGuiProcessTree(PluginContext context, XLog log) {
		TreeProcessor treeProcessor = new TreeProcessor( XLogReader.deepcopy( log ), System.out );
		treeProcessor.pluginContext	= context;
		treeProcessor.mine( );
		return treeProcessor.toProcessTree( );
	}
}

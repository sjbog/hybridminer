import ee.ut.LogProcessor;
import ee.ut.ProcessTree2BPMNConverter;
import ee.ut.XLogReader;
import ee.ut.XLogWriter;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;
import org.processmining.processtree.ProcessTree;

import java.io.PrintStream;
import java.util.Map;

public class Main {

	public static void main ( String args[] ) {
		String	outputPath	= "./output/";
		PrintStream	printStream;
		XLog log;

		try {
			if ( args.length == 0 )
				log = XLogReader.openLog( "data/L1.mxml" );
//				log = XLogReader.openLog ( "data/s12.mxml" );
			else
				log = XLogReader.openLog ( args[ 0 ] );

			printStream	= new PrintStream( outputPath + "output.txt" );

		} catch ( Exception e ) {
			System.out.println( "Exception : " + e.getMessage( ) );
//			e.printStackTrace( );
			return;
		}

		LogProcessor logProcessor = new LogProcessor( log, printStream );
		logProcessor.mine( );

		XLogWriter.saveXesGz( logProcessor.log, outputPath + "root" );
		for ( Map.Entry< String, XLog > entry : logProcessor.getChildLogs().entrySet() )
			XLogWriter.saveXesGz( entry.getValue(), outputPath + entry.getKey() );

		printStream.println( "\nProcess tree:" );
		printStream.println( logProcessor.toProcessTree() );

		BPMNDiagram bpmn = ( BPMNDiagram ) new ProcessTree2BPMNConverter( ).convertToBPMN(
				logProcessor.toProcessTree( ), true
		)[ 0 ];
		try {
			new BpmnExportPlugin().export( logProcessor.pluginContext, bpmn, new java.io.File( outputPath + "model.bpmn" ) );
		} catch ( Exception e ) {
			printStream.println( "Exception : " + e.getMessage( ) );
//			e.printStackTrace( );
		}
	}

	@Plugin (name = "Mine process tree with Hybrid Miner", returnLabels = { "Process Tree" }, returnTypes = { ProcessTree.class }, parameterLabels = { "Log" }, userAccessible = true)
	@UITopiaVariant (affiliation = UITopiaVariant.EHV, author = "Bogdan S", email = "bogdan89@ut.ee")
	@PluginVariant (variantLabel = "Mine a Process Tree, dialog", requiredParameterLabels = { 0 })
	public ProcessTree mineGuiProcessTree(PluginContext context, XLog log) {
		LogProcessor logProcessor = new LogProcessor( XLogReader.deepcopy( log ), System.out );
		logProcessor.pluginContext	= context;
		logProcessor.mine( );
		return logProcessor.toProcessTree( );
	}
}

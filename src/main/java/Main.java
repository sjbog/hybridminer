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

import java.io.File;
import java.io.PrintStream;
import java.util.Map;

public class Main {

	public static void main ( String args[] ) {
		String	outputPath	= "./output/";
		PrintStream	printStream;
		XLog log;

		try {
			if ( args.length == 0 )
				log = XLogReader.openLog( "data/l1.mxml" );
//				log = XLogReader.openLog ( "data/s1_py.xes" );
//				log = XLogReader.openLog ( "data/s2_wo_prefix_events_py.xes" );
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

		ProcessTree pt = logProcessor.toProcessTree( );
		printStream.println( "\nProcess tree:" + pt.toString() );
		System.out.println( "\nProcess tree:" + pt.toString( ) );

		BPMNDiagram bpmn = ( BPMNDiagram ) new ProcessTree2BPMNConverter( ).convertToBPMN( pt, false )[ 0 ];
		try {
			new BpmnExportPlugin().export(
					logProcessor.pluginContext
					, bpmn, new File( outputPath + "model.bpmn" )
			);
		} catch ( Exception e ) {
			System.out.println( "Exception : " + e.getMessage( ) );
			printStream.println( "Exception : " + e.getMessage( ) );
//			e.printStackTrace( );
		}
		System.out.flush();
		printStream.flush();
		printStream.close();
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

	@Plugin (name = "Mine BPMN with Hybrid Miner", returnLabels = { "Process Tree" }, returnTypes = { BPMNDiagram.class }, parameterLabels = { "Log" }, userAccessible = true)
	@UITopiaVariant (affiliation = UITopiaVariant.EHV, author = "Bogdan S", email = "bogdan89@ut.ee")
	@PluginVariant (variantLabel = "Mine a BPMN, dialog", requiredParameterLabels = { 0 })
	public BPMNDiagram mineGuiBPMN(PluginContext context, XLog log) {
		LogProcessor logProcessor = new LogProcessor( XLogReader.deepcopy( log ), System.out );
		logProcessor.pluginContext	= context;
		logProcessor.mine( );
		return ( BPMNDiagram ) new ProcessTree2BPMNConverter( ).convertToBPMN( logProcessor.toProcessTree( ), false )[ 0 ];
	}
}

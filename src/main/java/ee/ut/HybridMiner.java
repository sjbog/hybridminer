package ee.ut;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;
import org.processmining.processtree.ProcessTree;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HybridMiner {

	public static void main ( String args[] ) {
		String	outputPath	= "./output/";
		PrintStream	printStream;
		XLog log;

		try {
			if ( args.length == 0 )
				log = XLogReader.openLog ( "data/L1.mxml" );
//				log = XLogReader.openLog ( "data/provaH.xes" );
//				log = XLogReader.openLog ( "data/s12.mxml" );
			else
				log = XLogReader.openLog ( args[ 0 ] );

			printStream	= new PrintStream( outputPath + "output.txt" );

		} catch ( Exception e ) {
			System.out.println( "Exception : " + e.getMessage( ) );
			e.printStackTrace( );
			return;
		}


		TreeProcessor treeProcessor = new TreeProcessor( log, printStream );
		treeProcessor.analyzeSuccPred( treeProcessor.log );

		Set<String> allEvents	= new HashSet<>(  );
		allEvents.addAll( treeProcessor.successors.keySet() );
		allEvents.addAll( treeProcessor.predecessors.keySet( ) );

		Set<String>  events	= new HashSet<>(  );

		for ( String event : allEvents ) {
			if ( event.startsWith( "Send Notification" ) )
				events.add( event );
		}

		XLogWriter.saveXesGz( XLogReader.filterWithoutEvents( log, events ), outputPath + "L1_wo_Send_Notification" );
		XLogWriter.saveXesGz( XLogReader.filterByEvents( log, events ), outputPath + "L1_only_Send_Notification" );

		/*
		events	= new HashSet<>(  );

		for ( String event : allEvents ) {
			if ( event.startsWith( "W_Afhandelen leads" ) )
				events.add( event );
		}

		XLogWriter.saveXesGz( XLogReader.filterByEvents( log, events ), outputPath + "W_Afhandelen leads" );

		events	= new HashSet<>(  );

		for ( String event : allEvents ) {
			if ( event.startsWith( "W_Beoordelen" ) )
				events.add( event );
		}

		XLogWriter.saveXesGz( XLogReader.filterByEvents( log, events ), outputPath + "W_Beoordelen" );

		events	= new HashSet<>(  );

		for ( String event : allEvents ) {
			if ( event.startsWith( "W_Completeren" ) )
				events.add( event );
		}

		XLogWriter.saveXesGz( XLogReader.filterByEvents( log, events ), outputPath + "W_Completeren" );

		events	= new HashSet<>(  );

		for ( String event : allEvents ) {
			if ( event.startsWith( "A_" ) )
				events.add( event );
		}

		XLogWriter.saveXesGz( XLogReader.filterByEvents( log, events ), outputPath + "A_" );
		*/

//		XLogWriter.saveXesGz(
//				XLogReader.filterContainingEvents(
//						log, new HashSet<>( Arrays.asList( "A_APPROVED+complete", "A_REGISTERED+complete", "A_ACTIVATED+complete" ))
//				), outputPath + "financial_log_successful"
//		);

		treeProcessor.mine( );

		XLogWriter.saveXesGz( treeProcessor.log, outputPath + "root" );
		for ( Map.Entry< String, XLog > entry : treeProcessor.getChildLogs().entrySet() )
			XLogWriter.saveXesGz( entry.getValue(), outputPath + entry.getKey() );

		printStream.println( treeProcessor.toProcessTree() );

		BPMNDiagram bpmn = ( BPMNDiagram ) new ProcessTree2BPMNConverter( ).convertToBPMN(
				treeProcessor.toProcessTree( ), true
		)[ 0 ];
		try {
			new BpmnExportPlugin().export( treeProcessor.pluginContext, bpmn, new java.io.File( outputPath + "model.bpmn" ) );
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

package ee.ut;

import org.deckfour.xes.classification.XEventAndClassifier;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventLifeTransClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.processmining.contexts.cli.CLIContext;
import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMin;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.Task;

import java.io.PrintStream;
import java.util.*;

public class LogProcessor {
	public PrintStream printStream;
	public PluginContext pluginContext = new CLIPluginContext( new CLIContext( ), "Hybrid Miner" );;

//	Inductive Miner - incompleteness
	public MiningParameters inductiveMinerParams = new MiningParametersIMin( );
	public static XEventClassifier defaultXEventClassifier = new XEventAndClassifier( new XEventNameClassifier(), new XEventLifeTransClassifier() );

	public XLog log;
	public String SubprocessNamePrefix = "Block", DeclarativeSubprocessNamePrefix = "Declarative_" ;
	public static String DeclarativePseudoEvent = "__declarative_pseudo_event__";
	public static int DeclarativeBranchesThreshold = 3;

	public Map< String, LogProcessor > childBlocks = new HashMap<>(  );

	public LogProcessor( XLog log ) {
		this.log = log;
		this.printStream = System.out;
		this.inductiveMinerParams.setClassifier( defaultXEventClassifier );
		this.inductiveMinerParams.setUseMultithreading( false );
	}
	public LogProcessor( XLog log, PrintStream ps ) {
		this( log );
		if ( ps != null )
			this.printStream = ps;
	}

	public void mine( ) {
		try {
			this.SaveSubprocesses(
					new ContextAnalysis( this.log, printStream ).FindParallelBranches( )
			);
		} catch ( Exception e ) {
			String msg = String.format( "Exception [ %s ]: %s", this.SubprocessNamePrefix, e.getMessage( ) );
			System.out.println( msg );
			printStream.println( msg );
//				e.printStackTrace( );
			return;
		}

		XAttributeLiteralImpl nameAttr = ( XAttributeLiteralImpl ) log.getAttributes( ).getOrDefault( "concept:name", new XAttributeLiteralImpl( "concept:name", "" ) );
		nameAttr.setValue( String.format( "%s Root", nameAttr.getValue( ) ) );
	}

	public void SaveSubprocesses( List< Set< String > > parallelBranches ) {
		if ( parallelBranches == null || parallelBranches.isEmpty( ) )
			return;

		LinkedList< String > childrenToMine = new LinkedList<>(  );

		printStream.println( "Found parallel branches:" );

		for ( int i = 0, size = parallelBranches.size( ) ; i < size ; i++ ) {
			printStream.println( String.format( "%s: %s", i, parallelBranches.get( i ) ) );

			if ( parallelBranches.get( i ).size() > 1 ) {
				String name	= String.format( "%s_%d", SubprocessNamePrefix, childBlocks.size( ) );
				boolean declarativeBlock = parallelBranches.get( i ).contains( DeclarativePseudoEvent );

				if ( declarativeBlock )
					name = DeclarativeSubprocessNamePrefix + name;
				else
					childrenToMine.add( name );

				XLog sublog	= XLogReader.filterRemoveByEvents( log, parallelBranches.get( i ), name );

				childBlocks.put( name, new LogProcessor( sublog, printStream ) );
				childBlocks.get( name ).SubprocessNamePrefix = name;

				XAttributeLiteralImpl nameAttr = ( XAttributeLiteralImpl ) sublog.getAttributes( ).getOrDefault( "concept:name", new XAttributeLiteralImpl( "concept:name", "" ) );
				nameAttr.setValue( String.format( "%s %s", nameAttr.getValue( ), name ) );
			}
		}
		printStream.println();

//		Purpose: distinct separation of parent & child logging
		for ( String name : childrenToMine )
			childBlocks.get( name ).mine( );
	}

	public ProcessTree toProcessTree() {
		ProcessTree result = IMProcessTree.mineProcessTree( log, inductiveMinerParams );
		if ( result.getRoot( ) instanceof Block )
			ProcessTreeIterator( ( Block ) result.getRoot( ), childBlocks );
		return result;
	}

//	Recursively iterates over Block's children, replacing childBlocks' keys with IMProcessTree
	public static void ProcessTreeIterator( Block root, Map< String, LogProcessor > subProcesses ) {
		int i = 0;
		for (Iterator< Node > nodeIterator = root.iterator(); nodeIterator.hasNext( ); i ++ ) {
			Node node = nodeIterator.next();
			String nodeName	= node.getName();

//			Remove +complete suffix
			if ( nodeName.endsWith( "+complete" ))
				nodeName	= nodeName.substring( 0, nodeName.indexOf( "+complete" ) );

			if ( node instanceof Task.Manual && subProcesses.containsKey( nodeName ) ) {
				node.getParents().iterator().next().swapChildAt(
						subProcesses.get( nodeName ).toProcessTree( ).getRoot( ), i
				);
			}
			else if ( node instanceof Block && ( ( Block ) node ).numChildren() > 0 ) {
				ProcessTreeIterator( ( Block ) node, subProcesses );
			}
		}
	}

	public Map< String, XLog > getChildLogs() {
		HashMap< String, XLog > result = new HashMap<>();

		for( String name : this.childBlocks.keySet() ) {
			result.put( name, childBlocks.get( name ).log );
			result.putAll( childBlocks.get( name ).getChildLogs() );
		}
		return	result;
	}
}

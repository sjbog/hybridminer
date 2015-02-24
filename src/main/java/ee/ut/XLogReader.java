package ee.ut;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.in.XMxmlGZIPParser;
import org.deckfour.xes.in.XMxmlParser;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.io.File;
import java.util.*;

public class XLogReader {
	public static XFactory factory = XFactoryRegistry.instance( ).currentDefault( );
	public static XConceptExtension xConceptExtentionInstance = XConceptExtension.instance( );

	public static XLog openLog( String inputLogFileName ) throws Exception {
		XLog log = null;

		if ( inputLogFileName.toLowerCase( ).contains( "mxml.gz" ) ) {
			XMxmlGZIPParser parser = new XMxmlGZIPParser( );
			if ( parser.canParse( new File( inputLogFileName ) ) ) {
				try {
					log = parser.parse( new File( inputLogFileName ) ).get( 0 );
				} catch ( Exception e ) {
					e.printStackTrace( );
				}
			}
		} else if ( inputLogFileName.toLowerCase( ).contains( "mxml" ) ||
				inputLogFileName.toLowerCase( ).contains( "xml" ) ) {
			XMxmlParser parser = new XMxmlParser( );
			if ( parser.canParse( new File( inputLogFileName ) ) ) {
				try {
					log = parser.parse( new File( inputLogFileName ) ).get( 0 );
				} catch ( Exception e ) {
					e.printStackTrace( );
				}
			}
		} else if ( inputLogFileName.toLowerCase( ).contains( "xes.gz" ) ) {
			XesXmlGZIPParser parser = new XesXmlGZIPParser( );
			if ( parser.canParse( new File( inputLogFileName ) ) ) {
				try {
					log = parser.parse( new File( inputLogFileName ) ).get( 0 );
				} catch ( Exception e ) {
					e.printStackTrace( );
				}
			}
		} else if ( inputLogFileName.toLowerCase( ).contains( "xes" ) ) {
			XesXmlParser parser = new XesXmlParser( );
			if ( parser.canParse( new File( inputLogFileName ) ) ) {
				try {
					log = parser.parse( new File( inputLogFileName ) ).get( 0 );
				} catch ( Exception e ) {
					e.printStackTrace( );
				}
			}
		}
		if ( log == null )
			throw new Exception( "Couldn't read log file" );
		return log;
	}

	public static XLog sliceLastN( XLog log, int n ) {
		List< XTrace > tmp_sub_log = log.subList( log.size( ) - n, log.size( ) );

		log = factory.createLog( ( XAttributeMap ) log.getAttributes( ).clone() );
		log.addAll( tmp_sub_log );

		return log;
	}

	public static XLog filterByEvents( XLog log, Set< String > targetEvents ) {
		XLog filteredLog	= factory.createLog( ( XAttributeMap ) log.getAttributes().clone() );
		XLogInfo logInfo	= XLogInfoImpl.create( log, TreeProcessor.defaultXEventClassifier );
		XTrace filteredTrace;

		for ( XTrace trace : log ) {
			filteredTrace	= factory.createTrace ( ( XAttributeMap ) trace.getAttributes ().clone() );

			for ( XEvent event : trace ) {
				if ( targetEvents.contains( fetchName( event, logInfo ) ) )
					filteredTrace.add( event );
			}
			if ( filteredTrace.size() > 0 )
				filteredLog.add( filteredTrace );
		}
		return filteredLog;
	}

	public static XLog filterRemoveByEvents( XLog log, Set< String > targetEvents, String eventNameReplacement ) {
		XLog filteredLog	= factory.createLog( ( XAttributeMap ) log.getAttributes().clone() );
		XLogInfo logInfo	= XLogInfoImpl.create( log, TreeProcessor.defaultXEventClassifier );
		XTrace filteredTrace;

		for ( XTrace trace : log ) {
			filteredTrace	= factory.createTrace ( ( XAttributeMap ) trace.getAttributes ().clone() );
			LinkedList< Integer > indicesToRemove = new LinkedList<>( );

			for ( int i = 0, traceSize = trace.size( ) ; i < traceSize ; i++ ) {
				XEvent event = trace.get( i );

				if ( targetEvents.contains( fetchName( event, logInfo ) ) )
					if ( filteredTrace.isEmpty( ) ) {
						filteredTrace.add( ( XEvent ) event.clone( ) );
						event = ( XEvent ) event.clone( );
						XExtendedEvent.wrap( event ).setName( eventNameReplacement );
						XExtendedEvent.wrap( event ).setTransition( "complete" );
						trace.set( i, event );
					} else {
						filteredTrace.add( event );
						indicesToRemove.add( i );
					}
			}
			if ( filteredTrace.size() > 0 ) {
				filteredLog.add( filteredTrace );
				Collections.reverse( indicesToRemove );
				for ( int i : indicesToRemove )
					trace.remove( i );
			}
		}
		return filteredLog;
	}

	public static String fetchName( XEvent event, XLogInfo logInfo ) {
		return logInfo.getEventClasses().getClassOf( event ).toString();
	}

	public static List< List< List< String > > > divideLog( List< XTrace > log, Set< String > structured_events, Set< String > unstructured_events ) {
		LinkedList< List< String > > structured_seq = new LinkedList<>( );
		LinkedList< List< String > > unstructured_seq = new LinkedList<>( );

		boolean is_structured_seq = true;
		ArrayList< String > curr_seq = new ArrayList<>( );

		for ( XTrace trace : log ) {
			for ( int i = 0, size = trace.size( ) ; i < size ; i++ ) {

				XExtendedEvent event = XExtendedEvent.wrap( trace.get( i ) );
				String event_name = event.getName( );

				if ( structured_events.contains( event_name ) ) {
					if ( is_structured_seq ) {
						curr_seq.add( event_name );
					} else {
						unstructured_seq.add( curr_seq );
						curr_seq = new ArrayList<>( );
						is_structured_seq = true;
					}
				} else {
					if ( ! is_structured_seq ) {
						curr_seq.add( event_name );
					} else {
						structured_seq.add( curr_seq );
						curr_seq = new ArrayList<>( );
						is_structured_seq = false;
					}
				}
			}
			if ( is_structured_seq ) {
				structured_seq.add( curr_seq );
			} else {
				unstructured_seq.add( curr_seq );
			}
		}
		List< List< List< String > > > result = new LinkedList<>( );
		result.add( structured_seq );
		result.add( unstructured_seq );

		return result;
	}
}

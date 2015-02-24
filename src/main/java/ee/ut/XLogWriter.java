package ee.ut;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.out.*;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class XLogWriter {
	public static XSerializer mxml	= new XMxmlSerializer();
	public static XSerializer mxmlGz	= new XMxmlGZIPSerializer();
	public static XSerializer xes	= new XesXmlSerializer();
	public static XSerializer xesGz	= new XesXmlGZIPSerializer();

	public static XConceptExtension xConceptExtentionInstance = XConceptExtension.instance( );


	public static void saveMxml( XLog log, String filename ) {
		save( log, filename, mxml );
	}

	public static void saveMxmlGz( XLog log, String filename ) {
		save( log, filename, mxmlGz );
	}

	public static void saveXes( XLog log, String filename ) {
		save( log, filename, xes );
	}

	public static void saveXesGz( XLog log, String filename ) {
		save( log, filename, xesGz );
	}

	public static void save( XLog log, String filename, XSerializer serializer ) {
		try {
			OutputStream saveStream = new FileOutputStream( filename + "." + serializer.getSuffices()[ serializer.getSuffices().length -1 ] );
			serializer.serialize( log, saveStream );
			saveStream.flush();
			saveStream.close();
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	static public void print( XLog log, PrintStream out ) {
		for ( XTrace trace : log ) {
			out.println( "Trace name : " + xConceptExtentionInstance.extractName( trace ) );

			for ( XEvent event : trace ) {

				XExtendedEvent extended_event = XExtendedEvent.wrap( event );
				out.println( String.format( "\t[ %s # %s, %s ]\t%s"
						, extended_event.getResource( )
						, extended_event.getTransition( )
						, extended_event.getAttributes( ).get( "time:timestamp" )
						, extended_event.getName( )
				) );
			}
		}
	}
}

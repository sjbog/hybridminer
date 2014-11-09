package ee.ut;

import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.in.XMxmlGZIPParser;
import org.deckfour.xes.in.XMxmlParser;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class XLogReader {
	public static XLog openLog(String inputLogFileName) throws Exception {
		XLog log = null;

		if(inputLogFileName.toLowerCase().contains("mxml.gz")){
			XMxmlGZIPParser parser = new XMxmlGZIPParser();
			if(parser.canParse(new File(inputLogFileName))){
				try {
					log = parser.parse(new File(inputLogFileName)).get(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		else if(inputLogFileName.toLowerCase().contains("mxml") ||
				inputLogFileName.toLowerCase().contains("xml")){
			XMxmlParser parser = new XMxmlParser();
			if(parser.canParse(new File(inputLogFileName))){
				try {
					log = parser.parse(new File(inputLogFileName)).get(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		else if(inputLogFileName.toLowerCase().contains("xes.gz")){
			XesXmlGZIPParser parser = new XesXmlGZIPParser();
			if(parser.canParse(new File(inputLogFileName))){
				try {
					log = parser.parse(new File(inputLogFileName)).get(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		else if(inputLogFileName.toLowerCase().contains("xes")){
			XesXmlParser parser = new XesXmlParser();
			if(parser.canParse(new File(inputLogFileName))){
				try {
					log = parser.parse(new File(inputLogFileName)).get(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}


		if(log == null)
			throw new Exception("Couldn't read log file");
		
		return log;
	}

	public static List< List< List<String> > > divideLog ( List< XTrace > log, Set< String > structured_events, Set< String > unstructured_events )	{

		LinkedList< List<String> >	structured_seq		= new LinkedList<> (  );
		LinkedList< List<String> >	unstructured_seq	= new LinkedList<> (  );

		boolean is_structured_seq	= true;

		ArrayList< String > curr_seq = new ArrayList<> ( );

		for ( XTrace trace : log )	{
			for ( int i = 0, size = trace.size (); i < size ; i ++ )	{

				XExtendedEvent	event	= XExtendedEvent.wrap ( trace.get ( i ) );

				String event_name = event.getName ( );

				if ( structured_events.contains ( event_name ))	{
					if ( is_structured_seq )	{
						curr_seq.add ( event_name );
					}
					else {
						unstructured_seq.add ( curr_seq );
						curr_seq	= new ArrayList<> (  );
						is_structured_seq	= true;
					}
				}
				else {
					if ( ! is_structured_seq )	{
						curr_seq.add ( event_name );
					}
					else {
						structured_seq.add ( curr_seq );
						curr_seq	= new ArrayList<> (  );
						is_structured_seq	= false;
					}
				}
			}

			if ( is_structured_seq )	{
				structured_seq.add ( curr_seq );
			}
			else {
				unstructured_seq.add ( curr_seq );
			}
		}

		List< List < List<String> > >	result = new LinkedList<> (  );
		result.add ( structured_seq );
		result.add ( unstructured_seq );

		return	result;
	}
}

//public class XLogWriter {
//	public static
//}
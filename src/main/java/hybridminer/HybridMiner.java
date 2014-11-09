package hybridminer;

import ee.ut.XLogReader;
import hybridminer_refactor.LogProcessor;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.out.XesXmlGZIPSerializer;

import java.io.*;
import java.util.*;


public class HybridMiner {

	//ESPERIMENTONE   LIVELLO 1    0.5  3
	// LIVELLO 2    0.5    3

	//	private static boolean all = true;
	private static HashMap<Integer,Set<String>> groupsOfEvents = new HashMap<Integer,Set<String>>();
	private static int groupID = 1;
	private static int level = 1;
	private static int maximumSuccessors = 4;
	private static int maximumPredecessors = 4;
	//private static double percentageOfCommonElements = 0.1;
	private static HashMap<String, HashMap<String, Double>> resultBefore = new HashMap<String, HashMap<String,Double>>();
	private static HashMap<String, HashMap<String, Double>> resultAfter = new HashMap<String, HashMap<String,Double>>();
	private static HashMap<String, Double> frequencies = new HashMap<String, Double>();
	private static  Vector<String> analyzedNodes = new Vector<String>();
	private static  Vector<String> groupAdded = new Vector<String>();
	private static float minDeclSupportPercentage = 0.01f;

	public static void main(String[] args) throws Exception {
		//args[0] =new String[]{"."};
		
//		InputStream templateInputStream = new FileInputStream("./financial_log.mxml.gz");
//		File languageFile = null;
//		try {
//			languageFile = File.createTempFile("cpnToolsSimulationLog", ".mxml");
////			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(templateInputStream));
////			String line = bufferedReader.readLine();
////			PrintStream out = new PrintStream(languageFile);
////			while (line != null) {
////				out.println(line);
////				line = bufferedReader.readLine();
////			}
////			out.flush();
////			out.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		
		
		
		File fi= new File(".\\output");
		fi.mkdir();

//		String inputLogFileName = "data/L1.mxml";
		String inputLogFileName = "data/financial_log.mxml.gz";

		String outputFileName = ".\\output\\output.txt";
		//String proceduralLogPrefix = args[0]+"\\procedural";
		String proceduralLogPrefix = ".\\output\\procedural";
//		String rootLogPrefix = args[0]+"\\level1";
		String rootLogPrefix = ".\\output\\level1";
	//	String declarativeLogPrefix = args[0]+"\\declarative";
		String declarativeLogPrefix = ".\\output\\declarative";
		
		for(int g=1; g<=1; g++){
			
			if(g==2){
				
				inputLogFileName = ".\\level11.xes.gz";
				//String outputFileName = "C:\\Users\\fabrizio\\Desktop\\output.txt";
				//String proceduralLogPrefix = "C:\\Users\\fabrizio\\Desktop\\procedural";
				rootLogPrefix = ".\\level2";
				//String declarativeLogPrefix = "C:\\Users\\fabrizio\\Desktop\\declarative";
				
				//private static HashMap<Integer,Set<String>> groupsOfEvents = new HashMap<Integer,Set<String>>();
			//	private static int groupID = 1;
				level = 2;
				maximumSuccessors = 4;
				maximumPredecessors = 4;
			}

			HashMap<Integer,XLog> sublogsProcedural = new HashMap<Integer,XLog>();
			HashMap<Integer,XLog> sublogsDeclarative = new HashMap<Integer,XLog>();
			HashMap<String,Double> eventFrequencies = new HashMap<String, Double>();

			PrintStream pw = new PrintStream (outputFileName);

			XLog log = XLogReader.openLog ( inputLogFileName );
//			log	= LogProcessor.slice_last_n ( log, 200 );


			//	HashMap<String, ArrayList<String>> possiblePredecessors = new HashMap<String, ArrayList<String>>();
			//	HashMap<String, ArrayList<String>> possibleSuccessors = new HashMap<String, ArrayList<String>>();

			for(XTrace t : log){
				String previousEvent = null;
				HashMap<String,Double> endEvents = new HashMap<String,Double>();
				HashMap<String,Double> startEvents = new HashMap<String,Double>();
				XEvent lastEvent = null;
				for(XEvent e : t){
					lastEvent = e; 
					String beforeEvent;
					String afterEvent;
					String currentEvent = ((XAttributeLiteral) e.getAttributes().get("concept:name")).getValue()+"#"+XLifecycleExtension.instance().extractTransition(e);

					if(eventFrequencies.containsKey(currentEvent)){
						eventFrequencies.put(currentEvent, eventFrequencies.get(currentEvent)+1);
					}else{
						eventFrequencies.put(currentEvent, 1.);
					}
					Double freq = 0.;
					if(frequencies.containsKey(currentEvent)){
						freq = frequencies.get(currentEvent);
						freq ++;
						frequencies.put(currentEvent, freq);
					}else{
						freq ++;
						frequencies.put(currentEvent, freq);
					}

					if(previousEvent== null){
						if(startEvents.containsKey(currentEvent)){
							//currentAfter = resultAfter.get(currentEvent);
							startEvents.put(currentEvent, startEvents.get(currentEvent)+1);
						}else{
							startEvents.put(currentEvent, 1.);
						}
						resultAfter.put("start",startEvents);
						beforeEvent = "start";
						previousEvent = currentEvent;

						HashMap<String, Double> currentBefore;

						if(resultBefore.containsKey(currentEvent)){
							currentBefore = resultBefore.get(currentEvent);
						}else{
							currentBefore = new HashMap<String, Double>();
						}
						Double noBefore = 0.;
						if(currentBefore.containsKey("start")){
							noBefore = currentBefore.get("start");
							noBefore ++;
							currentBefore.put("start", noBefore);
						}else{
							noBefore ++;
							currentBefore.put("start", noBefore);
						}
						resultBefore.put(currentEvent, currentBefore);
					}else{
						beforeEvent = previousEvent;
						previousEvent = currentEvent;

						HashMap<String, Double> currentBefore;
						HashMap<String, Double> currentAfter;
						if(resultBefore.containsKey(currentEvent)){
							currentBefore = resultBefore.get(currentEvent);
						}else{
							currentBefore = new HashMap<String, Double>();
						}
						Double noBefore = 0.;
						if(currentBefore.containsKey(beforeEvent)){
							noBefore = currentBefore.get(beforeEvent);
							noBefore ++;
							currentBefore.put(beforeEvent, noBefore);
						}else{
							noBefore ++;
							currentBefore.put(beforeEvent, noBefore);
						}

						resultBefore.put(currentEvent, currentBefore);
						afterEvent = currentEvent;
						currentEvent = beforeEvent;

						if(resultAfter.containsKey(currentEvent)){
							currentAfter = resultAfter.get(currentEvent);
						}else{
							currentAfter = new HashMap<String, Double>();
						}
						Double noAfter = 0.;
						if(currentAfter.containsKey(afterEvent)){
							noAfter = currentAfter.get(afterEvent);
							noAfter ++;
							currentAfter.put(afterEvent, noAfter);
						}else{
							noAfter ++;
							currentAfter.put(afterEvent, noAfter);
						}
						resultAfter.put(currentEvent, currentAfter);
					}
				}
				String current = ((XAttributeLiteral) lastEvent.getAttributes().get("concept:name")).getValue()+"#"+XLifecycleExtension.instance().extractTransition(lastEvent);
				if(endEvents.containsKey(current)){
					//currentAfter = resultAfter.get(currentEvent);
					endEvents.put(current, endEvents.get(current)+1);
				}else{
					endEvents.put(current, 1.);
				}
				resultBefore.put("end",endEvents);
				String afterEvent = "end";
				String currentEvent = previousEvent;
				HashMap<String, Double> currentAfter;
				if(resultAfter.containsKey(currentEvent)){
					currentAfter = resultAfter.get(currentEvent);
				}else{
					currentAfter = new HashMap<String, Double>();
				}
				Double noAfter = 0.;
				if(currentAfter.containsKey(afterEvent)){
					noAfter = currentAfter.get(afterEvent);
					noAfter ++;
					currentAfter.put(afterEvent, noAfter);
				}else{
					noAfter ++;
					currentAfter.put(afterEvent, noAfter);
				}
				resultAfter.put(currentEvent, currentAfter);
			}

			for(String event : resultBefore.keySet()){

				if(!event.equals("start") && !event.equals("end")){

					for(String target : resultBefore.get(event).keySet()){
						if(frequencies.get(target)==null){
							frequencies.put(target, (double)log.size());
						}
						if(frequencies.get(event)==null){
							frequencies.put(event, (double)log.size());
						}	
						double beft = resultBefore.get(event).get(target)/frequencies.get(target);
						double befe = resultBefore.get(event).get(target)/frequencies.get(event);
//						pw.println(target +"(freq = "+beft+ ") "+event+"(freq = "+befe+") = "+(beft+befe)/2);

					}

					for(String target : resultAfter.get(event).keySet()){
						if(frequencies.get(target)==null){
							frequencies.put(target, (double)log.size());
						}
						if(frequencies.get(event)==null){
							frequencies.put(event, (double)log.size());
						}
						double aftt = resultAfter.get(event).get(target)/frequencies.get(target);
						double afte = resultAfter.get(event).get(target)/frequencies.get(event);
//						pw.println(event + "(freq = "+afte+") "+target+"(freq = "+aftt+") = "+(aftt+afte)/2);


					}
					/*pw.println("");
					pw.println("");
					pw.println(event+": number of predecessors = "+ resultBefore.get(event).keySet().size()+"; number of successors ="+ resultAfter.get(event).size());
					pw.println("");
					pw.println("");*/

					//		String eventToVisit = "start";



					//				int noPredec = resultBefore.get(event).keySet().size();
					//				int noSucc = resultAfter.get(event).keySet().size();
					//
					//				if(noPredec <= maximumPredecessors){
					//					boolean groupFound = false;
					//					for(int group : groupsOfEvents.keySet()){
					//						boolean containsPredecessor = false;
					//						for(String pred : resultBefore.get(event).keySet()){
					//							if(groupsOfEvents.get(group).contains(pred)){
					//								if(resultAfter.get(pred).keySet().size()<=maximumSuccessors){
					//									containsPredecessor = true;
					//								}
					//							}
					//						}
					//						if(groupsOfEvents.get(group).contains(event)||containsPredecessor){
					//							for(String predec : resultBefore.get(event).keySet()){
					//								if(resultAfter.containsKey(predec)){
					//									if(resultAfter.get(predec).keySet().size()<=maximumSuccessors){
					//										groupsOfEvents.get(group).add(predec);
					//									}
					//								}
					//							}
					//							groupFound = true;
					//						}
					//					}
					//					if(!groupFound){
					//						HashSet<String> newGroup = new HashSet<String>();
					//						newGroup.add(event);
					//						for(String predec : resultBefore.get(event).keySet()){
					//							if(resultAfter.containsKey(predec)){
					//								if(resultAfter.get(predec).keySet().size()<=maximumSuccessors){
					//									newGroup.add(predec);
					//								}
					//							}
					//						}
					//						groupsOfEvents.put(groupID, newGroup);
					//						groupID++;
					//					}
					//				}
					//
					//				if(noSucc <= maximumSuccessors){
					//					boolean groupFound = false;
					//					//for(String predecessor : resultBefore.get(event).keySet()){
					//					for(int group : groupsOfEvents.keySet()){
					//						boolean containsSuccessor = false;
					//						for(String succ : resultAfter.get(event).keySet()){
					//							if(groupsOfEvents.get(group).contains(succ)){
					//								if(resultBefore.get(succ).keySet().size()<=maximumPredecessors){
					//									containsSuccessor = true;
					//								}
					//							}
					//						}
					//
					//						if(groupsOfEvents.get(group).contains(event) || containsSuccessor){
					//							for(String succ : resultAfter.get(event).keySet()){
					//								if(resultBefore.containsKey(succ)){
					//									if(resultBefore.get(succ).keySet().size()<=maximumPredecessors){
					//										groupsOfEvents.get(group).add(succ);
					//									}
					//								}
					//							}
					//							groupFound = true;
					//						}
					//					}
					//					if(!groupFound){
					//						HashSet<String> newGroup = new HashSet<String>();
					//						newGroup.add(event);
					//						for(String succ : resultAfter.get(event).keySet()){
					//							if(resultBefore.containsKey(succ)){
					//								if(resultBefore.get(succ).keySet().size()<=maximumPredecessors){
					//									if(resultBefore.get(succ).keySet().size()<=maximumPredecessors){
					//										newGroup.add(succ);
					//									}
					//								}
					//							}
					//						}
					//						groupsOfEvents.put(groupID, newGroup);
					//						groupID++;
					//					}
					//				}
				}
			}

			for(String bef : resultBefore.keySet()){
				if(resultBefore.get(bef).containsKey("start")){
					visit(bef, new HashSet<String>());
				}
			}



			XFactory fac = XFactoryRegistry.instance().currentDefault();

			for (int groupId : groupsOfEvents.keySet()){
				XLog sublog = fac.createLog();
				sublogsProcedural.put(groupId, sublog);
			}
			int traceId = 1;
			ArrayList<String> proceduralEvents = new ArrayList<String>();
			for(int group : groupsOfEvents.keySet()){
				proceduralEvents.addAll(groupsOfEvents.get(group));
			}

			pw.println ( "Groups of structured events:" );
			pw.println ( groupsOfEvents );

			XLog declarativeLog = fac.createLog();
			Integer maxDecTraceSize = 0;
			int declIndex = 1;
			boolean added  = false;
			boolean declActive = false;
			XTrace cTrace = null;
			XTrace rootTrace = null;
			XEvent rootEvent = null;
			XTrace decTrace = null;
			XEvent decEvent = null;
			XEvent cEvent = null;
			XLog cLog = null;
			int subprocessIndex = -1;
			XLog root = fac.createLog();
			decTrace = fac.createTrace();
			XConceptExtension.instance().assignName(decTrace, ""+declIndex);
			String currentEvent = null;
			int currentProcessedGroupId = -1;
			for(XTrace t : log){
				int oldProcessedGroupId = -1;
				rootTrace = fac.createTrace();
				added = false;
				XEvent last = null;
				for(XEvent e : t){
					last = e;
					//currentProcessedGroupId = -1;
					currentEvent = ( ( XAttributeLiteral ) e.getAttributes ( ).get ( "concept:name" ) ).getValue ( ) + "#" + XLifecycleExtension.instance ( ).extractTransition ( e );
					//Vector<Integer> groupids = new Vector<Integer>();
					if(proceduralEvents.contains(currentEvent) && !added){
						rootEvent = fac.createEvent();
						declActive = false;
						//	added = false;
						for(int group : groupsOfEvents.keySet()){
							if(groupsOfEvents.get(group).contains(currentEvent)){
								subprocessIndex = group;
								//		added = true;
							}
						}
//						if(!added){
//							System.out.println();
//						}
						XConceptExtension.instance().assignName(rootEvent, "P"+level+"."+subprocessIndex);
						rootTrace.add(rootEvent);
						added = true;
					}
					if(proceduralEvents.contains(currentEvent) && declActive){
						declActive = false;
						added = false;
						declarativeLog.add(decTrace);
						if(decTrace.size()>maxDecTraceSize){
							maxDecTraceSize = decTrace.size();
						}
						declIndex++;
						decTrace = fac.createTrace();
						XConceptExtension.instance().assignName(decTrace, ""+declIndex);
					}
					boolean found = false;
					for(int group : groupsOfEvents.keySet()){
						if(groupsOfEvents.get(group).contains(currentEvent)){
							currentProcessedGroupId = group;
							found = true;
						}
					}
					if(!found){
						currentProcessedGroupId = 0;
					}
					if(!proceduralEvents.contains(currentEvent)){
						//added = false;
						declActive = true;
						decEvent = (XEvent) e.clone();
						XConceptExtension.instance().assignName(decEvent, currentEvent);
						decTrace.add(decEvent);
						rootEvent = (XEvent) e.clone();
						rootTrace.add(rootEvent);
					}else{
						declActive = false;
						if((currentProcessedGroupId!=oldProcessedGroupId) &&oldProcessedGroupId!=-1){
							rootEvent = fac.createEvent();
							//	added = false;
							for(int group : groupsOfEvents.keySet()){
								if(groupsOfEvents.get(group).contains(currentEvent)){
									subprocessIndex = group;
									//		added = true;
								}
							}
							//if(!added){
							//	System.out.println();
							//	}
							XConceptExtension.instance().assignName(rootEvent, "P"+level+"."+subprocessIndex);
							rootTrace.add(rootEvent);
							added = true;
						}
					}

					if(oldProcessedGroupId!=currentProcessedGroupId){
						if(oldProcessedGroupId!= -1 && oldProcessedGroupId!= 0){
							XTrace toAdd = (XTrace) cTrace.clone();
							XConceptExtension.instance().assignName(toAdd, traceId+"");
							traceId++;
							cLog.add(toAdd);
						}
						cTrace = fac.createTrace();
						cEvent = (XEvent) e.clone();
						cTrace.add(cEvent);
						cLog = sublogsProcedural.get(currentProcessedGroupId);
					}else{
						if(cTrace==null){
							cTrace = fac.createTrace();
						}
						cEvent = (XEvent) e.clone();
						cTrace.add(cEvent);
					}
					oldProcessedGroupId = currentProcessedGroupId;
					//	String beforeEvent;
					//	String afterEvent;
				}
				if(!declActive){
					cTrace = fac.createTrace();
					cEvent = (XEvent) last.clone();
					cTrace.add(cEvent);
					XTrace toAdd = (XTrace) cTrace.clone();
					XConceptExtension.instance().assignName(toAdd, traceId+"");
					traceId++;
					cLog.add(toAdd);
					//cLog.add(cTrace);
				}
				if(!added && !declActive){
					rootEvent = fac.createEvent();
					//	added = true;
					for(int group : groupsOfEvents.keySet()){
						if(groupsOfEvents.get(group).contains(currentEvent)){
							subprocessIndex = group;
							//		added = false;
						}
					}
					//				if(added){
					//					System.out.println();
					//				}
					XConceptExtension.instance().assignName(rootEvent, "P"+level+"."+subprocessIndex);
					rootTrace.add(rootEvent);
				}
				if(declActive){
					declarativeLog.add(decTrace);
					if(decTrace.size()>maxDecTraceSize){
						maxDecTraceSize = decTrace.size();
					}
				}
				XConceptExtension.instance().assignName(rootTrace, XConceptExtension.instance().extractName(t));
				root.add(rootTrace);
			}

			pw.println ( );
			pw.println ( "root log" );
			LogProcessor.print_log ( root, pw );

//			pw.println (  );
//			pw.println ( "clog" );
//			LogProcessor.print_log ( cLog, pw );

			pw.println (  );
			pw.println ( "declarativeLog" );
			LogProcessor.print_log ( declarativeLog, pw );

			boolean x = true;
			if ( x )
				return;

			//int h= 1;
			for (int groupId : groupsOfEvents.keySet()){
				try {
					if(!sublogsProcedural.get(groupId).isEmpty() && sublogsProcedural.get(groupId).size()>1){
						FileOutputStream out = new FileOutputStream(proceduralLogPrefix+level+"_"+groupId+".xes.gz");
						XesXmlGZIPSerializer xesGzipSerializer = new XesXmlGZIPSerializer();
						xesGzipSerializer.serialize(sublogsProcedural.get(groupId), out);
						out.close();
					}
				} catch (FileNotFoundException e) {
					//JOptionPane.showMessageDialog(new JFrame(), e.getMessage());
				} catch (IOException e) {
					//JOptionPane.showMessageDialog(new JFrame(), e.getMessage());
				}
			}
			//	try {
			//	FileOutputStream out = new FileOutputStream(rootLogPrefix+level+"_"+".xes.gz");
			//	XesXmlGZIPSerializer xesGzipSerializer = new XesXmlGZIPSerializer();
			//	xesGzipSerializer.serialize(root, out);
			//	out.close();
			//} catch (FileNotFoundException e) {
			//	} catch (IOException e) {
			//	}




			int declGroupIndex = 1;


			if(true){

				//	for(int i=maxDecTraceSize; i>1; i--){
				//FindItemSets f = new FindItemSets(declarativeLog);
				//Map<Set<String>, Float> items = f.findItemSets(declarativeLog, i, minDeclSupportPercentage , false);
				AlgoINDIRECT indirect = new AlgoINDIRECT();
				StringSequenceDatabaseMaxSP db = SequenceMiner.convertIntoStringSequenceDatabaseMaxSP(declarativeLog);
				Map<String, Integer> map = db.getAlphabetMap();
				HashMap<Integer, String> mapping = new HashMap<Integer, String>();
				for(String str : map.keySet()){
					mapping.put(map.get(str), str);
				}
				indirect.runAlgorithm(db, outputFileName, minDeclSupportPercentage, 1., minDeclSupportPercentage);
				indirect.toString();
				ArrayList<Result> result = indirect.result;
				while(!result.isEmpty()){
					XLog tempdeclarativeLog = fac.createLog();
					int dnum = 1;
					//	System.out.println("CURRENT SIZE: "+i);
					Set<Set<String>> toBeConsidered = new HashSet<Set<String>>();
					//	float suppTobeConsidered = minDeclSupportPercentage;
					//	boolean iter = true;
					boolean found = false;
					boolean tobe = true;
					HashSet<Integer> set = new HashSet<Integer>();
					Vector<Result> toremove = new Vector<Result>();
					boolean first = true;
					for(Result r : result){
						if(first){
							first = false;
							set.addAll(r.getSet());
						}
						HashSet<Integer> cl = (HashSet<Integer>) set.clone();
						cl.retainAll(r.getSet());
						if(!cl.isEmpty()){
							set.addAll(r.getSet());
							toremove.add(r);
						}
					}
					result.removeAll(toremove);
					HashSet<String> orderedList = new HashSet<String>();
					for(Integer inte : set){
						orderedList.add(mapping.get(inte));
					}
					found = false;
					int u = 0;
					XLog subDecl = null;
					while(!found &&  u!=set.size()){
						XLog tempRoot = fac.createLog();
						subDecl = fac.createLog();
						found = true;
						boolean exit = false;
						XTrace decTr = null;
						int decTrnum = 1;
						for(XTrace t : root){
							XTrace rTrace = fac.createTrace();
							XTrace d = fac.createTrace();
							XConceptExtension.instance().assignName(d, dnum+"");
							dnum++;
							XConceptExtension.instance().assignName(rTrace, XConceptExtension.instance().extractName(t));
							boolean decRemaining = false;
							if(!exit){
								boolean declStarted = false;
								HashSet<String> currentDeclarativeNumber = new HashSet<String>();
								for(XEvent e : t){
									if(!exit ){					
										currentEvent = ((XAttributeLiteral) e.getAttributes().get("concept:name")).getValue()+"#"+XLifecycleExtension.instance().extractTransition(e);
										if(orderedList.contains(currentEvent) && declStarted){
											declStarted = true;
											XEvent decEv = (XEvent)e.clone();
											decTr.add(decEv);
											currentDeclarativeNumber.add(currentEvent);

										}
										if(orderedList.contains(currentEvent) && ! declStarted){
											declStarted = true;
											decTr = fac.createTrace();
											XEvent decEv = (XEvent)e.clone();
											decTr.add(decEv);
											XEvent rEv = fac.createEvent();
											XConceptExtension.instance().assignName(rEv, "D"+level+"."+declGroupIndex);
											rTrace.add(rEv);
											currentDeclarativeNumber.add(currentEvent);
											if(decRemaining){
												XTrace toAdd = (XTrace) d.clone();
												tempdeclarativeLog.add(toAdd);
												d = fac.createTrace();
												XConceptExtension.instance().assignName(d, dnum+"");
												dnum++;
												decRemaining = false;
											}
										}

										if(!orderedList.contains(currentEvent)){
											//declStarted = false;

											if(XLifecycleExtension.instance().extractTransition(e)!=null){
												XEvent ev = (XEvent) e.clone();
												currentEvent = ((XAttributeLiteral) e.getAttributes().get("concept:name")).getValue()+"#"+XLifecycleExtension.instance().extractTransition(e);
												XConceptExtension.instance().assignName(ev, currentEvent);
												d.add(ev);
												decRemaining = true;
											}
											XEvent rEv = (XEvent) e.clone();
											//XConceptExtension.instance().assignName(rEv, "D"+level+"."+declGroupIndex);
											rTrace.add(rEv);
											if(declStarted){
												double currentSize = currentDeclarativeNumber.size();
												double eventratio = 1;//currentSize/(double)orderedList.get(u).getFirst().size();

												if(eventratio<0){
													exit = true;
													found = false;
												}else{
													XConceptExtension.instance().assignName(decTr, decTrnum+"");
													decTrnum ++;
													XTrace toAdd = (XTrace) decTr.clone();
													subDecl.add(toAdd);

												}
											}
											declStarted = false;

										}
									}
								}
							}
							XTrace toAdd = (XTrace) rTrace.clone();
							tempRoot.add(toAdd);
							if(decRemaining){
								XTrace toA = (XTrace) d.clone();
								tempdeclarativeLog.add(toA);
								d = fac.createTrace();
								XConceptExtension.instance().assignName(d, dnum+"");
								dnum++;
								decRemaining = false;
							}
						}
						u++;
						if(found){
							root = (XLog) tempRoot.clone();
							//	declGroupIndex ++;
							declarativeLog = (XLog) tempdeclarativeLog.clone();
						}
					}		
					if(found && subDecl.size()>0){
						XLog toAdd = (XLog) subDecl.clone();
						sublogsDeclarative.put(declGroupIndex, toAdd);
						declGroupIndex++;
					}

				}

			}















			//int declGroupIndex = 1;
			for(int i=maxDecTraceSize; i>1; i--){
				FindItemSets f = new FindItemSets();
				Map<Set<String>, Float> items = f.findItemSets(declarativeLog, i, minDeclSupportPercentage , false);
				XLog tempdeclarativeLog = fac.createLog();
				int dnum = 1;
				System.out.println("CURRENT SIZE: "+i);
				Set<Set<String>> toBeConsidered = new HashSet<Set<String>>();
				float suppTobeConsidered = minDeclSupportPercentage;
				//	boolean iter = true;
				boolean found = false;
				boolean tobe = true;
				while(!items.isEmpty()&&!found&&tobe){
					for(Set<String> decl : items.keySet()){
						if(items.get(decl)>suppTobeConsidered){
							toBeConsidered = new HashSet<Set<String>>();
							suppTobeConsidered = items.get(decl); 
							toBeConsidered.add(decl);
						}
						if(items.get(decl)==suppTobeConsidered){
							toBeConsidered.add(decl);
						}
					}
					if(toBeConsidered.isEmpty()){
						tobe = false;
					}
					if(tobe){
						ArrayList<Pair<Set<String>,Double>> orderedList = new ArrayList<Pair<Set<String>,Double>>();
						//		double minimumnumberofeventsindeclarative = percentageOfCommonElements * (double)i; 
						for(Set<String> decl : toBeConsidered){
							double freq = 0.;
							for(String event: decl){
								if(eventFrequencies.containsKey(event)){
								freq = freq + eventFrequencies.get(event);
								}
							}
							Pair<Set<String>,Double> pair = new Pair<Set<String>, Double>(decl, freq);
							if(orderedList.isEmpty()){
								orderedList.add(pair);
							}else{
								for(int j = 0; j< orderedList.size();j++){
									if(orderedList.get(j).getSecond()>freq){
										orderedList.add(j, pair);
										break;
									}
								}
							}
						}

						found = false;
						int u = 0;
						XLog subDecl = null;
						while(!found &&  u!=orderedList.size()){
							XLog tempRoot = fac.createLog();
							subDecl = fac.createLog();
							found = true;
							boolean exit = false;
							XTrace decTr = null;
							int decTrnum = 1;
							for(XTrace t : root){
								XTrace rTrace = fac.createTrace();
								XTrace d = fac.createTrace();
								XConceptExtension.instance().assignName(d, dnum+"");
								dnum++;
								XConceptExtension.instance().assignName(rTrace, XConceptExtension.instance().extractName(t));
								boolean decRemaining = false;
								if(!exit){
									boolean declStarted = false;
									HashSet<String> currentDeclarativeNumber = new HashSet<String>();
									for(XEvent e : t){
										if(!exit ){					
											currentEvent = ((XAttributeLiteral) e.getAttributes().get("concept:name")).getValue()+"#"+XLifecycleExtension.instance().extractTransition(e);
											if(orderedList.get(u).getFirst().contains(currentEvent) && declStarted){
												declStarted = true;
												XEvent decEv = (XEvent)e.clone();
												decTr.add(decEv);
												currentDeclarativeNumber.add(currentEvent);

											}
											if(orderedList.get(u).getFirst().contains(currentEvent) && ! declStarted){
												declStarted = true;
												decTr = fac.createTrace();
												XEvent decEv = (XEvent)e.clone();
												decTr.add(decEv);
												XEvent rEv = fac.createEvent();
												XConceptExtension.instance().assignName(rEv, "D"+level+"."+declGroupIndex);
												rTrace.add(rEv);
												currentDeclarativeNumber.add(currentEvent);
												if(decRemaining){
													XTrace toAdd = (XTrace) d.clone();
													tempdeclarativeLog.add(toAdd);
													d = fac.createTrace();
													XConceptExtension.instance().assignName(d, dnum+"");
													dnum++;
													decRemaining = false;
												}
											}

											if(!orderedList.get(u).getFirst().contains(currentEvent)){
												//declStarted = false;

												if(XLifecycleExtension.instance().extractTransition(e)!=null){
													XEvent ev = (XEvent) e.clone();
													currentEvent = ((XAttributeLiteral) e.getAttributes().get("concept:name")).getValue()+"#"+XLifecycleExtension.instance().extractTransition(e);
													XConceptExtension.instance().assignName(ev, currentEvent);
													d.add(ev);
													decRemaining = true;
												}
												XEvent rEv = (XEvent) e.clone();
												//XConceptExtension.instance().assignName(rEv, "D"+level+"."+declGroupIndex);
												rTrace.add(rEv);
												if(declStarted){
													double currentSize = currentDeclarativeNumber.size();
													double eventratio = currentSize/(double)orderedList.get(u).getFirst().size();

													if(eventratio<0){
														exit = true;
														found = false;
													}else{
														XConceptExtension.instance().assignName(decTr, decTrnum+"");
														decTrnum ++;
														XTrace toAdd = (XTrace) decTr.clone();
														subDecl.add(toAdd);

													}
												}
												declStarted = false;

											}
										}
									}
								}
								XTrace toAdd = (XTrace) rTrace.clone();
								tempRoot.add(toAdd);
								if(decRemaining){
									XTrace toA = (XTrace) d.clone();
									tempdeclarativeLog.add(toA);
									d = fac.createTrace();
									XConceptExtension.instance().assignName(d, dnum+"");
									dnum++;
									decRemaining = false;
								}
							}
							u++;
							if(found){
								root = (XLog) tempRoot.clone();

								declarativeLog = (XLog) tempdeclarativeLog.clone();
							}
						}		
						if(found){
							XLog toAdd = (XLog) subDecl.clone();
							sublogsDeclarative.put(declGroupIndex, toAdd);
							declGroupIndex ++;
						}
						for(Set<String> decl : toBeConsidered){
							items.remove(decl);
						}
					}
				}
			}









			for (int groupId : sublogsDeclarative.keySet()){
				try {
					if(!sublogsDeclarative.get(groupId).isEmpty() && sublogsDeclarative.get(groupId).size()>1){
						FileOutputStream out = new FileOutputStream(declarativeLogPrefix+level+"_"+groupId+".xes.gz");
						XesXmlGZIPSerializer xesGzipSerializer = new XesXmlGZIPSerializer();
						xesGzipSerializer.serialize(sublogsDeclarative.get(groupId), out);
						out.close();
					}
				} catch (FileNotFoundException e) {
					//JOptionPane.showMessageDialog(new JFrame(), e.getMessage());
				} catch (IOException e) {
					//JOptionPane.showMessageDialog(new JFrame(), e.getMessage());
				}
			}
			try {
				FileOutputStream out = new FileOutputStream(rootLogPrefix+level+"_"+".xes.gz");
				XesXmlGZIPSerializer xesGzipSerializer = new XesXmlGZIPSerializer();
				xesGzipSerializer.serialize(root, out);
				System.out.println("Files in: "+rootLogPrefix);
				out.close();
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
//			pw.flush();
//			pw.close();

		}
	}

	private static void visit(String eventToVisit, HashSet<String> value){
		if(!analyzedNodes.contains(eventToVisit)){
			analyzedNodes.add(eventToVisit);
			if(resultAfter.containsKey(eventToVisit)){
				if(resultAfter.get(eventToVisit).keySet().size()<=maximumSuccessors){
					for(String 	succe : resultAfter.get(eventToVisit).keySet()){
						if(resultBefore.containsKey(succe)||succe.equals("end")){
							if(resultBefore.get(succe).keySet().size()<=maximumPredecessors){
								value.add(eventToVisit);
								value.add(succe);
							}else{
								visit(succe, new HashSet<String>());
							}
							//if(!succe.equals("end")){
							visit(succe, value);
							//}
						}
					}

				}else{
					for(String 	succe : resultAfter.get(eventToVisit).keySet()){
						visit(succe, new HashSet<String>());
					}
				}
			}

		}
		boolean found = false;
		for(String el : value){
			if(groupAdded.contains(el)){
				found = true;
			}
		}
		if(!groupAdded.contains(eventToVisit) && ! found && !value.isEmpty()){
			groupAdded.addAll(value);
			groupsOfEvents.put(groupID, value);
			groupID++;
		}
		groupAdded.add(eventToVisit);
	}
}


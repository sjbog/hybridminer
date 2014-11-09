package hybridminer_refactor;

import ee.ut.XLogReader;
import hybridminer.*;
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
	private static int maximumSuccessors = 2;
	private static int maximumPredecessors = 2;
	//private static double percentageOfCommonElements = 0.1;
	private static HashMap<String, HashMap<String, Double>> resultBefore = new HashMap<String, HashMap<String,Double>>();
	private static HashMap<String, HashMap<String, Double>> resultAfter = new HashMap<String, HashMap<String,Double>>();
	private static HashMap<String, Double> frequencies = new HashMap<String, Double>();
	private static  Vector<String> analyzedNodes = new Vector<String>();
	private static  Vector<String> groupAdded = new Vector<String>();
	private static float minDeclSupportPercentage = 0.01f;

	public static void main(String[] args) throws Exception {
		
		
		File fi= new File(".\\output_mod");
		fi.mkdir();
		String inputLogFileName = "./financial_log.mxml.gz";
		String outputFileName = ".\\output_mod\\output.txt";
		//String proceduralLogPrefix = args[0]+"\\procedural";
		String proceduralLogPrefix = ".\\output_mod\\procedural";
//		String rootLogPrefix = args[0]+"\\level1";
		String rootLogPrefix = ".\\output_mod\\level1";
	//	String declarativeLogPrefix = args[0]+"\\declarative";
		String declarativeLogPrefix = ".\\output_mod\\declarative";
		
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

			inputLogFileName	= "data/l1.mxml";
			inputLogFileName	= "data/financial_log.mxml.gz";
			LogProcessor lp = new LogProcessor ( inputLogFileName );

			if ( lp.flag )
				return;


			XLog log = XLogReader.openLog ( inputLogFileName );

			XFactory fac = XFactoryRegistry.instance ( ).currentDefault();
			XLog declarativeLog = fac.createLog();
			XLog root = fac.createLog();
			String currentEvent = null;
			Integer maxDecTraceSize = 0;





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
			pw.flush();
			pw.close();

		}
	}

}


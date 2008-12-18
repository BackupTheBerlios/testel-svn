package de.beimax.testel;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**Vergleicht erzeugte ideale Dateien mit realen Ergebnissen..
 * @author mkalus
 *
 */
public class CompareIdealtoReal {
	static HashMap<String, HashMap<String, TestElItem>> idealMap; 
	static HashMap<String, HashMap<String, TestElItem>> realMap; 

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 2) throw new Exception("2 Argumente erwartet: ideal-Pfad + real-Pfad");

		//Daten aggregieren
		System.out.println("Aggregiere Idealdaten");
		idealMap = aggregateData(args[0]);
		System.out.println("Aggregiere Realdaten");
		realMap = aggregateData(args[1]);
		
		//Daten vergleichen
		compareData();
	}

	/**
	 * Daten aggregieren
	 */
	private static HashMap<String, HashMap<String, TestElItem>> aggregateData(String path) throws Exception {
		File dir = new File(path);
		HashMap<String, HashMap<String, TestElItem>> map = new HashMap<String, HashMap<String,TestElItem>>();
		
		if (!dir.isDirectory()) throw new Exception("Kein Directory: " + dir);
		
		//Dateien aus dem Verzeichnis lesen
		String[] files = dir.list();
		
		for (int i = 0; i < files.length; i++) {
			if (!files[i].endsWith(".xml")) continue;
			
			//System.out.println("Bearbeite: " + files[i]);
			HashMap<String, TestElItem> testElItemMap = new HashMap<String, TestElItem>();
			
			//XML-Parser
			DocumentBuilderFactory factory  = DocumentBuilderFactory.newInstance();
			DocumentBuilder        builder  = factory.newDocumentBuilder();
			Document               document = builder.parse( new File(path, files[i]) );
			NodeList ndList = document.getElementsByTagName("testel:tag");
			for (int j = 0; j < ndList.getLength(); j++) {
				TestElItem item = new TestElItem();
				item.type = ndList.item(j).getNodeName();
				NamedNodeMap attribs = ndList.item(j).getAttributes();
				for (int k = 0; k < attribs.getLength(); k++) {
					String key = attribs.item(k).getNodeName();
					String val = attribs.item(k).getNodeValue();
					if (key.equals("class")) {
						item.className = val;
					} else if (key.equals("bpos")) {
						item.bpos = Integer.parseInt(val);
					} else if (key.equals("bcol")) {
						item.bcol = Integer.parseInt(val);
					} else if (key.equals("brow")) {
						item.brow = Integer.parseInt(val);
					} else if (key.equals("epos")) {
						item.epos = Integer.parseInt(val);
					} else if (key.equals("ecol")) {
						item.ecol = Integer.parseInt(val);
					} else if (key.equals("erow")) {
						item.erow = Integer.parseInt(val);
					} else throw new Exception("Unbekanntes Attribut: " + item);
				}
//				if (testElItemMap.containsKey(item.bpos + item.className))
//					throw new Exception("Doppelte Startposition: " + item + '\n' + testElItemMap.get(item.bpos + item.className));
				testElItemMap.put(item.brow + ":" + item.bcol + item.className, item);
			}
			ndList = document.getElementsByTagName("testel:ref");
			for (int j = 0; j < ndList.getLength(); j++) {
				TestElItem item = new TestElItem();
				item.type = ndList.item(j).getNodeName();
				NamedNodeMap attribs = ndList.item(j).getAttributes();
				for (int k = 0; k < attribs.getLength(); k++) {
					String key = attribs.item(k).getNodeName();
					String val = attribs.item(k).getNodeValue();
					if (key.equals("class")) {
						item.className = val;
					} else if (key.equals("bpos")) {
						item.bpos = Integer.parseInt(val);
					} else if (key.equals("bcol")) {
						item.bcol = Integer.parseInt(val);
					} else if (key.equals("brow")) {
						item.brow = Integer.parseInt(val);
					} else if (key.equals("epos")) {
						item.epos = Integer.parseInt(val);
					} else if (key.equals("ecol")) {
						item.ecol = Integer.parseInt(val);
					} else if (key.equals("erow")) {
						item.erow = Integer.parseInt(val);
					} else throw new Exception("Unbekanntes Attribut: " + item);
				}
//				if (testElItemMap.containsKey(item.bpos + item.className))
//					throw new Exception("Doppelte Startposition: " + item + '\n' + testElItemMap.get(item.bpos + item.className));
				testElItemMap.put(item.brow + ":" + item.bcol + item.className, item);
			}
			map.put(files[i], testElItemMap);
		}
		
		return map;
	}
	
	/**
	 * Daten vergleichen
	 */
	private static void compareData() throws Exception {
		String[] keys = new String[idealMap.keySet().size()];
		
		idealMap.keySet().toArray(keys);
		Arrays.sort(keys);
		
		for (int i = 0; i < keys.length; i++) {
			HashMap<String, TestElItem> map = idealMap.get(keys[i]);
			buildSums(keys[i], map);
		}
		
//		System.out.println("===============================================");
//		System.out.println("Gesamt");
//		System.out.println("===============================================");
		//LaTeX
		System.out.println("\\toprule\n" +
				"\\multicolumn{11}{l}{\\textbf{Gesamt}} \\\\\n" +
				"\\midrule\n" +
				"Typ & Klasse & \\multicolumn{1}{c}{$I$} & \\multicolumn{1}{c}{$T^+$} & " +
				"\\multicolumn{1}{c}{$T^e$} & \\multicolumn{1}{c}{$F^+$} & " +
				"\\multicolumn{1}{c|}{$F^-$} & \\multicolumn{1}{c}{$R_s$} & " +
				"\\multicolumn{1}{c}{$R_m$} & \\multicolumn{1}{c}{$P_s$} & " +
				"\\multicolumn{1}{c}{$P_m$}\\\\\n" +
				"\\midrule");

		keys = new String[totalMap.keySet().size()];

		totalMap.keySet().toArray(keys);
		Arrays.sort(keys);
		for (int i = 0; i < keys.length; i++) {
			DataItem entry = totalMap.get(keys[i]);
			System.out.println(entry);
		}
		//LaTeX
		System.out.println("\\bottomrule\n");
	}
	
	/**
	 * Gesamtsumme wird hier gespeichert
	 */
	static TreeMap<String, DataItem> totalMap = new TreeMap<String, DataItem>();

	/**Summen einer Map bauen
	 * @param map
	 */
	private static void buildSums(String key, HashMap<String, TestElItem> idealMap) throws Exception {
		TreeMap<String, DataItem> treeMap = new TreeMap<String, DataItem>();
		HashMap<String, TestElItem> realMap = CompareIdealtoReal.realMap.get(key);
		if (realMap == null) throw new Exception("Ideal und Real stimmen nicht überein...");
		
		for (Entry<String, TestElItem> entry : idealMap.entrySet()) {
			TestElItem item = entry.getValue();
			//TreeMap durchsuchen
			DataItem dataItem;
			if (treeMap.containsKey(item.type + item.className)) {
				dataItem = treeMap.get(item.type + item.className);
				dataItem.idealSum++;
			} else { //hinzufügen
				dataItem = new DataItem();
				dataItem.type = item.type;
				dataItem.className = item.className;
				dataItem.idealSum = 1;
				treeMap.put(item.type + item.className, dataItem);
			}
			//gesamtMap
			DataItem totalItem;
			if (totalMap.containsKey(item.type + item.className)) {
				totalItem = totalMap.get(item.type + item.className);
				totalItem.idealSum++;
			} else { //hinzufügen
				totalItem = new DataItem();
				totalItem.type = item.type;
				totalItem.className = item.className;
				totalItem.idealSum = 1;
				totalMap.put(item.type + item.className, totalItem);
			}
			
			//Vergleich
			TestElItem compareItem = realMap.remove(entry.getKey());
			if (compareItem == null) { //kein Treffer -> false negative
				dataItem.FN++;
				totalItem.FN++;
			} else { //möglicher Treffer
				if (item.erow == compareItem.erow && item.ecol == compareItem.ecol) {
					dataItem.TP++; //-> true positive gefunden, juhu!
					totalItem.TP++;
				} else {
					dataItem.TE++; //-> true errorous, naja.
					totalItem.TE++;
				}
			}
		}
		
		//false Positives bearbeiten -> Rest von realMap
		for (Entry<String, TestElItem> entry : realMap.entrySet()) {
			TestElItem item = entry.getValue();
			DataItem dataItem;
			if (treeMap.containsKey(item.type + item.className)) {
				dataItem = treeMap.get(item.type + item.className);
				dataItem.FP++;
			} else {
				dataItem = new DataItem();
				dataItem.type = item.type;
				dataItem.className = item.className;
				dataItem.idealSum = 0;
				dataItem.FP = 1;
				treeMap.put(item.type + item.className, dataItem);
			}
			//gesamtMap
			DataItem totalItem;
			if (totalMap.containsKey(item.type + item.className)) {
				totalItem = totalMap.get(item.type + item.className);
				totalItem.FP++;
			} else { //hinzufügen
				totalItem = new DataItem();
				totalItem.type = item.type;
				totalItem.className = item.className;
				totalItem.FP = 1;
				totalMap.put(item.type + item.className, totalItem);
			}
		}
		
//		System.out.println("===============================================");
//		System.out.println(key);
//		System.out.println("===============================================");
		//LaTeX
		System.out.println("\\toprule\n" +
				"\\multicolumn{11}{l}{\\textbf{Datei \\code{" + key + "}}} \\\\\n" +
				"\\midrule\n" +
				"Typ & Klasse & \\multicolumn{1}{c}{$I$} & \\multicolumn{1}{c}{$T^+$} & " +
				"\\multicolumn{1}{c}{$T^e$} & \\multicolumn{1}{c}{$F^+$} & " +
				"\\multicolumn{1}{c|}{$F^-$} & \\multicolumn{1}{c}{$R_s$} & " +
				"\\multicolumn{1}{c}{$R_m$} & \\multicolumn{1}{c}{$P_s$} & " +
				"\\multicolumn{1}{c}{$P_m$}\\\\\n" +
				"\\midrule");
		String[] keys = new String[treeMap.keySet().size()];
		
		treeMap.keySet().toArray(keys);
		Arrays.sort(keys);
		for (int i = 0; i < keys.length; i++) {
			DataItem entry = treeMap.get(keys[i]);
			System.out.println(entry);
		}
		//LaTeX
		System.out.println("\\bottomrule\n");
	}

	private static class TestElItem {
		String type;
		String className;
		int bpos, bcol, brow;
		int epos, ecol, erow;
		
		public String toString() {
			return "<" + type + " class=\""+className+"\" bpos=\""+bpos+"\" bcol=\""+bcol+"\" brow=\""+brow+"\" epos=\""+epos+"\" ecol=\""+ecol+"\" erow=\""+erow+"\" />";
		}
	}
	
	private static class DataItem {
		String type;
		String className;
		
		int idealSum;
		int TP = 0, FN = 0, FP = 0, TE = 0;
		
		public String toString() {
			double TEh = ((double) TE)/2;
			
			double RS = TP;
			if (RS != 0) RS = RS/idealSum;
			double RM = TEh + TP;
			if (RM != 0) RM = RM/idealSum;
			double PS = TP + FP;
			if (PS != 0) PS = TP/PS;
			double PM = TEh + TP + FP;
			if (PM != 0) PM = (TEh + TP)/PM;
//			return type.substring(7) + "("+ className + "), I: " + idealSum + ", TP: " + TP + ", FN: " + FN + ", FP: " + FP + ", TE: " + TE + ";  RS=" + round(RS, 2) + ", RM=" + round(RM, 2) + ", PS=" + round(PS, 2) + ", PM=" + round(PM, 2);
			//LaTeX
			return "\\code{" + type.substring(7) + "} & \\code{"+ className.replace("_", "\\_") + "} & " + idealSum + " & " + TP + " & " + TE + " & " + FP + " & " + FN + " & " + round(RS, 2) + " & " + round(RM, 2) + " & " + round(PS, 2) + " & " + round(PM, 2) + " \\\\";
		}
		
		public static double round(double d, int decimalPlace) {
			// see the Javadoc about why we use a String in the constructor
			// http://java.sun.com/j2se/1.5.0/docs/api/java/math/BigDecimal.html#BigDecimal(double)
			BigDecimal bd = new BigDecimal(Double.toString(d));
			bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
			return bd.doubleValue();
		}
	}

}

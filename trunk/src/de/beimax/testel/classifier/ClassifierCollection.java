/**
 * Datei: ClassifierCollection.java
 * Paket: de.beimax.testel.classifier
 * Projekt: TestEl
 *
 * Copyright (c) 2008 Maximilian Kalus.  All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * or visit: http://www.gnu.org/licenses/lgpl.html
 *
 */
package de.beimax.testel.classifier;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import de.beimax.testel.config.Config;
import de.beimax.testel.exception.TestelClassifierException;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.token.SubTokenList;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;
import de.beimax.testel.token.impl.SomeToken;
import de.beimax.testel.token.impl.TestElTag;

/**Collection von gleichen Klassifizierern
 * @author mkalus
 *
 */
public class ClassifierCollection {
	//Logger
	static final Logger logger = Logger.getLogger(ClassifierCollection.class.getName());

	/**Kollektion selbst - die Implementation ist eine Subklasse unten
	 */
	private Collection collection; 
	
	/**Klasse der Klassifizierer
	 * Class classifierClass 
	 */
	private Class classifierClass;
	
	private final String classifierName;
	
	/** Konstruktor
	 * @param classifierClass Klasse des Klassifizierers
	 */
	public ClassifierCollection(Class classifierClass) throws TestelException {
		//Testen
		try {
			Object o = classifierClass.newInstance();
			if (!(o instanceof Classifier)) throw new ClassCastException(classifierClass + " ist keine Implementation der Schnittstelle de.beimax.testel.classifier.Classifier");
			classifierName = ((Classifier) o).getClassifierName();
		} catch (Exception e) {
			throw new TestelException("Fehler beim Initiieren der Klassifizierer-Kollektion:\n" + e.getLocalizedMessage());
		}
		
		this.classifierClass = classifierClass;
		
		//Kollektion erstellen
		collection = new Collection();
	}
	
	/** Getter für classifierName
	 * @return classifierName
	 */
	public String getClassifierName() {
		return classifierName;
	}

	/**Lernmethode: Fügt einen neuen Klassifizierer in die Collection ein
	 * @param classifier
	 * @return true, falls die Liste aufgenommen wurde, false, falls sie nicht in die Collection
	 * aufgenommen wurde
	 * @throws TestelException
	 */
	public boolean add(SubTokenList subList, TokenList completeList) throws TestelException {
		return collection.add(subList, completeList);
	}
	
	/**Gibt eine Klassifizierer-Instanz zurück
	 * @return
	 * @throws TestelException
	 */
	public Classifier getNewClassifier() throws TestelClassifierException {
		try {
			return (Classifier) classifierClass.newInstance();
		} catch (Exception e) {
			throw new TestelClassifierException("Konnte keinen neuen Klassifizierer des Typs " + classifierClass + " erstellen:\n" + e.getLocalizedMessage());
		}
	}
	
	/**Taggt die Liste mit Hilfe der Daten in der Kollektion
	 * @param list
	 * @return
	 */
	public TokenList tag(TokenList list) throws TestelTaggerException {
		return new Matcher().tag(list); //die Arbeit wird an den Matcher weitergegeben
	}

	/**Prüft, ob eine Liste eine valide Match-List ist, d.h. es müssen TestEl-Match-Tags am
	 * Anfang und am Ende existieren und die Anfangs- und End-Tags eindeutig zuweisbar sein.
	 * @param list
	 * @return
	 */
	public static boolean checkMatchList(TokenList list) {
		//Die Liste muss mindestens drei Elemente besitzen: Start-/Endtag und ein Inhaltselement
		if (list.size() < 4) return false;
		
		//Anfangs- und Endtoken checken und
		//prüfen, ob 2. und vorletztes Token keine SomeTokens sind
		Iterator<Token> it = list.iterator();
		if (!(it.next() instanceof TestElTag)) return false;
		if (it.next() instanceof SomeToken) return false;

		it = list.descendingIterator();
		if (!(it.next() instanceof TestElTag)) return false;
		if (it.next() instanceof SomeToken) return false;

		return true;
	}
	
	
	/**Repräsentation der gesamten Kollektion
	 * @author mkalus
	 *
	 */
	protected class Collection {
		/**TreeMap der Kollektion - der Integerschlüssel spiegelt die Anzahl der inneren Tags
		 * wieder - danach wird die Kollektion geordnet.
		 * TreeMap<Integer,SubCollection> collection 
		 */
		TreeMap<Integer, SubCollection> collection;
		
		/** Konstruktor
		 * 
		 */
		public Collection() {
			collection = new TreeMap<Integer, SubCollection>();
		}

		/**Füge eine Liste zur Kollektion hinzu
		 * @param subList Unterliste, die eingefügt werden soll
		 * @param start StartPosition der Unterliste innerhalb der Liste
		 * @param stop StopPosition der Unterliste innerhalb der Liste
		 * @param completeList Komplette TokenListe
		 * @return
		 */
		public boolean add(SubTokenList subList, TokenList completeList) throws TestelClassifierException {
//			//zuerst prüfen
//			if (!ClassifierCollection.checkMatchList(subList))
//				throw new TestelClassifierException("Liste ist nicht valide mit Tags am Anfang und Ende und anderen Tokens außer SomeTokens:\n" + subList.toString());
//			
			//schauen, ob es eine Unlearn-Liste ist
			Token first = subList.getTokenList().getFirst();
			if (first == null) throw new TestelClassifierException("Unterliste war leer");
			//System.out.println(first);
			String unlearn = first.getAttributeValue("unlearn");
			//falls ja, dann wird auf eine andere Methode umgeleitet
			if (unlearn != null && unlearn.equalsIgnoreCase("yes")) return unlearn(subList, completeList);
			
			int interiorTags = subList.getTokenList().countTestElTags();
			
			//schauen, ob dieser Wert schon als Key im Tree ist
			if (collection.containsKey(interiorTags)) { //ja
				SubCollection coll = collection.get(interiorTags);
				if (coll.add(subList, completeList)) {
					logger.fine("Neuer Klassifizierer hinzugefügt mit dem Typ " + classifierClass + " und " + interiorTags + " inneren Tags");
					return true;
				} else return false;
			} else { //nein: neuen Key erstellen und hinzufügen
				//neuer Klassifizierer
				Classifier classAdd = getNewClassifier();
				classAdd.insertTokenList(subList, completeList); //initialisiere Liste
				//neue Unterkollektion
				collection.put(interiorTags, new SubCollection(classAdd, interiorTags));
				logger.fine("Neuer Klassifizierer hinzugefügt mit dem Typ " + classifierClass + " und " + interiorTags + " inneren Tags");
				return true;
			}
		}
		
		/**Wie oben, nur dass sie sich exkulsiv um "unlearn" Kandidaten kümmert - das Unlearning
		 * betrifft alle Klassifikatoren, die der selben Klasse angehören, wie der Unlearnung-
		 * Klassifizierer
		 * @param subList
		 * @param start
		 * @param stop
		 * @param completeList
		 * @return
		 */
		protected boolean unlearn(SubTokenList subList, TokenList completeList) throws TestelClassifierException {
			boolean learned = false;
			
			Classifier classAdd = getNewClassifier();
			classAdd.insertTokenList(subList, completeList); //initialisiere Liste
			
			//durch alle Einträge iterieren und Matchklassen lernen lassen
			Iterator<Entry<Integer, SubCollection>> collIt = collection.entrySet().iterator();
			
			while (collIt.hasNext()) {
				//SubCollection durchlaufen
				SubCollection subCollection = collIt.next().getValue();
				Iterator<Entry<String, SubSubCollection>> subIt = subCollection.subsubMap.entrySet().iterator();
				
				while (subIt.hasNext()) {
					//SubSubCollection durchlaufen
					SubSubCollection subSubCollection = subIt.next().getValue();
					Iterator<Classifier> it = subSubCollection.classifierList.iterator();
					
					while (it.hasNext()) {
						Classifier classifier = it.next();
						//Jetzt nach Klassifizierern suchen, die der selben Klasse angehören
						//und unlearn lehren
						if (classifier.getClassName().equals(classAdd.getClassName())) {
							classifier.insertTokenList(subList, completeList);
							learned = true;
						}
					}
				}
			}
			
			//Keiner Erweiterungen
			if (!learned) logger.warning("unlearn-Klasse " + classAdd.getClassName() + " fand keine zu trainierenden Klassifizierer");
			
			return learned;
		}

//		/**Sucht in der Kollektion alle Klassifizierer, die mit diesem Element anfangen
//		 * @param startClassName
//		 * @return
//		 */
//		public HashMap<Integer, LinkedList<Classifier>> searchClassifier(String startClassName) {
//			HashMap<Integer, LinkedList<Classifier>> matchList = new HashMap<Integer, LinkedList<Classifier>>();
//			
//			//durch die ganze Kollektion laufen und alle SubCollection abklappern
//			Iterator<Entry<Integer, SubCollection>> it = collection.entrySet().iterator();
//			
//			while (it.hasNext())
//				it.next().getValue().searchClassifier(startClassName, matchList);
//			
//			return matchList;
//		}
	}
	
	
	
	/**Repräsentation einer Unterkollektion, d.h. eine Kollektion, die aufgrund von gemeinsamen
	 * inneren Tags zusammengehalten wird
	 * @author mkalus
	 *
	 */
	protected class SubCollection {
		int interiorTags;
		//Schlüssel hier ist der Start-Key
		HashMap<String, SubSubCollection> subsubMap;
		
		/** Konstruktor
		 * @param classifier
		 * @param interiorTags
		 */
		public SubCollection(Classifier classifier, int interiorTags) {
			subsubMap = new HashMap<String, SubSubCollection>();
			//Schlüssel für initiales Element festlegen
			String key = classifier.getStartTokenRep();
			subsubMap.put(key, new SubSubCollection(classifier));
			//innere Tags
			this.interiorTags = interiorTags;
		}

		/**versucht, eine Liste hinzuzufügen
		 * @param subList Unterliste, die eingefügt werden soll
		 * @param start StartPosition der Unterliste innerhalb der Liste
		 * @param stop StopPosition der Unterliste innerhalb der Liste
		 * @param completeList Komplette TokenListe
		 * @return
		 * @throws TestelClassifierException
		 */
		public boolean add(SubTokenList subList, TokenList completeList) throws TestelClassifierException {
			//Kopie der Token-Liste als Klassifizierer erstellen
			Classifier classAdd = getNewClassifier();
			classAdd.insertTokenList(subList, completeList); //initialisiere Liste
			String startkey = classAdd.getStartTokenRep();
			
			//existiert schon ein Eintrag?
			if (subsubMap.containsKey(startkey)) { //ja -> an SubSubList weitergeben
				return subsubMap.get(startkey).add(subList.getTokenList(),
						classAdd, subList.getStartPosition(), subList.getStopPosition(),
						completeList);
			} else { //nein -> neu hinzufügen
				//Neue Unter-Unterkollektion
				subsubMap.put(startkey, new SubSubCollection(classAdd));
				return true;
			}
		}
		
		/**Gibt die SubSubCollection mit dem richtigen Startkey oder null zurück
		 * @param startKey
		 * @return
		 */
		public SubSubCollection checkStartkey(String startKey) {
			return subsubMap.get(startKey);
		}
	
//		/**Match-Liste erweitern: diese Methode sucht startClassName als StartMerkmal
//		 * und gibt diesen ggf. zurück
//		 * @param startClassName
//		 * @param matchList
//		 */
//		public void searchClassifier(String startClassName, HashMap<Integer, LinkedList<Classifier>> matchList) {
//			SubSubCollection match = subsubMap.get(startClassName);
//			
//			if (match != null) {//falls es einen Treffer gab, wird der Inhalt dieser Liste angehängt
//				LinkedList<Classifier> classfList = match.classifierList;
//				matchList.put(interiorTags, classfList);
//			}
//		}
	}
	
	
	
	/**Repräsentation einer Unter-Unterkollektion, d.h. einer Kollektion, die aufgrund des
	 * gemeinsamen Startmerkmals ausgeprägt ist.
	 * @author mkalus
	 *
	 */
	protected class SubSubCollection {
		String key;
		LinkedList<Classifier> classifierList;
		
		/** Konstruktor
		 * @param classifier Initialer Klassifizierer
		 */
		public SubSubCollection(Classifier classifier) {
			//neue erstellen und hinzufügen
			classifierList = new LinkedList<Classifier>();
			classifierList.add(classifier);
			//Schlüssel festlegen
			this.key = classifier.getStartTokenRep();
		}

		/**Hinzufügen der Liste - es wird geprüft, ob die Klasse und das Start/Stop-Merkmal gleich
		 * ist.
		 * @param subList Unterliste, die eingefügt werden soll
		 * @param classifier die selbe Liste als klassifizierte Liste
		 * @param start StartPosition der Unterliste innerhalb der Liste
		 * @param stop StopPosition der Unterliste innerhalb der Liste
		 * @param completeList Komplette TokenListe
		 * @return
		 * @throws TestelClassifierException
		 */
		public boolean add(TokenList subList, Classifier classifier, int start, int stop, TokenList completeList) throws TestelClassifierException {
			Iterator<Classifier> it = classifierList.iterator();
			
			while (it.hasNext()) {
				Classifier toCompare = it.next();
				//Treffer mit gleichem Anfang/Ende und Klasse suchen
				if (toCompare.isAddable(subList, classifier)) {
					toCompare.insertTokenList(subList, start, stop, completeList); //dazu lernen
					return true;
				}
			}
			
			//nicht gefunden: neue Struktur am Ende eintragen
			classifierList.add(classifier);
			return true;
		}
	}
	
	

	
	
	/**Innere Klasse, die zum matchen verwendet wird
	 * @author mkalus
	 *
	 */
	protected class Matcher {
		/**Wie viele Endtags werden maximal vom Matcher pro Klassifizierer weit nach vorne geschaut?
		 * int maxEndCounters 
		 */
		public final int maxEndCounters;
		
		public Matcher() throws TestelTaggerException {
			try {
				maxEndCounters = Integer.parseInt(Config.getConfig("maxEndCounters"));
			} catch (Exception e) {
				throw new TestelTaggerException("Konnte den maxEndCounters-Wert für den Matcher nicht initialiseren - ist dieser in der Properties-Datei gesetzt?");
			}
		}
		
		/**zentrale Methode des Matchers - wird beim Taggen aufgerufen
		 * @param list
		 * @return
		 * @throws TestelTaggerException
		 */
		public TokenList tag(TokenList list) throws TestelTaggerException {
			//wir iterieren die Liste mehrfach, und zwar in aufsteigender Reihenfolge
			//der Kollektion - d.h. Tags mit wenigen/keinen inneren Tags werden zuerst behandelt,
			//dann die mit jeweils mehr inneren Tags, usw.
			Iterator<Entry<Integer, SubCollection>> collIter = collection.collection.entrySet().iterator();
			
			while (collIter.hasNext()) {
				Entry<Integer, SubCollection> entry = collIter.next();
				logger.info("Behandle mögliche Treffer mit " + entry.getKey() + " inneren TestEl-Tags");
				SubCollection subCollection = entry.getValue();
				
				//jetzt die Liste durchlaufen
				ListIterator<Token> it = list.listIterator();
				while (it.hasNext()) {
					Token tok = it.next();
					if (tok instanceof SomeToken) continue;
					//int startPos = it.nextIndex();
					String startKey = tok.getClassifierName();
					SubSubCollection subSubCollection = subCollection.checkStartkey(startKey);
					//mögliches Match?
					if (subSubCollection != null) {
						//logger.info("Mögliches Match an Position " + tok.getPosition());
						int endPos = workPossibleMatch(subSubCollection, list, it);
						if (endPos != -1) //falls ein Treffer vorlag, Iterator an diese Position
							it = list.listIterator(endPos); //Iterator erneuern
					}
				}
			}
			
			return list;
		}
		
		/**wird von Matcher.tag aufgerufen, wenn mögliche Matches gefunden werden
		 * @param subSubCollection
		 * @param it
		 * @return Iterator-Position nach dem Treffer oder -1 (wir können damit den Iterator in
		 * 	der obigen Methoden weiterlaufen lassen - da Schachtelungen ja ausgeschlossen sind -
		 * Nein: ds stimmt nicht - es können ja neue Schachtelungen auftreten...)
		 */
		private int workPossibleMatch(SubSubCollection subSubCollection, TokenList list, ListIterator<Token> it) throws TestelTaggerException {
			int start = it.previousIndex();
			
			//ist der Rest der Liste überhaupt noch lang genug?
			if (list.size() < start + 1) return -1;
			
			//neuer Iterator zum Suchen des Endes
			//+ 2 deshalb, weil wir das Innenleben nicht weiter betrachten wollen
			ListIterator<Token> stopIt = list.listIterator(start + 2);
			
			int max = 0;
			//counter pro Subcollection
			int count[] = new int[subSubCollection.classifierList.size()];
			
			while (stopIt.hasNext() && max < this.maxEndCounters) {
				Token tok = stopIt.next();
				if (tok instanceof SomeToken) continue;
				String stopKey = tok.getClassifierName();
				
				//stimmt dieses Token mit einem Klassifiziererende aus der Liste überein?
				Iterator<Classifier> clIt = subSubCollection.classifierList.iterator();
				int c = 0;
				while (clIt.hasNext()) {
					Classifier classifier = clIt.next();
					//übereinstimmende Enden finden
					if (classifier.getStopTokenRep().equals(stopKey)) {
						count[c]++;
						if (count[c] > max) max = count[c];
						c++;
						try {
							int endTagPos = classifier.match(list, start, stopIt.nextIndex(), it);
							if (endTagPos != -1) {
								if (classifier.isGreedy() || classifier.noNesting())
									return endTagPos; //zurück mit dem Ende des Iterators
								return it.nextIndex()+2; //zurück - hier können Schachtelungen auftreten +2 weil ja Match-Tags hinzugekommen sind
							}
						} catch (TestelClassifierException e) {
							throw new TestelTaggerException("Fehler beim Taggen - Klassifizierungsfehler beim Endtoken " + tok + ":\n" + e.getLocalizedMessage());
						}
						//System.out.println(tok);
					}
				}
			}
			return -1;
		}
	}
}

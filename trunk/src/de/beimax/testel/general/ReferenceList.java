/**
 * Datei: ReferenceList.java
 * Paket: de.beimax.testel.general
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
package de.beimax.testel.general;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.logging.Logger;

import de.beimax.testel.token.TextPosition;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;
import de.beimax.testel.token.impl.NumberToken;
import de.beimax.testel.token.impl.TestElTag;

/**Klasse zur Verwaltung von Referenzlisten - nutzt zwei weitere innere Klassen unten
 * @author mkalus
 *
 */
public class ReferenceList {
	//Logger
	static final Logger logger = Logger.getLogger(ReferenceList.class.getName());
	
	/**Der Schlüssel ist der Startname des Elements
	 * HashMap<String,ReferenceStartElementList> referenceMap 
	 */
	private HashMap<String, ReferenceStartElementList> referenceMap;
	
	/** Konstruktor
	 * 
	 */
	public ReferenceList() {
		referenceMap = new HashMap<String, ReferenceStartElementList>();
	}
	
	/**Fügt eine TokenListe ein, falls keine vergleichbare Liste vorhanden ist
	 * @param tokenList
	 * @return true, falls Liste eingefügt wurde
	 */
	public boolean add(TokenList tokenList, String type) {
		//Assert
		if (tokenList == null || tokenList.isEmpty()) return false;
		
		String startName = tokenList.getFirst().getName();
		
		//gibt es schon einen Startschlüssel?
		if (referenceMap.containsKey(startName)) { //ja
			ReferenceStartElementList checker = referenceMap.get(startName);
			return checker.add(tokenList, type);
		} else { //nein -> neuen erstellen
			ReferenceStartElementList toAdd = new ReferenceStartElementList(startName);
			toAdd.add(tokenList, type);
			referenceMap.put(startName, toAdd);
			return true;
		}
	}

	/**Treffer suchen
	 * @param list
	 * @param tok
	 * @param it
	 * @return
	 */
	public boolean match(TokenList list, Token tok, ListIterator<Token> it) {
		ReferenceStartElementList startList = referenceMap.get(tok.getName());
		
		if (startList != null)
			return startList.match(list, tok, it);
		
		return false;
	}
	
	
	
	/**Diese innere Klasse sammelt Listen mit dem selben Startelement, um das Tagging zu vereinfachen
	 * @author mkalus
	 *
	 */
	protected class ReferenceStartElementList {
		private String startElementName;
		private LinkedList<Reference> referenceList;
		
		/** Konstruktor
		 * @param startElementName
		 */
		public ReferenceStartElementList(String startElementName) {
			this.startElementName = startElementName;
			referenceList = new LinkedList<Reference>();
		}
		
		/** Getter für startElementName
		 * @return
		 */
		public String getStartElementName() {
			return startElementName;
		}
		
		/**Fügt eine TokenListe ein, falls keine vergleichbare Liste vorhanden ist
		 * @param tokenList
		 * @return true, falls Liste eingefügt wurde
		 */
		public boolean add(TokenList tokenList, String type) {
			Iterator<Reference> it = referenceList.iterator();
			
			while (it.hasNext()) {
				Reference ref = it.next();
				TokenList list = ref.tokenList;
				if (list.hasSameTextContent(tokenList)) {
					if (!type.equals(ref.getClassName()))
						logger.warning("Liste " + tokenList + " wurde nicht eingetragen, da hierfür schon ein Eintrag mit dem Typ " +
								ref.getClassName() + " vorhanden ist - der neue Typ sollte laut Benutzer jedoch " + type + " sein...");
					return false;
				}
			}
			
			referenceList.add(new Reference(type, tokenList));
			
			return true;
		}

		/**Treffer suchen - der Treffer, der die meisten Tokens abdeckt, wird genommen.
		 * @param list
		 * @param tok
		 * @param it
		 * @return
		 */
		public boolean match(TokenList list, Token tok, ListIterator<Token> it) {
			//Match-Liste aufstellen
			LinkedList<Reference> matchList = new LinkedList<Reference>();
			
			//Matches finden
			Iterator<Reference> iter = referenceList.iterator();
			int position = it.previousIndex();
			
			while (iter.hasNext()) {
				Reference possibleMatch = iter.next();
				if (possibleMatch.match(list, position)) //Treffer?
					matchList.add(possibleMatch);
			}
			
			//Keine Treffer -> nix zurückgeben
			if (matchList.isEmpty()) return false;
			
			if (matchList.size() == 1) //ein Treffer: einfach
				applyMatch(matchList.getFirst(), it);
			else { //mehrere Treffer - hier den längsten heraussuchen
				int max = 0;
				Reference maxRef = null;
				Iterator<Reference> miter = matchList.iterator();
				while (miter.hasNext()) {
					Reference check = miter.next();
					if (check.tokenList.size() > max) {
						max = check.tokenList.size();
						maxRef = check;
					}
				}
				if (maxRef == null)
					logger.severe("ACHTUNG: maxRef war null, obwohl das nicht passieren sollte");
				applyMatch(maxRef, it);
			}

			return true;
		}
		
		/**Wendet einen Match an - d.h. ändert die Liste
		 * @param reference
		 * @param it
		 */
		protected void applyMatch(Reference reference, ListIterator<Token> it) {
			String className = reference.getClassName();
			
			//neues Starttag erstellen
			TextPosition pos = it.previous().getTextPosition();
			TestElTag startTag = new TestElTag("ref", className);
			startTag.initTextPosition(pos.getBpos(), pos.getBrow(), pos.getBcol(), pos.getBpos(), pos.getBrow(), pos.getBcol());
			it.add(startTag);
			
			for (int i = 0; i < reference.tokenList.size(); i++)
				pos = it.next().getTextPosition();
			
			//EndTag erstellen
			TestElTag stopTag = new TestElTag("/ref", className);
			stopTag.initTextPosition(pos.getEpos(), pos.getErow(), pos.getEcol(), pos.getEpos(), pos.getErow(), pos.getEcol());
			//einfügen
			it.add(stopTag);
			
			//Referenzen erstellen
			startTag.setReference(stopTag);
			stopTag.setReference(startTag);
			//System.out.println(lastTok);
		}
	}

	
	
	/**Innere Klasse zur Darstellung einzelner Referenzen
	 * @author mkalus
	 *
	 */
	protected class Reference {
		private String className;
		private TokenList tokenList;
		
		/** Konstruktor
		 * @param className
		 * @param tokenList
		 */
		public Reference(String className, TokenList tokenList) {
			this.className = className;
			this.tokenList = tokenList;
			forgetStuff(); //überflüssige Informationen entfernen
		}
		
		/** Getter für className
		 * @return
		 */
		public String getClassName() {
			return className;
		}

		/**Ruft hasSameTextContent von beiden Listen auf
		 * @param compareList
		 * @return
		 */
		public boolean hasSameTextContent(TokenList compareList) {
			return tokenList.hasSameTextContent(compareList);
		}
		
		/**Ruft hasSameTextContent von beiden Listen auf
		 * @param compareList
		 * @return
		 */
		public boolean hasSameTextContent(Reference reference) {
			return tokenList.hasSameTextContent(reference.tokenList);
		}
		
		/**
		 * wird vom Konstruktor aufgerufen, um überflüssige Informationen aus der TokenList
		 * (Positionen, Referenzen) zu entfernen
		 */
		private void forgetStuff() {
			Iterator<Token> it = tokenList.iterator();
			
			while (it.hasNext()) {
				Token tok = it.next();
				tok.forgetTextPosition();
				tok.setReference(null);
			}
		}
		
		/**Treffer suchen - Iterator wird vorerst nicht verändert
		 * @param list
		 * @param startPosition
		 * @return
		 */
		public boolean match(TokenList list, int startPosition) {
			//Plausibilität checken
			int listSize = tokenList.size();
			//überhaupt noch so lang, der Rest der Liste?
			if (list.size() - startPosition < listSize) return false;
			
			//Iteratorer an dieser Stelle erstellen und Listen vergleichen
			ListIterator<Token> it1 = list.listIterator(startPosition);
			ListIterator<Token> it2 = tokenList.listIterator();
			
			for (int i = 0; i < listSize; i++) {
				Token t1 = it1.next();
				Token t2 = it2.next();
				//Sonderfall: Zahlen -> werden nicht überprüft
				if (t1 instanceof NumberToken && t2 instanceof NumberToken) continue;
				if (!t1.getName().equals(t2.getName())) return false;
			}
			//niemand hat widersprochen -> ok zurück
			return true;
		}
	} //protected class Reference Ende
	
}

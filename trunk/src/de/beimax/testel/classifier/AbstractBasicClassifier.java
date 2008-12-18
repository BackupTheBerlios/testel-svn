/**
 * Datei: AbstractBasicClassifier.java
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.ListIterator;

import de.beimax.testel.exception.TestelClassifierException;
import de.beimax.testel.token.SubTokenList;
import de.beimax.testel.token.TextPosition;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;
import de.beimax.testel.token.impl.SomeToken;
import de.beimax.testel.token.impl.TestElTag;

/**Abstrakte Klasse für Klassifizierer - diese bietet einige wichtige Basisfunktionen zur
 * Eingabe und Analyse von TokenListen an...
 * @author mkalus
 *
 */
public abstract class AbstractBasicClassifier implements Classifier {
	/**Name des Klassifizierers, z.B. ÜBERSCHRIFT
	 * String className 
	 */
	private String className;

	/**
	 * start- und stop-Attribute des Tags
	 */
	private String startAttr, stopAttr;
	
	/**
	 * Boolsche Werte
	 */
	private boolean outer, exclStart, exclStop, showTag, greedy, noNesting;
	
	/**Repräsentation der Start- und Stop-Tokens
	 * String startTokenRep 
	 */
	protected String startTokenRep, stopTokenRep;
	
	/** Getter für className
	 * @return className
	 */
	public String getClassName() {
		return className;
	}

	/** Setter für className
	 * @param className Festzulegender className
	 */
	public void setClassName(String className) {
		this.className = className.toUpperCase();
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#getStartTokenRep()
	 */
	public String getStartTokenRep() {
		return startTokenRep;
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#getStopTokenRep()
	 */
	public String getStopTokenRep() {
		if (stopTokenRep == null) return getStartTokenRep();
		return stopTokenRep;
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#getStartStopTokenRep()
	 */
	public String getStartStopTokenRep() {
		return getStartTokenRep() + "__" + getStopTokenRep();
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#isGreedy()
	 */
	public boolean isGreedy() {
		return this.greedy;
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#noNesting()
	 */
	public boolean noNesting() {
		return this.noNesting;
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#insertTokenList(de.beimax.testel.token.SubTokenList, de.beimax.testel.token.TokenList)
	 */
	public void insertTokenList(SubTokenList subList, TokenList completeList) throws TestelClassifierException {
		insertTokenList(subList.getTokenList(), subList.getStartPosition(),
				subList.getStopPosition(), completeList);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#insertTokenList(de.beimax.testel.token.TokenList, int, int, de.beimax.testel.token.TokenList)
	 */
	public void insertTokenList(TokenList subListIn, int start, int stop, TokenList completeList) throws TestelClassifierException {
		//Kopie anfertigen von der Lsite
		TokenList subList = subListIn.copy();
		
		//zuerst prüfen, ob das Ding eine valide Liste ist
		if (!ClassifierCollection.checkMatchList(subList))
			throw new TestelClassifierException("Liste ist nicht valide! Sind in der folgenden Liste Matches am Anfang und am Ende und andere Tokens außer SomeToken von diesen eingerahmt?\n" + subList.toString());
		
		//Ok, dann können wir mal mit der Verarbeitung der Liste beginnen
		//erstes und letztes Token extrahieren
		Token first = subList.pollFirst();
		Token last = subList.pollLast();
		
		//Vergleich der beiden Tags - sind sie gleich?
		if (last.compare(first) == 0)
			throw new TestelClassifierException("Liste ist nicht valide, da Anfangs und Endtokens nicht übereinstimmen:\nAnfangstoken: " + first + "\nEndtoken: " + last);
		
		//negatives Beispiel?
		boolean unlearn = getTestElTagCommandValue(first.getAttributeValue("unlearn"));
		
		//wurde der Klassifizierer schon initialisiert?
		if (startTokenRep == null) {//neu
			//Namen setzen
			setClassName(first.getClassName());
			
			//erstes Tag nach speziellen Regeln abklopfen
			parseTestElTagCommands(first);
			
			//Anfangs- und End-Token herausziehen und bereinigen
			this.startTokenRep = subList.pollFirst().getClassifierName();
			if (subList.isEmpty()) this.stopTokenRep = null; //falls Liste jetzt leer...
			else this.stopTokenRep = subList.pollLast().getClassifierName();
		} else { //nur Start und End-Token herausziehen
			subList.pollFirst();
			subList.pollLast();
		}
		
		//Liste auf Match abgleichen
		SubTokenList subTokenList = getInnerOuterListToMatch(subList, start+2, stop-2, completeList);
		
		//Darf hier nicht passieren: Leere Rückgabe
		if (subTokenList == null)
			throw new TestelClassifierException("Match-Anweisung erzeugte eine leere Trefferliste: " + first);
		//Unterliste vom eingeschränkten Treffer übernehmen
		subList = subTokenList.getTokenList();
		//System.out.println(subList);

		//Rest der Liste wird vereinfacht und klassifiziert
		subList = simplifyList(subList);
		
		classifyList(subList, unlearn);
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#isAddable(de.beimax.testel.token.TokenList)
	 */
	public boolean isAddable(TokenList list, Classifier classifier) {
		//Start- und EndToken sind vergleichbar?
		if (!getStartStopTokenRep().equals(classifier.getStartStopTokenRep())) return false;
		//Klassenname gleich
		if (!getClassName().equals(classifier.getClassName())) return false;
		//andere Parameter vergleichen
		Token token = list.getFirst();
		try {
			if (getTestElTagCommandValue(token.getAttributeValue("outer")) != this.outer) return false;
			if (getTestElTagCommandValue(token.getAttributeValue("exclstart")) != this.exclStart) return false;
			if (getTestElTagCommandValue(token.getAttributeValue("exclstop")) != this.exclStop) return false;
			if (getTestElTagCommandValue(token.getAttributeValue("exclstop")) != this.exclStop) return false;
			if (getTestElTagCommandValue(token.getAttributeValue("donotshow")) == this.showTag) return false;
			if (getTestElTagCommandValue(token.getAttributeValue("greedy")) != this.greedy) return false;
			if (getTestElTagCommandValue(token.getAttributeValue("nonesting")) != this.noNesting) return false;
		} catch (TestelClassifierException e) {
			logger.warning("Fehler beim Parsen von " + token);
			return false;
		}
		return true;
	}
	
	/**Übernimmt das erste Token und prüft es auf TestEl-Tag-Kommandos und speichert diese
	 * @param token
	 */
	public void parseTestElTagCommands(Token token) throws TestelClassifierException {
		//Attributwerte übernehmen
		this.startAttr = token.getAttributeValue("start");
		this.stopAttr = token.getAttributeValue("stop");
		try {
			this.outer = getTestElTagCommandValue(token.getAttributeValue("outer"));
		} catch (TestelClassifierException e) {
			throw new TestelClassifierException("Fehler im Attribut 'outer' in Token " + token + ":\n" + e.getLocalizedMessage());
		}
		try {
			this.exclStart = getTestElTagCommandValue(token.getAttributeValue("exclstart"));
		} catch (TestelClassifierException e) {
			throw new TestelClassifierException("Fehler im Attribut 'exclstart' in Token " + token + ":\n" + e.getLocalizedMessage());
		}
		try {
			this.exclStop = getTestElTagCommandValue(token.getAttributeValue("exclstop"));
		} catch (TestelClassifierException e) {
			throw new TestelClassifierException("Fehler im Attribut 'exclstop' in Token " + token + ":\n" + e.getLocalizedMessage());
		}
		try { //hier umkehren
			this.showTag = !getTestElTagCommandValue(token.getAttributeValue("donotshow"));
		} catch (TestelClassifierException e) {
			throw new TestelClassifierException("Fehler im Attribut 'donotshow' in Token " + token + ":\n" + e.getLocalizedMessage());
		}
		try {
			this.greedy = getTestElTagCommandValue(token.getAttributeValue("greedy"));
		} catch (TestelClassifierException e) {
			throw new TestelClassifierException("Fehler im Attribut 'greedy' in Token " + token + ":\n" + e.getLocalizedMessage());
		}
		try {
			this.noNesting = getTestElTagCommandValue(token.getAttributeValue("nonesting"));
		} catch (TestelClassifierException e) {
			throw new TestelClassifierException("Fehler im Attribut 'nonesting' in Token " + token + ":\n" + e.getLocalizedMessage());
		}
		//unlearn wird nur einmal gebraucht - siehe oben (insertTokenList)
	}
	
	private boolean getTestElTagCommandValue(String attr) throws TestelClassifierException {
		if (attr == null) return false; //default-Wert
		if (attr.equalsIgnoreCase("yes")) return true;
		if (attr.equalsIgnoreCase("no")) return false;
		throw new TestelClassifierException("Attributwert darf nur 'yes' oder 'no' sein");
	}
	
	/**Durchläuft die Liste und löscht doppelte Vorkommen von SomeTokens
	 * @param list
	 * @return
	 * @throws TestelClassifierException
	 */
	protected TokenList simplifyListSomeTokens(TokenList list) throws TestelClassifierException {
		Iterator<Token> it = list.iterator();
		Token last = it.next();
		if (last instanceof SomeToken)
			last.setName("ANY");
		
		//SomeTokens zusammenschrumpfen
		while (it.hasNext()) {
			Token curr = it.next();
			if (curr instanceof SomeToken) //Text von SomeTokens ist egal...
				curr.setName("ANY");
			//falls letztes und dieses Some-Tokens sind, dann dieses hier löschen
			if (curr instanceof SomeToken && last instanceof SomeToken)
				it.remove();
			else last = curr;
		}
		
		return list;
	}

//Bringt nur Ärger...
//	/**Durchläuft die Liste und vereinfacht Vorkommen von TestElTags auf den Anfang des Tags
//	 * @param list
//	 * @return
//	 * @throws TestelClassifierException
//	 */
//	//Löschen/ändern
//	protected TokenList simplifyListTestElTags(TokenList list) throws TestelClassifierException {
//		Iterator<Token> it = list.iterator();
//		
//		boolean withinTestEl = false;
//		Token compare = null;
//		while (it.hasNext()) {
//			Token tok = it.next();
//			if (withinTestEl) { //Ende suchen
//				if (tok.compare(compare) == 1) { //Ende gefunden
//					withinTestEl = false;
//					it.remove();
//				} else
//					it.remove(); //Tokens innen löschen
//			} else if (tok instanceof TestElTag) { //Anfangstoken suchen
//				withinTestEl = true;
//				compare = tok;
//			}
//		}
//		
//		return list;
//	}
//
	/**Übernimmt die vereinfachte (innere) Liste ohne Anfangs- und End-Tags und klassifiziert
	 * diese
	 * @param list
	 * @param unlearn true, um negatives Lernen zu aktivieren
	 * @throws TestelClassifierException
	 */
	public abstract void classifyList(TokenList list, boolean unlearn) throws TestelClassifierException;
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#getInnerOuterListToMatch(de.beimax.testel.token.TokenList, int, int, de.beimax.testel.token.TokenList)
	 */
	public SubTokenList getInnerOuterListToMatch(TokenList subListIn, int start, int stop, TokenList completeList) throws TestelClassifierException {
		if (outer) { //outer wurde gesetzt
			//Startverschiebung feststellen
			int moveStart = getPositionOffsetByStartStopAttribute(completeList, this.startAttr, start, true);
			int moveStop = getPositionOffsetByStartStopAttribute(completeList, this.stopAttr, stop, false);

			//mehr Elemente zu entfernen als in der Liste sind? -> kein Treffer!
			//genauso: falls searchStartStopToken -1 zurückgegeben hat
			if (moveStart == -1 || moveStop == -1) return null;

			start = start - moveStart;
			stop = stop + moveStop;

			if (start < 0 || stop >= subListIn.size() -1) return null;
			
			TokenList subList = completeList.subList(start, stop).copy();
			
			return new SubTokenList(subList, start, stop);
		} else { //Grenzen innerhalb der Liste ausloten
			//Kopie der Liste
			TokenList subList = subListIn.copy();

			//Startverschiebung feststellen
			int moveStart = getPositionOffsetByStartStopAttribute(subList, this.startAttr, 0, false);
			int moveStop = getPositionOffsetByStartStopAttribute(subList, this.stopAttr, 0, true);
			
			//mehr Elemente zu entfernen als in der Liste sind? -> kein Treffer!
			//genauso: falls searchStartStopToken -1 zurückgegeben hat
			if (moveStart == -1 || moveStop == -1 || moveStart + moveStop >= subList.size()) return null;

			//jetzt von der Unterliste x Startelemente entfernen
			for (int i = 0; i < moveStart; i++)
				subList.pollFirst();
			for (int i = 0; i < moveStop; i++)
				subList.pollLast();
			//Treffer generieren
			//System.out.println(completeList.subList(start + moveStart, stop - moveStop));
			start = start + moveStart;
			stop = stop - moveStop;
			return new SubTokenList(subList, start, stop);
		}
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#match(de.beimax.testel.token.TokenList, int, int, java.util.ListIterator)
	 */
	//eigentliche Match-Methode
	public int match(TokenList list, int startPos, int stopPos, ListIterator<Token> it) throws TestelClassifierException {
		return match(list, startPos, stopPos, it, true);
	}
	
	/**eigentlicher Match-Aufruf - unterscheidet sich durch einen zusätzlichen Parameter
	 * allowGreedy, der nur intern gestzt wird, um rekursive greedy-Aufrufe zu vermeiden
	 * @param list
	 * @param startPos
	 * @param stopPos
	 * @param it
	 * @param allowGreedy
	 * @return
	 * @throws TestelClassifierException
	 */
	public int match(TokenList list, int startPos, int stopPos, ListIterator<Token> it, boolean allowGreedy) throws TestelClassifierException {
		//Bei greedy Klassifizierern muss etwas anders vorgegangen werden...
		if (allowGreedy && isGreedy()) {
			//Die Idee ist folgende: wir laufen vom Ende der Liste her die Tokens ab
			//und schauen, ob ein mögliches EndToken gefunden wird - dann wird geprüft, ob
			//hier ein mögliches Match vorliegen könnte, das verwendet werden kann. Wenn ja,
			//wird dieses Match durchgeführt und die die Endposition zurückgegeben.
			Iterator<Token> endIt = list.descendingIterator();
			int pos = list.size(); //da descendingIterator keinen Zähler besitzt
			
			Token tok = null;
			while (endIt.hasNext() && pos >= stopPos) {
				tok = endIt.next();
				if (stopTokenRep.equals(tok.getClassifierName())) {
					int endpos = match(list, startPos, pos, it, false);
					if (endpos != -1) return endpos;
				}
				pos--;
			}
		}

		try {
			//Unterliste bauen
			TokenList subList;
			try {
				subList = list.subList(startPos, stopPos);
				//System.out.println(subList);
			} catch (RuntimeException e) {
				throw new TestelClassifierException("Fehlerhafte Erstellung einer Unterliste bei Token-Positionen " +
						startPos + "/" + stopPos + ":\n" + e.getLocalizedMessage());
			}
			//schon ein Match der selben Klasse hier?
			if (startPos > 1) {
				Token check = list.get(startPos-2);
				if (check instanceof TestElTag && getClassName().equals(check.getClassName()))
					return -1; //kein zusätzliches Match erzeugen, da wir hier schon eines haben
			}
			
			//bei Verschachtelungsverbot: Verschachtelungen verbieten
			if (noNesting() && checkNesting(list, startPos)) return -1;
			
			//Verschachtelungen - kommt das Starttag innerhalb der Unterliste schon vor?
			if (matchOpenEndTags(subList)) return -1; //falls ja, dann zurück
			
			//Anfangs- und Endtoken der Liste anpassen
			SubTokenList subTokenList = getInnerOuterListToMatch(subList, startPos, stopPos, list);
			
			//Kein Treffer?
			if (subTokenList == null) return -1;
			
			//ist die Liste schon ausgezeichnet?
			if (subTokenList.getStartPosition() > 1) {
				Token check = subTokenList.getTokenList().getFirst();
				if (check instanceof TestElTag && getClassName().equals(check.getClassName()))
					return -1; //kein zusätzliches Tag erzeugen, da wir hier schon eines haben
				//Token davor überprüfen
				ListIterator<Token> iterator = list.listIterator(subTokenList.getStartPosition()-1);
				
				while (iterator.hasPrevious()) {
					check = iterator.previous();
					if (check instanceof TestElTag) { //solange es TestElTags sind
						if (getClassName().equals(check.getClassName())) return -1;
					} else break; //sonst unterbrechen
				}
			}

//			if (subTokenList.getStartPosition() > 1) {
//				Token check = subTokenList.getTokenList().getFirst();
//				if (check instanceof TestElTag && getClassName().equals(check.getClassName()))
//					return -1; //kein zusätzliches Tag erzeugen, da wir hier schon eines haben
//				check = list.get(subTokenList.getStartPosition()-2);
//				if (check instanceof TestElTag && getClassName().equals(check.getClassName()))
//				return -1; //kein zusätzliches Tag erzeugen, da wir hier schon eines haben
//				//eines davor nachsehen
//				if (subTokenList.getStartPosition() > 0) {
//					check = list.get(subTokenList.getStartPosition() - 1);
//					if (check instanceof TestElTag && getClassName().equals(check.getClassName()))
//						return -1; //kein zusätzliches Tag erzeugen, da wir hier schon eines haben
//				}
//			}
			
			//Matching durchführen
			if (!classifierMatchesSubList(subTokenList.getTokenList())) return -1; //kein Treffer
			
			//Treffer durch Tags erkenntlich machen
			//insertStartEndTags(list, startPos - 1, stopPos +1, subTokenList.getStartPosition(), subTokenList.getEndPosition());
			
			//Positionen anpassen
			int tagstart, tagend;
			if (this.exclStart) tagstart = subTokenList.getStartPosition() + 1;
			else tagstart = subTokenList.getStartPosition();
			if (this.exclStop) tagend = subTokenList.getStopPosition();
			else tagend = subTokenList.getStopPosition();
			
			//TestEl-Tags erzeugen
			TestElTag startTag = new TestElTag("tag", getClassName());
			TextPosition pos = subTokenList.getTokenList().getFirst().getTextPosition();
			startTag.initTextPosition(pos.getBpos(), pos.getBrow(), pos.getBcol(), pos.getBpos(), pos.getBrow(), pos.getBcol());
			if (!this.showTag) startTag.addAttribute("donotshow", "yes");
			TestElTag stopTag = new TestElTag("/tag");
			pos = subTokenList.getTokenList().getLast().getTextPosition();
			stopTag.initTextPosition(pos.getEpos(), pos.getErow(), pos.getEcol(), pos.getEpos(), pos.getErow(), pos.getEcol());
			startTag.setReference(stopTag);
			stopTag.setReference(startTag);
			
			//Matches erzeugen
			TestElTag startMatch = new TestElTag("match", getClassName());
			pos = subList.getFirst().getTextPosition();
			startMatch.initTextPosition(pos.getBpos(), pos.getBrow(), pos.getBcol(), pos.getBpos(), pos.getBrow(), pos.getBcol());
			//Attribute
			if (this.startAttr != null) startMatch.addAttribute("start", startAttr);
			if (this.stopAttr != null) startMatch.addAttribute("stop", stopAttr);
			if (this.outer) startMatch.addAttribute("outer", "yes");
			if (this.exclStart) startMatch.addAttribute("exclstart", "yes");
			if (this.exclStop) startMatch.addAttribute("exclstop", "yes");
			if (!this.showTag) startMatch.addAttribute("donotshow", "yes");
			if (this.greedy) startMatch.addAttribute("greedy", "yes");
			if (this.noNesting) startMatch.addAttribute("nonesting", "yes");
			
			TestElTag stopMatch = new TestElTag("/match");
			pos = subList.getLast().getTextPosition();
			stopMatch.initTextPosition(pos.getEpos(), pos.getErow(), pos.getEcol(), pos.getEpos(), pos.getErow(), pos.getEcol());
			startMatch.setReference(stopMatch);
			stopMatch.setReference(startMatch);
			
			//so, jetzt von hinten her einfügen
			if (tagend <= stopPos) { //erst Match-Ende einfügen
				list.add(stopPos, stopMatch);
				list.add(tagend, stopTag);
			} else {
				list.add(tagend, stopTag);			
				list.add(stopPos, stopMatch);
			}
			if (tagstart >= startPos) { //erst Tag-Start einfügen
				list.add(tagstart, startTag);
				list.add(startPos, startMatch);
			} else {
				list.add(startPos, startMatch);			
				list.add(tagstart, startTag);
			}
			
			logger.info("Treffer eingefügt: " + getClassName() + ", Positionen start=" + startTag.getTextPosition() + ", stop=" + stopTag.getTextPosition());

			return tagend;
		} catch (Exception e) {
			StringWriter w = new StringWriter();
			e.printStackTrace(new PrintWriter(w));
			String msg = w.toString();
			throw new TestelClassifierException("Fehler beim Match eines " + getClassifierName() + "/" + getClassName() + " Klassifizierers:\n" + msg);
		}
	}
	
	//Verschachtelung prüfen
	private boolean checkNesting(TokenList list, int startPos) {
		if (startPos < 1) return false;
		ListIterator<Token> it = list.listIterator(startPos-1);
		
		while (it.hasPrevious()) { //vorhergehende Tokens prüfen
			Token tok = it.previous();
			if (!(tok instanceof TestElTag)) continue;
				
			//Endtag des selben Typs gefunden - weg!
			if (tok.getName().charAt(0) == '/' && this.className.equals(tok.getReference().getClassName()))
				return false;
			//einschließendes Tag gefunden...
			if ((tok.getName().equals("tag") || tok.getName().equals("match")) && this.className.equals(tok.getClassName())) return true;
		}
		
		return false;
	}
	
	/**Schaut, ob innerhalb einer Liste öffnende und schließende Tags vorkommen, die hier
	 * eine Rolle spielen könnten
	 * @param subList
	 * @return
	 */
	private boolean matchOpenEndTags(TokenList subList) {
		//die Idee ist, die Liste rückwärts zu durchlaufen und nach Enden zu suchen - diese
		//zählen positiv, denn Anfänge zählen negativ, d.h. bei einer negativen Gesamtsumme
		//wird das Tagging abgebrochen
		
		int counter = 0;
		Iterator<Token> it = subList.descendingIterator();
		
		while (it.hasNext()) {
			String rep = it.next().getClassifierName();

			if (this.stopTokenRep != null && this.stopTokenRep.equals(rep)) counter++; //positives Ende
			else if (this.startTokenRep != null && this.startTokenRep.equals(rep)) { //negativer Anfang
				//System.out.println(rep);
				if (counter <= 0) return true;
				counter--;
			}
		}
		
		return false;
	}
	
	/**
	 * Typen von Start oder Stop-Attributen
	 * (nummerisch ist positive Ganzzahl)
	 */
	public static final int A_NONE = -1;		//leer
	public static final int A_TOKEN = -2;		//z.B. "MARKUP"
	public static final int A_TOKENNAME = -3;	//z.B. "MARKUP:p"
	public static final int A_TOKENCLASS = -4;	//z.B. "MARKUP:p"
	public static final int A_MULTI = -5;		//z.B. "PUNCTUATION MARKUP:/p"
	
	private int getPositionOffsetByStartStopAttribute(TokenList list, String attr, int offset, boolean backward) throws TestelClassifierException {
		//muss der Start verschoben werden?
		int type = getStartStopAttributeType(attr);
		switch (type) {
		case A_NONE: //nichts machen
			return 0;
		case A_TOKEN: //nur Token suchen
			return searchStartStopToken(list, offset, attr, null, null, backward);
		case A_TOKENNAME: //Token und Namen suchen
			try {
				String tokenType = attr.substring(0, attr.indexOf(':'));
				String tokenName = attr.substring(attr.indexOf(':')+1);
				return searchStartStopToken(list, offset, tokenType, tokenName, null, backward);
			} catch (RuntimeException e) {
				throw new TestelClassifierException("Fehler beim Auflösen des Start-/Stop-Attributs " + attr + ":\n" + e.getLocalizedMessage());
			}
		case A_TOKENCLASS: //Token und Klasse suchen
			try {
				String tokenType = attr.substring(0, attr.indexOf('='));
				String tokenClass = attr.substring(attr.indexOf('=')+1);
				return searchStartStopToken(list, offset, tokenType, null, tokenClass, backward);
			} catch (RuntimeException e) {
				throw new TestelClassifierException("Fehler beim Auflösen des Start-/Stop-Attributs " + attr + ":\n" + e.getLocalizedMessage());
			}
		case A_MULTI:
			//Trick ist hier, nach dem ersten Token zu suchen und dann die anderen Token danach zu checken
			System.out.println("MultiTags IMPLEMENTIEREN"); //TODO
			System.exit(0);
			break;
		default:
			return type;
		}
		return -1;
	}
	
	/**Prüft Start-/Stop-Attribut auf Typ
	 * @param attr
	 * @return A_NONE, A_TOKEN, A_TOKENNAME, A_MULTI oder positive Zahl, falls nummerisch
	 */
	private int getStartStopAttributeType(String attr) throws TestelClassifierException {
		if (attr == null || attr.length() == 0) return A_NONE;
		
		//auf Zahl testen
		try {
			int num = Integer.parseInt(attr);
			if (num <= 0) throw new TestelClassifierException("Start-/Stop-Wert muss positive Ganzzahl sein (war aber " + num + ")");
			return num;
		} catch (NumberFormatException e) {}
		
		//Multis haben Trennzeichen
		if (attr.trim().indexOf('|') > -1) return A_MULTI;
		
		//Token + Klasse haben =
		if (attr.trim().indexOf('=') > -1) return A_TOKENCLASS;
		
		//Token + Name haben :
		if (attr.trim().indexOf(':') > -1) return A_TOKENNAME;
		
		//alle anderen Fälle
		return A_TOKEN;
	}
	
	/**Sucht nach bestimmten Token innerhalb der Liste
	 * @param list Suchliste
	 * @param offset so viele Tokens werden am Anfang übersprungen (bei Rückwärtssuche ist das
	 * 	die Anzahl der Tokens vom Ende!)
	 * @param tokenType Typ des Tokens, z.B. MARKUP
	 * @param tokenName Name des Tokens
	 * @param tokenClass Klasse des Tokens, z.B. DOPPELPUNKT
	 * @param backward true, falls rückwärts gesucht wird
	 * @return Anzahl der Tokens die übersprungen wurden, bis das richtige gefunden wurde oder -1
	 * 	falls kein Treffer gefunden wurde
	 */
	private int searchStartStopToken(TokenList list, int offset, String tokenType, String tokenName, String tokenClass, boolean backward) {
		//Vorwärts- oder Rückwärts-Iterator holen
		Iterator<Token> it;
		if (backward) it = list.descendingIterator();
		else it = list.iterator();
		
		//# Offset überspringen
		for (int i = 0; i < offset; i++)
			if (it.hasNext()) it.next();
			else return -1; //falls das Ende der Liste vorzeitig erreicht wurde
		
		//Token suchen
		int count = 0;
		while (it.hasNext()) {
			count++;
			Token tok = it.next();
			//Ausschlusskriterien
			if (tokenType != null && !tok.getType().equals(tokenType)) continue;
			if (tokenName != null && !tok.getName().equals(tokenName)) continue;
			if (tokenClass != null && !tok.getClassName().equals(tokenClass)) continue;
			//Treffer:
			return count-1;
		}
		
		return -1;
	}
}

/**
 * Datei: Trainer.java
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Logger;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.classifier.Classifier;
import de.beimax.testel.exception.TestelClassifierException;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.lang.AbbreviationSubTagger;
import de.beimax.testel.lang.AbstractNumberParser;
import de.beimax.testel.lang.MeaningfulWordsSubTagger;
import de.beimax.testel.lang.PunctuationSubTagger;
import de.beimax.testel.token.SubTokenList;
import de.beimax.testel.token.TextPosition;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;
import de.beimax.testel.token.impl.NumberToken;
import de.beimax.testel.token.impl.PunctuationToken;
import de.beimax.testel.token.impl.SomeToken;
import de.beimax.testel.token.impl.TestElTag;
import de.beimax.testel.token.impl.TextToken;

/**
 * @author mkalus
 *
 */
public class Trainer {
	//Logger
	static final Logger logger = Logger.getLogger(Trainer.class.getName());

	protected TestElHandler handler;

	/** Konstruktor
	 * @param handler
	 */
	public Trainer(TestElHandler handler) {
		this.handler = handler;
	}
	
	/**Das Training ist in drei Phasen geteilt: In der ersten werden bedeutungstragende
	 * Wörter, Abkürzungen, Punktuation und Nummern ausgelesen und verwertet. Am Ende werden
	 * diese normalisiert. Dann werden Referenzen bearbeitet und als letzte Phase Textstrukturen.
	 * @param tokenList
	 * @throws TestelException
	 */
	public void train(TokenList list) throws TestelException {
		//1. Phase:
		list = trainLangStuff(list);
		//2. Phase:
		list = trainReferences(list);
		//3. Phase:
		list = trainTextStructures(list);
		
		//Benutzer informieren
		String msg;
		if (handler.isDryrun())
			msg = "Keine Daten gespeichert, da --dryrun gesetzt war";
		else msg = "Daten wurden erfolgreich gespeichert - Corpus erweitert";

		logger.info(msg);
		if (!handler.isQuiet())
			System.out.println(msg);
	}
	
	/**Phase 1 des Trainings: Extrahiere bedeutungstragende Wörter, Abkürzungen, Punktuation
	 * und Nummern und lerne sie. Dann normaliesiere die Liste, so dass weitere Phasen nicht
	 * mit Tags von den genannten Elementen zu kämpfen haben.
	 * @param list
	 * @return
	 * @throws TestelException
	 */
	protected TokenList trainLangStuff(TokenList tokList) throws TestelException {
		logger.info("TestEl-Trainer startet Sprachdatenverarbeitung");
		
		//konvertiere Elemente in andere Tags
		tokList = convertList(tokList);
		
		//jetzt die entsprechenden Elemente extrahieren
		
		//Punktuation:
		savePunctuation(tokList);
		//Zahlwörter:
		saveNumbers(tokList);
		//Abkürzungen:
		saveAbbreviations(tokList);
		//bedeutungstragende Wörter:
		saveWords(tokList);
		
		return tokList;
	}
	
	/**Bereinigt Tags von bedeutungstragenden Wörtern, Abkürzungen, Punktuation
	 * und Nummern, d.h. wandelt die Listen in entsprechende Elemente um
	 * @param list
	 * @return
	 * @throws TestelException
	 */
	protected TokenList convertList(TokenList tokList) throws TestelException {
		ListIterator<Token> it = tokList.listIterator();

		while (it.hasNext()) { //Durchlauf: number-Tags
			Token tok = it.next();
			int type = TestElTag.getTestElTagType(tok);
			if (tok instanceof TestElTag && type >= TestElTag.FIRST_LANGSTUFF_ELEM) {
				Token first = tok; //erstes Token merken
				Token last = null;
				
				it.remove(); //erstes Token löschen
				//Liste zum späteren Verarbeiten merken
				LinkedList<Token> tagList = new LinkedList<Token>();
				inner:
				while (it.hasNext()) { //Unterliste abarbeiten
					Token innerTok = it.next();
					//Ende des Tokens? -> Referenzen checken
					if (innerTok.getReference() == first) {
						last = innerTok;
						it.remove();
						break inner;
					} else { //kein Ende: Token zur Verarbeitungsliste und aus aktueller
						tagList.add(innerTok);
						it.remove();
					}
				}
				//Fehler, wenn kein Ende gefunden wurde
				if (last == null) throw new TestelException("Tag wurde nicht beendet: " + tok);
				
				if (tagList.isEmpty()) //bei leerer Liste einfach weiter...
					logger.warning("Leeres TestelTag " + first + " entfernt.");
				else { //so, jetzt Ersatzelement erstellen
					Token addTok;
					String className = first.getClassName();
					if (type != TestElTag.TAG_ABBREV && className == null)
						throw new TestelException("Bei diesem TestEl-Tag fehlt das class-Attribut: " + first);
					switch (type) {
					case TestElTag.TAG_NUMBER:
						addTok = normalizeNumber(tagList, className); break;
					case TestElTag.TAG_WORD:
						addTok = normalizeWord(tagList, className); break;
					case TestElTag.TAG_ABBREV:
						addTok = normalizeAbbrev(tagList); break;
					case TestElTag.TAG_PUNCT:
						addTok = normalizePunct(tagList, className); break;
					default:
						throw new TestelException("Fehlerhafte Auswahl eines LangStuff-Tokens: " + first);
					}
					//Grenzen, etc. ziehen
					addTok.setReference(tagList.getFirst().getReference());
					TextPosition bpos = first.getTextPosition();
					TextPosition epos = last.getTextPosition();
					addTok.initTextPosition(bpos.getBpos(), bpos.getBrow(), bpos.getBcol(), epos.getEpos(), epos.getErow(), epos.getEcol());
					it.add(addTok); //an dieser Position einsetzen
				}
			}
		}
		
		return tokList;
	}
	
	/**Normalisiert eine testel:word-Liste
	 * @param list
	 * @throws TestelException
	 */
	private TextToken normalizeWord(List<Token> list, String className) throws TestelException {
		Iterator<Token> it = list.iterator();

		StringBuffer mw = new StringBuffer(); //nimmt normalisierte Phrase auf
		
		while (it.hasNext()) {
			Token tok = it.next();
			if (!tok.isTextToken()) throw new TestelException("Token " + tok + " ist kein TextToken und kann nicht als bedeutungsvolles Wort deklariert werden");
			mw.append(" " + tok.getName());
		}
		
		String concatenated = mw.toString().trim();
		
		//alles ok, jetzt neues Element erzeugen
		return new TextToken(concatenated, className);
	}
	
	/**Normalisiert eine testel:number-Liste
	 * @param list
	 * @throws TestelException
	 */
	private NumberToken normalizeNumber(List<Token> list, String className) throws TestelException {
		if (list.size() != 1) throw new TestelException("Ein Tag <testel:number> kann nur einen einzigen Token besitzen (also fünfundzwanzig aber nicht fünf und zwanzig).\nHier: " + list);

		Token tok = list.get(0);
		if (!tok.isTextToken()) throw new TestelException("Token " + tok + " ist kein TextToken und kann nicht als Nummer deklariert werden");
		String name = tok.getName();
		
		try {
			Double.valueOf(className);
		} catch (Exception e) {
			throw new TestelException("Klasse des <testel:punct>-Tokens ist keine Zahl: " + tok);
		}

		//alls ok, jetzt neues Element erzeugen
		return new NumberToken(name, className);
	}
	
	/**Normalisiert eine testel:abbrev-Liste
	 * @param list
	 * @throws TestelException
	 */
	private SomeToken normalizeAbbrev(List<Token> list) throws TestelException {
		Iterator<Token> it = list.iterator();

		StringBuffer mw = new StringBuffer(); //nimmt normalisierte Phrase auf
		
		while (it.hasNext()) {
			Token tok = it.next();
			if (!tok.isTextToken()) throw new TestelException("Token " + tok + " ist kein TextToken und kann nicht als Abkürzung deklariert werden");
			mw.append(tok.getName());
		}
		
		String concatenated = mw.toString().trim();
		if (concatenated.charAt(concatenated.length()-1) != '.') throw new TestelException("Abkürzungen müssen mit einem . enden!\nHier: " + concatenated + " (" + list + ")");
		
		return new SomeToken(concatenated);
	}
	
	/**Normalisiert eine testel:punct-Liste
	 * @param list
	 * @throws TestelException
	 */
	private PunctuationToken normalizePunct(List<Token> list, String className) throws TestelException {
		if (list.size() != 1) throw new TestelException("Ein Tag <testel:punct> kann nur einen Token besitzen (z.B. ! oder /).\nHier: " + list);

		Token tok = list.get(0);
		if (!tok.isTextToken()) throw new TestelException("Token " + tok + " ist kein TextToken und kann nicht als Nummer deklariert werden");
		String name = tok.getName();
		if (name.length() != 1) throw new TestelException("Elemente innerhalb <testel:punct> dürfen nur ein Zeichen enthalten: " + tok);
		
		//alls ok, jetzt neues Element erzeugen
		return new PunctuationToken(name, className);
	}

	/**Extrahiert Punkte aus einer Liste und speichert diese in der entsprechenden Datei ab
	 * @param list
	 * @throws TestelException
	 */
	protected void savePunctuation(TokenList list) throws TestelException {
		//Benutze den Code vom PunctuationSubTagger
		PunctuationSubTagger tagger = handler.getLangFactory().getPunctuationSubTagger();
		tagger.setHandler(handler);
		try {
			tagger.init();
		} catch (TestelTaggerException e) {
			throw new TestelException("Satzzeichen konnten nicht geladen werden weil:\n" + e.getLocalizedMessage());
		}
		
		//Satzzeichen holen
		HashMap<Character,String> map = tagger.getPunctuationSet();
		
		//Datei anhängen
		FileWriter fw;
		File file = tagger.getPunctFile();
		try {
			if (!handler.isDryrun()) fw = new FileWriter(file, true); //anhängen, nicht überschreiben
			else fw = null;
		} catch (IOException e) {
			throw new TestelException("Konnte Satzzeichendatei " + file + " nicht zum Schreiben öffnen:\n" + e.getLocalizedMessage());
		}

		//Liste durchlaufen
		Iterator<Token> it = list.iterator();
		boolean hasWritten = false;
		
		while(it.hasNext()) {
			Token tok = it.next();
			if (!(tok instanceof PunctuationToken)) continue;
			String name = tok.getName();
			if (map.containsKey(name.charAt(0))) {
				logger.finer("Satzzeichen '" + name + "' ist schon in der Liste eingetragen");
			} else {
				String className = tok.getClassName();
				try {
					if (!handler.isDryrun()) fw.write('\n' + name + "=" + className);
				} catch (IOException e) {
					throw new TestelException("Konnte keine Zeile an Datei " + file + " anhängen:\n" + e.getLocalizedMessage());
				}
				hasWritten = true;
				map.put(name.charAt(0), className);
				String msg = "Neues Satzzeichen '" + name + "' wird in die Liste eingetragen";
				if (!handler.isQuiet()) System.out.println(msg);
				logger.fine(msg);
			}
		}

		try {
			if (!handler.isDryrun()) fw.close();
		} catch (IOException e) {
			throw new TestelException("Konnte Satzzeichendatei " + file + " nicht schließen:\n" + e.getLocalizedMessage());
		}
		String msg;
		if (hasWritten) msg = "Satzzeichendatei " + file + " wurde erfolgreich erweitert";
		else msg = "Satzzeichendatei " + file + " musste nicht erweitert werden";
		
		logger.info(msg);
		if (!handler.isQuiet()) System.out.println(msg);
	}
	
	/**Speichere Zahlwörter
	 * @param list
	 * @throws TestelException
	 */
	protected void saveNumbers(TokenList list) throws TestelException {
		//Benutze den Code vom PunctuationSubTagger
		AbstractNumberParser parser;
		try {
			parser = handler.getLangFactory().createNumberParser(handler);
		} catch (Exception e) {
			throw new TestelException("Nummernparser konnte nicht geladen werden weil:\n" + e.getLocalizedMessage());
		}
		
		//Satzzeichen holen
		HashMap<String,Integer> map = parser.getNumberSet();
		
		//Datei anhängen
		FileWriter fw;
		File file = parser.getNumberFile();
		try {
			if (!handler.isDryrun()) fw = new FileWriter(file, true); //anhängen, nicht überschreiben
			else fw = null;
		} catch (IOException e) {
			throw new TestelException("Konnte Nummerndatei " + file + " nicht zum Schreiben öffnen:\n" + e.getLocalizedMessage());
		}
		
		//Liste durchlaufen
		Iterator<Token> it = list.iterator();
		boolean hasWritten = false;
		
		while(it.hasNext()) {
			Token tok = it.next();
			if (!(tok instanceof NumberToken)) continue;
			String name = tok.getName().toLowerCase();
			//Name=Nummer oder Zahlenname?
			if (name.contains(",") || name.contains(".")) continue; //keine Kommazahlen
			try {
				Integer.parseInt(name);
			} catch (NumberFormatException nfe) { //nur, wenn Konvertierung nicht klappt, speichern
				//Einzelbuchstabe?
				if (name.length() == 1) continue;
				if (map.containsKey(name)) {
					logger.finer("Nummer '" + name + "' ist schon in der Liste eingetragen");
				} else {
					String className = tok.getAttributeValue("_TOK:number");
					Integer num;
					try {
						num = Integer.parseInt(className);
					} catch(NumberFormatException e) {
						throw new TestelException("Konnte " + className + " nicht in Zahl verwandeln");
					}
					try {
						if (!handler.isDryrun()) fw.write('\n' + name + "=" + className);
					} catch (IOException e) {
						throw new TestelException("Konnte keine Zeile an Datei " + file + " anhängen:\n" + e.getLocalizedMessage());
					}
					hasWritten = true;
					map.put(name, num);
					String msg = "Neues Zahlenwort '" + name + "' wird in die Liste eingetragen";
					if (!handler.isQuiet()) System.out.println(msg);
					logger.fine(msg);
				}				
			}
		}
		
		try {
			if (!handler.isDryrun()) fw.close();
		} catch (IOException e) {
			throw new TestelException("Konnte Nummerndatei " + file + " nicht schließen:\n" + e.getLocalizedMessage());
		}
		String msg;
		if (hasWritten) msg = "Nummerndatei " + file + " wurde erfolgreich erweitert";
		else msg = "Nummerndatei " + file + " musste nicht erweitert werden";
		
		logger.info(msg);
		if (!handler.isQuiet()) System.out.println(msg);
	}
	
	/**Speichere Abkürzungen
	 * @param list
	 * @throws TestelException
	 */
	protected void saveAbbreviations(TokenList list) throws TestelException {
		//Benutze den Code vom AbbreviationSubTagger
		AbbreviationSubTagger tagger = handler.getLangFactory().getAbbreviationSubTagger();
		tagger.setHandler(handler);
		try {
			tagger.init();
		} catch (TestelTaggerException e) {
			throw new TestelException("Abkürzungen konnten nicht geladen werden weil:\n" + e.getLocalizedMessage());
		}
		
		//Abkürzungen holen
		HashSet<String> abbrvSet = tagger.getAbbreviationSet();
		
		//Datei anhängen
		FileWriter fw;
		File abbrev = tagger.getAbbrevFile();
		try {
			if (!handler.isDryrun()) fw = new FileWriter(abbrev, true);
			else fw = null;
		} catch (IOException e) {
			throw new TestelException("Konnte Abkürzungsdatei " + abbrev + " nicht zum Schreiben öffnen:\n" + e.getLocalizedMessage());
		}
		
		//Liste durchlaufen
		Iterator<Token> it = list.iterator();
		boolean hasWritten = false;
		
		while(it.hasNext()) {
			Token tok = it.next();
			if (!(tok instanceof SomeToken) && !(tok instanceof TextToken)) continue;
			String name = tok.getName();
			if (name.charAt(name.length()-1) == '.') {
				if (abbrvSet.contains(name)) {
					logger.finer("Abkürzung '" + name + "' ist schon in der Liste eingetragen");
				} else {
					try {
						if (!handler.isDryrun()) fw.write('\n' + name);
					} catch (IOException e) {
						throw new TestelException("Konnte keine Zeile an Datei " + abbrev + " anhängen:\n" + e.getLocalizedMessage());
					}
					hasWritten = true;
					abbrvSet.add(name);
					String msg = "Neue Abkürzung '" + name + "' wird in die Liste eingetragen";
					if (!handler.isQuiet()) System.out.println(msg);
					logger.fine(msg);
				}
			}
		}

		try {
			if (!handler.isDryrun()) fw.close();
		} catch (IOException e) {
			throw new TestelException("Konnte Abkürzungsdatei " + abbrev + " nicht schließen:\n" + e.getLocalizedMessage());
		}
		String msg;
		if (hasWritten) msg = "Abkürzungsdatei " + abbrev + " wurde erfolgreich erweitert";
		else msg = "Abkürzungsdatei " + abbrev + " musste nicht erweitert werden";
		
		logger.info(msg);
		if (!handler.isQuiet()) System.out.println(msg);
	}
	
	/**Speichere bedeutungstragende Wörter
	 * @param list
	 * @throws TestelException
	 */
	protected void saveWords(TokenList list) throws TestelException {
		//Benutze den Code vom MeaningfulWordsSubTagger
		MeaningfulWordsSubTagger tagger = handler.getLangFactory().getMeaningfulWordsSubTagger();
		tagger.setHandler(handler);
		HashMap<String,String> map;
		try {
			map = tagger.readMeaningFulWords();
		} catch (TestelTaggerException e) {
			throw new TestelException("Bedeutungstragende Wörter konnten nicht geladen werden weil:\n" + e.getLocalizedMessage());
		}
		
		//Datei anhängen
		FileWriter fw;
		File file = tagger.getMeaningfulWordFile();
		try {
			if (!handler.isDryrun()) fw = new FileWriter(file, true);
			else fw = null;
		} catch (IOException e) {
			throw new TestelException("Konnte Datei bedeutungstragender Wörter " + file + " nicht zum Schreiben öffnen:\n" + e.getLocalizedMessage());
		}
		
		//Liste durchlaufen
		Iterator<Token> it = list.iterator();
		boolean hasWritten = false;
		
		while(it.hasNext()) {
			Token tok = it.next();
			if (!(tok instanceof TextToken)) continue;
			String name = tok.getName();
			if (map.containsKey(name)) {
				logger.finer("Bedeutungstragendes Wort '" + name + "' ist schon in der Liste eingetragen");
			} else {
				String className = tok.getClassName();
				try {
					if (!handler.isDryrun()) fw.write('\n' + name + "=" + className);
				} catch (IOException e) {
					throw new TestelException("Konnte keine Zeile an Datei " + file + " anhängen:\n" + e.getLocalizedMessage());
				}
				hasWritten = true;
				map.put(name, className);
				String msg = "Neues bedeutungstragendes Wort '" + name + "' wird in die Liste eingetragen";
				if (!handler.isQuiet()) System.out.println(msg);
				logger.fine(msg);
			}
		}
		
		try {
			if (!handler.isDryrun()) fw.close();
		} catch (IOException e) {
			throw new TestelException("Konnte Datei bedeutungstragender Wörter " + file + " nicht schließen:\n" + e.getLocalizedMessage());
		}
		
		String msg;
		if (hasWritten) msg = "Datei bedeutungstragender Wörter " + file + " wurde erfolgreich erweitert";
		else msg = "Datei bedeutungstragender Wörter " + file + " musste nicht erweitert werden";
		
		logger.info(msg);
		if (!handler.isQuiet()) System.out.println(msg);
	}
	
	/**Phase 2: Trainiert die Referenz-Angaben innerhalb von Tags
	 * @param tokenList
	 * @return
	 * @throws TestelException
	 */
	protected TokenList trainReferences(TokenList tokenList) throws TestelException {
		logger.info("TestEl-Trainer startet Referenzauswertung");
		
		LinkedList<SubTokenList> tagList = aggregateTags(tokenList, TestElTag.TAG_REF);

		//Liste leer?
		if (tagList.isEmpty()) {
			String msg = "Referenzliste war leer";
			logger.info(msg);
			if (!handler.isQuiet()) System.out.println(msg);
		}
		
		//Referenztagger laden
		ReferenceTagger tagger = handler.getMimeFactory().createReferenceTagger(handler, handler.getLangFactory().getLang());
		tagger.loadReferenceFile();
		
		//Referenzen durchgehen und abgleichen
		Iterator<SubTokenList> it = tagList.descendingIterator();
		boolean hasWritten = false;
		
		while (it.hasNext()) {
			TokenList list = it.next().getTokenList(); //hier ist nur die Tokenliste interessant
			if (list.size() < 3) throw new TestelException("testel:ref war leer:\n" + list);
			Token first = list.pollFirst(); //erstes Element abschneiden
			Token last = list.pollLast(); //letztes Element abschneiden
			//prüfen
			if (!(first instanceof TestElTag) || !first.getName().equals("ref"))
				throw new TestelException("testel:ref fehlerhaft - erstes Tag kein testel:ref:\n" + first);
			if (!(last instanceof TestElTag) || !last.getName().equals("/ref"))
				throw new TestelException("testel:ref fehlerhaft - letztes Tag kein testel:ref:\n" + last);
			//alles scheint ok - Restliste hinzufügen und prüfen, ob sie nicht schon
			//existiert
			if (tagger.addToList(list, first.getClassName())) hasWritten = true;
		}
		
		if (!handler.isDryrun()) {
			//falls Änderungen vorgenommen wurden
			if (hasWritten) tagger.saveReferenceFile();
		}	
		
		String msg;
		if (hasWritten) msg = "Referenzliste wurde erfolgreich erweitert";
		else msg = "Referenzliste musste nicht erweitert werden";

		logger.info(msg);
		if (!handler.isQuiet()) System.out.println(msg);

		return tokenList;
	}
	
	/**Phase 3: Trainiert die Tag-Strukturen - das ist der aufwendigste Teil
	 * @param tokenList
	 * @return
	 * @throws TestelException
	 */
	protected TokenList trainTextStructures(TokenList tokenList) throws TestelException {
		logger.info("TestEl-Trainer startet Auswertung der Textstruktur");
		
		//Liste der TestEl-Matches holen -> in der Liste sind Kopien der Match-SubListen
		LinkedList<SubTokenList> matchList = aggregateTags(tokenList, TestElTag.TAG_MATCH);

		//Liste leer?
		if (matchList.isEmpty()) {
			String msg = "Tagliste war leer - keine neuen Tags gelernt";
			logger.info(msg);
			if (!handler.isQuiet()) System.out.println(msg);
			return tokenList;
		}

		//TestEl-Tagger holen und Klassifizierer-Struktur laden
		TestElTagger testElTagger = (TestElTagger) handler.getMimeFactory().createTestElTagger(handler, handler.getLangFactory().getLang());
		testElTagger.loadClassifiersFile();

		//Liste der Positionen der TestEl-Tags holen
		TreeMap<Integer, LinkedList<TreeMapEntry>> tagPositionList = aggregateTagPositions(tokenList, matchList, testElTagger);

		//extrahierte Tag-Liste dem Klassifizierer stückweise übergeben
		//Referenzen durchgehen und abgleichen
		//descendingIterator ist hier wichtig, da Unlearn-Matches ans Ende der Liste gestellt
		//wurden - damit können sie Matches auf "unlearn" setzen, die gerade neu gelernt wurden.
		//Das macht Sinn, da damit die Qualitität der Treffer erhöht wird.
		Iterator<SubTokenList> it = matchList.descendingIterator();
		
		boolean hasWritten = false;
		
		while (it.hasNext()) {
			SubTokenList matchEntry = it.next();
			//so, schauen wir mal, ob in diese Liste Tags eingefügt werden müssen - die
			//Methode fügt diese Elemente dann gleich in die Liste ein, dann
			//kann der Trainer diese auch entsprechend lernen...
			addPossibleTestElTags(matchEntry, tagPositionList, testElTagger);

			if (testElTagger.addNewTokenList(matchEntry, tokenList))
				hasWritten = true;
		}
		
		if (!handler.isDryrun()) {
			//falls Änderungen vorgenommen wurden
			if (hasWritten) testElTagger.saveClassifiersFile();
		}
		
		String msg;
		if (hasWritten) msg = "Klassifizierer-Kollektion für '" + testElTagger.getClassifierName() + "' wurde erfolgreich erweitert";
		else msg = "Klassifizierer-Kollektion für '" + testElTagger.getClassifierName() + "' musste nicht erweitert werden";
		
		logger.info(msg);
		if (!handler.isQuiet()) System.out.println(msg);

		return tokenList;
	}
	
	/**Liest bestimmte Tag-Elemente aus der TokenListe heraus - es werden Kopien der Matches
	 * geholt. 
	 * @param tokenList
	 * @param type Typ des Elements, das gesucht wird, z.B. TAG_REF oder TAG_TAG
	 * @return Die SubListen in umgekehrter Reihenfolge!!! (also descending Iterator verwenden!)
	 * Die SubToken-Listen sind Kopien.
	 * @throws TestelException
	 */
	protected LinkedList<SubTokenList> aggregateTags(TokenList tokenList, int type) throws TestelException {
		LinkedList<SubTokenList> tags = new LinkedList<SubTokenList>();
		Stack<Integer> startStack = new Stack<Integer>();
		Stack<Boolean> unlearnStack = new Stack<Boolean>();

		// Liste durchlaufen
		ListIterator<Token> it = tokenList.listIterator();

		while (it.hasNext()) {
			Token tok = it.next();
			if (tok instanceof TestElTag && TestElTag.getTestElTagType(tok) == type) {
				if (tok.getName().charAt(0) == '/') { // Endtag
					int startpos;
					try {
						startpos = startStack.pop();
						// Unterliste in die Liste einfügen
					} catch (RuntimeException e) {
						throw new TestelException(
								"TestEl-Tags sind nicht alle geschlossen! Fehler bei "
										+ tok.toString());
					}
					//Unterliste hinzufügen - am Anfang = normale Matches
					//Unlearn-Matches werden am Anfang eingefügt (kommen also am Ende dran)
					if (unlearnStack.pop())
						tags.addFirst(new SubTokenList(tokenList.subList(startpos, it.nextIndex()),
								startpos, it.nextIndex()));
					else
						tags.addLast(new SubTokenList(tokenList.subList(startpos, it.nextIndex()),
								startpos, it.nextIndex()));
				} else { // Anfangstag
					// Position dieses Tags auf den Stack
					startStack.push(it.previousIndex());
					//AnfangsToken checken auf unlearn
					String unlearn = tok.getAttributeValue("unlearn");
					if (unlearn != null && unlearn.equalsIgnoreCase("yes"))
						unlearnStack.add(true);
					else unlearnStack.add(false);
				}
			}
		}

		return tags;
	}

	/**Holt sich die möglichen Positionen von Tags
	 * @param tokenList
	 * @param matchList
	 * @return
	 */
	protected TreeMap<Integer, LinkedList<TreeMapEntry>> aggregateTagPositions(TokenList tokenList, LinkedList<SubTokenList> matchList, TestElTagger testElTagger) throws TestelException {
		//Rückgabe-Hash
		TreeMap<Integer, LinkedList<TreeMapEntry>> treeMap =  new TreeMap<Integer, LinkedList<TreeMapEntry>>();
		
		//Matches durchlaufen
		Iterator<SubTokenList> it = matchList.iterator();
		
		while(it.hasNext()) {
			SubTokenList subTokenList = it.next();
			try {
				//unlearn-Matches explizit ausschließen
				Token matchToken = subTokenList.getTokenList().getFirst();
				String unlearn = matchToken.getAttributeValue("unlearn");
				if (unlearn != null && unlearn.equalsIgnoreCase("yes")) continue;
				//sonstige Matches -> weiter
				//Erstelle einen Klassifizierer, mit dessen Hilfe man die Tag-Positionen bestimmen kann
				Classifier classifier = testElTagger.getNewClassifier();
				classifier.insertTokenList(subTokenList, tokenList);
				//so, nun schauen, wo das Match hinfallen würde
				//Match-Tags entfernen
				TokenList innerList = subTokenList.getTokenList().subList(1, subTokenList.getTokenList().size()-2);
				SubTokenList match = classifier.getInnerOuterListToMatch(innerList,
						subTokenList.getStartPosition() + 1,
						subTokenList.getStopPosition() - 1, tokenList);
				//so, nun wissen wir Anfangs- und Endpunkt der Tokens, wie sie in die Liste
				//beim Matching eingefügt werden würden -> jetzt Tokens erstellen
				//Edit: Endtags werden nicht eingefügt...
				Token startTag = new TestElTag("tag", classifier.getClassName());
				Token stopTag = new TestElTag("/tag");
				startTag.setReference(stopTag);
				stopTag.setReference(startTag);
				//diese Tags nun in die Liste einfügen
				LinkedList<TreeMapEntry> l1 = treeMap.get(match.getStartPosition());
				LinkedList<TreeMapEntry> l2 = treeMap.get(match.getStopPosition());
				//neu oder erweitern?
				if (l1 == null) { //noch kein Eintrag vorhanden
					l1 = new LinkedList<TreeMapEntry>();
					l1.add(new TreeMapEntry(startTag, subTokenList));
					treeMap.put(match.getStartPosition(), l1);
				} else l1.add(new TreeMapEntry(startTag, subTokenList)); //Eintrag verlängern
				if (l2 == null) { //noch kein Eintrag vorhanden
					l2 = new LinkedList<TreeMapEntry>();
					l2.add(new TreeMapEntry(stopTag, subTokenList));
					treeMap.put(match.getStopPosition(), l2);
				} else l2.add(new TreeMapEntry(stopTag, subTokenList)); //Eintrag verlängern
			} catch (TestelClassifierException e) {
				throw new TestelException("Fehler beim Klassifizieren von " + subTokenList.getTokenList().getFirst() + '\n' + e.getLocalizedMessage());
			}			
		}
		return treeMap;
	}

	/**Verändert entry, indem mögliche TestElTags hinzugefügt werden 
	 * @param entry
	 * @param tagPositionList
	 */
	protected void addPossibleTestElTags(SubTokenList entry, TreeMap<Integer, LinkedList<TreeMapEntry>> tagPositionList, TestElTagger testElTagger) throws TestelException {
		//Liste der Tags rückwärts durchlaufen und nachElementen suchen
		Iterator<Integer> it = tagPositionList.descendingKeySet().iterator();
		
		//System.out.println(entry.getTokenList().getClassifiedList());
		//Klassifizierer holen wegen der Vereinfachung von Tags
//		Classifier classifier = testElTagger.getNewClassifier();
//		classifier.simplifyList(entry.getTokenList());
		
		while (it.hasNext()) {
			int pos = it.next();
			if (pos > entry.getStartPosition() && pos < entry.getStopPosition()) {
				//Eintrag holen und alle Treffer durchlaufen
				LinkedList<TreeMapEntry> listEntries = tagPositionList.get(pos);
				if (listEntries == null) throw new TestelException("Integritätsfehler in der TreeMapEntry des Trainers!");
				Iterator<TreeMapEntry> it2 = listEntries.iterator();
				while (it2.hasNext()) {
					TreeMapEntry mapEntry = it2.next();
					//prüfen, ob das nicht ein Tag ist, das von der aktuellen SubTokenList
					//erzeugt wurde - das wird dann ignoriert - im anderen Fall wird es in die
					//Liste eingetragen
					if (mapEntry.referenceList != entry) {
						int offset = pos - entry.getStartPosition();
						entry.getTokenList().add(offset, mapEntry.token);
					}
				}
				
			}
		}
//		System.out.println(entry.getTokenList().getClassifiedList());
//		System.out.println();
	}
	
	/**Kleine Hilfsklasse (eigentlich Enumeration), die Tags zu SubTokenList zuordnen kann
	 * @author mkalus
	 *
	 */
	protected class TreeMapEntry {
		SubTokenList referenceList;
		Token token;
		
		/** Konstruktor
		 * @param token
		 * @param referenceList
		 */
		public TreeMapEntry(Token token, SubTokenList referenceList) {
			this.token = token;
			this.referenceList = referenceList;
		}
		
		/* (Kein Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "TreeMapEntry->" + token.toString();
		}
	}
}

/**
 * Datei: MeaningfullWordsSubTagger.java
 * Paket: de.beimax.testel.lang
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
package de.beimax.testel.lang;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.TreeMap;

import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.impl.SomeToken;
import de.beimax.testel.token.impl.TextToken;

/**Subtagger für Bedeutungstragende Wörter bzw. Phrasen
 * @author mkalus
 *
 */
public class MeaningfulWordsSubTagger extends AbstractLangSubTagger {
	private PhraseListMap map; //PhraseListMap ist eine private Klasse (s.u.)
	private Token lastToken; //zeiger auf letztes Token, das überprüft wurde
							//verhindert Endlosschleifen bei nicht-Matches
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#init()
	 */
	public void init() throws TestelTaggerException {
		logger.info(getType() + " initialisiert");
		map = new PhraseListMap();

		//Lade punctuation.txt aus Sprachabhängigem Hintergrund
		BufferedReader reader;
		File file = getMeaningfulWordFile();
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new TestelTaggerException("Konnte Bedeutungstragende-Wörter-Datei " + file + " nicht laden:\n" + e.getLocalizedMessage());
		}

		String line;
		try {
			while ((line = reader.readLine()) != null)
				if (!line.trim().equals("") && line.trim().charAt(0) != '#') { //Kommentarzeilen
					//Instanz der privaten Klasse Phrase (s.u.) in die Map einfügen
					map.add(new Phrase(line));
				}
		} catch (IOException e) {
			throw new TestelTaggerException("Fehler beim Bearbeiten der Bedeutungstragende-Wörter-Datei " + file + ":\n" + e.getLocalizedMessage());
		}

	}
	
	/**Liest die Datei aus und gibt eine einfache Schlüssel=Wert-Liste zurück
	 * wird z.B. für den Trainer gebraucht.
	 * @return
	 */
	public HashMap<String, String> readMeaningFulWords() throws TestelTaggerException {
		HashMap<String, String> mymap = new HashMap<String, String>();
		
		//Lade punctuation.txt aus Sprachabhängigem Hintergrund
		BufferedReader reader;
		File file = getMeaningfulWordFile();
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new TestelTaggerException("Konnte Bedeutungstragende-Wörter-Datei " + file + " nicht laden:\n" + e.getLocalizedMessage());
		}

		String line;
		try {
			while ((line = reader.readLine()) != null)
				if (!line.trim().equals("") && line.trim().charAt(0) != '#') { //Kommentarzeilen
					Phrase phrase = new Phrase(line);
					mymap.put(phrase.getPhrase(), phrase.getType());
				}
		} catch (IOException e) {
			throw new TestelTaggerException("Fehler beim Bearbeiten der Bedeutungstragende-Wörter-Datei " + file + ":\n" + e.getLocalizedMessage());
		}		
		
		return mymap;
	}

	/**Gibt die Datei der meaningfulwords zurück
	 * @return
	 * @throws TestelTaggerException
	 */
	public File getMeaningfulWordFile() throws TestelTaggerException {
		return new File(getLangDir(), "meaningfulwords.txt");
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#subTag(de.beimax.testel.token.Token, de.beimax.testel.token.TokenList)
	 */
	public boolean subTag(Token currentToken, ListIterator<Token> iterator)
			throws TestelTaggerException {
		//dieses Token schon gechecked worden?
		if (lastToken == currentToken) return false;
		//im Prinzip lassen wir die Map die Hauptarbeit machen
		return map.checkToken(currentToken, iterator);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#getName()
	 */
	public String getType() {
		return "SubTagger für Bedeutungstragende Wörter";
	}
	
	/**Repräsentation einer Phrase
	 * @author mkalus
	 *
	 */
	protected class Phrase {
		private LinkedList<String> tokenlist;
		private String type;
		
		/** Konstruktor
		 * @param line Zeile aus der Datei meaningfulwords.txt - Phrase spaltet diese
		 * Zeile selbst auf
		 * @throws TestelTaggerException
		 */
		public Phrase(String line) throws TestelTaggerException {
			parse(line); //Parse
		}
		
		/**Parst die Zeile
		 * @param line
		 * @throws TestelTaggerException
		 */
		private void parse(String line) throws TestelTaggerException {
			//Leerzeichen weg
			line = line.trim();
			
			//Parsen
			int pos = line.lastIndexOf("=");
			if (pos == -1 || pos == line.length()-1) throw new TestelTaggerException("Zeile '" + line + "' konnte nicht in bedeutungsvolles Wort/Phrase geparst werden (=TYP fehlt)");
			
			//Typ zuweisen
			type = line.substring(pos+1).trim().toUpperCase();
			
			//restliche Zeile Tokenizen
			tokenlist = new LinkedList<String>();
			StringTokenizer tokenizer = new StringTokenizer(line.substring(0, pos));
			
			while (tokenizer.hasMoreTokens())
				tokenlist.add(tokenizer.nextToken());
			
			if (tokenlist.size() == 0)
				throw new TestelTaggerException("Zeile '" + line + "' konnte nicht in bedeutungsvolles Wort/Phrase geparst werden (TOKEN= fehlt)");
		}

		/** Getter für type
		 * @return type
		 */
		public String getType() {
			return type;
		}
		
		/**Gibt Phrase zurück
		 * @return
		 */
		public String getPhrase() {
			StringBuffer buffer = new StringBuffer();
			
			Iterator<String> it = tokenlist.iterator();
			
			while (it.hasNext()) buffer.append(' ' + it.next());
			
			return buffer.toString().trim();
		}
		
		/**Holt erstes Element aus der Liste
		 * @return
		 */
		public String getFirst() {
			return tokenlist.getFirst();
		}

		/**prüft, ob die Liste mit dem weiteren Verlauf übereinstimmt, bzw. stellt die
		 * Elemente dann zusammen
		 * @param currentToken
		 * @param iterator
		 * @return
		 */
		public boolean checkToken(Token currentToken, ListIterator<Token> iterator) {
			//einfacher Fall: Liste hat nur ein Element
			if (tokenlist.size() == 1) {
				String name = currentToken.getName();
				if (!name.equals(getFirst())) return false;
				Token tok = new TextToken(name, getType());
				tok.initTextPosition(currentToken.getTextPosition());
				iterator.set(tok);
				logger.finer("Einzelnes Token " + name + " zum bedeutungstragenden Wort erhoben");
				return true;
			}
			
			//check der ganzen Liste
			if (!iterator.hasNext()) return false; //Ende der Liste
			
			int counter = 1; //Zähler, falls es nicht klappt...
			Token next = currentToken; //weiterlaufen
			
			Iterator<String> it = tokenlist.iterator();
			String elem = null;
			String name = null;
			
			while (iterator.hasNext() && it.hasNext()) {
				if (!(next instanceof SomeToken)) break;
				name = next.getName();
				elem = it.next();
				if (!name.equals(elem)) break; //Schleife unterbrechen
				//ansonsten weiter
				next = iterator.next();
				counter++;
			}
			
			//ganze Liste durchlaufen? Dann ist es ein Treffer!
			if (!it.hasNext() && name.equals(elem)) {
				int max = counter-1;
				//zum currentToken zurück
				while (counter > 1) {
					next = iterator.previous();
					counter--;
				}
				//noch einmal vorwärts laufen
				while (max > 1) {
					next = iterator.next();
					currentToken.simpleJoin(next, true);
					iterator.remove();
					max--;
				}
				//jetzt ersetzen
				next = iterator.previous();
				Token tok = new TextToken(next.getName(), getType());
				tok.initTextPosition(next.getTextPosition());
				iterator.set(tok);
				return true;
			}
			
			//zurück in der Liste
			while (counter > 0) {
				next = iterator.previous();
				if (next == currentToken) break;
				counter--;
			}
			
			return false;
		}
	}
	
	/**Enthält eine Liste mit Phrasen gleicher Anfänge
	 * @author mkalus
	 *
	 */
	protected class PhraseList {
		private LinkedList<Phrase> phraseList = new LinkedList<Phrase>();
		
		/**Fügt eine Phrase zur Liste hinzu
		 * @param phrase
		 */
		public void add(Phrase phrase) {
			phraseList.add(phrase);
		}
		
		/**Geht die Liste durch und schaut, ob die weitere Liste mit einer
		 * der Phrasen übereinstimmen
		 * @param currentToken
		 * @param iterator
		 * @return
		 */
		public boolean checkToken(Token currentToken, ListIterator<Token> iterator) {
			Iterator<Phrase> it = phraseList.iterator();
			
			while (it.hasNext()) {
				if (it.next().checkToken(currentToken, iterator)) return true;
			}
			
			return false;
		}
	}
	
	/**Enthält eine Map mit PhrasenListen
	 * @author mkalus
	 *
	 */
	protected class PhraseListMap {
		private TreeMap<String, PhraseList> phraseMap = new TreeMap<String, PhraseList>();
		
		/**Fügt eine Phrase zur Liste hinzu
		 * @param phrase
		 */
		public void add(Phrase phrase) {
			String key = phrase.getFirst();
			//schon ein Eintrag vorhanden?
			PhraseList pl = phraseMap.get(key);
			if (pl == null) { //neue Liste einfügen
				pl = new PhraseList();
				pl.add(phrase);
				phraseMap.put(key, pl);
			} else pl.add(phrase);
		}
		
		/**parst ein Element der Liste und überprüft, ob die Tokens vorkommen können
		 * @param currentToken
		 * @param iterator
		 */
		public boolean checkToken(Token currentToken, ListIterator<Token> iterator) {
			if (currentToken == null) return false; //am Ende der Liste
			
			//nur sometokens berücksichtigen
			if (!(currentToken instanceof SomeToken)) return false;

			//Namen checken
			String name = new String(currentToken.getName());
			PhraseList pl = phraseMap.get(name); //in der Map nachsehen...
			//ersten Eintrag gefunden - jetzt Liste befragen
			if (pl != null) {
				lastToken = currentToken; //Zeiger auf diesen Token setzen
				return pl.checkToken(currentToken, iterator);
			}
			
			return false;
		}
	}
}

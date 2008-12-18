/**
 * Datei: AbbreviationSubTagger.java
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
import java.util.HashSet;
import java.util.ListIterator;

import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.impl.PunctuationToken;
import de.beimax.testel.token.impl.SomeToken;

/**Abkürzungstagger - sollte im zweiten Durchlauf der SubTaggers aufgerufen werden.
 * @author mkalus
 *
 */
public class AbbreviationSubTagger extends AbstractLangSubTagger {
	protected HashSet<String> abbreviations;
	protected boolean checked = false;

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#init()
	 */
	public void init() throws TestelTaggerException {
		logger.info(getType() + " initialisiert");
		abbreviations = new HashSet<String>();
		
		//Lade punctuation.txt aus Sprachabhängigem Hintergrund
		BufferedReader reader;
		File file = getAbbrevFile();
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new TestelTaggerException("Konnte Abkürzungsdatei " + file + " nicht laden:\n" + e.getLocalizedMessage());
		}

		String line;
		try {
			while ((line = reader.readLine()) != null)
				if (!line.trim().equals("") && line.trim().charAt(0) != '#') //Kommentarzeilen
					abbreviations.add(line.trim());
		} catch (IOException e) {
			throw new TestelTaggerException("Fehler beim Bearbeiten der Abkürzungsdatei " + file + ":\n" + e.getLocalizedMessage());
		}
		logger.finer("Abkürzungsdatei geladen.");
	}
	
	/**Gibt das Abkürzungsset heraus - wird vom Trainer zur Überprüfung von Doubletten
	 * benötigt
	 * @return
	 */
	public HashSet<String> getAbbreviationSet() {
		return abbreviations;
	}
	
	/**Gibt die Datei der abbreviations zurück
	 * @return
	 * @throws TestelTaggerException
	 */
	public File getAbbrevFile() throws TestelTaggerException {
		return new File(getLangDir(), "abbreviations.txt");
	}

//alte Version hat nicht alle Fälle zuverlässig abgefangen, z.B. "d. h.," wurde übersehen!
//	/* (Kein Javadoc)
//	 * @see de.beimax.testel.general.SubTagger#subTag(de.beimax.testel.token.Token, de.beimax.testel.token.TokenList)
//	 */
//	public boolean subTag(Token currentToken, ListIterator<Token> iterator)
//			throws TestelTaggerException {
//		if (currentToken == null) return false; //am Ende der Liste
//		
//		//nur sometokens berücksichtigen
//		if (!(currentToken instanceof SomeToken)) return false;
//
//		//letzten Buchstaben checken
//		String name = new String(currentToken.getName());
//		char lastchar = name.charAt(name.length()-1);
//
//		if (lastchar != '.') return false;
//		
//		logger.finest("Checke " + name + " auf mögliche Abkürzung.");
//		
//		//komplexe Abkürzung
//		if (iterator.hasNext()) {
//			int index = iterator.previousIndex();
//			String concat = name;
//			
//			while (iterator.hasNext()) {
//				Token t = iterator.next();
//				
//				if (!(t instanceof SomeToken) &&
//						!(t instanceof PunctuationToken)) break;
//				String n = new String(t.getName());
//				char lc = name.charAt(name.length()-1);
//				
//				if (lc != '.') break;
//				
//				//gefunden!
//				if (abbreviations.contains(concat + n)) {
//					joinAbbrevs(concat + n, index, iterator);
//					logger.finer("Komplexe Abkürzung " + concat + n + " entdeckt.");
//					return true;
//				} else concat += n; //weiter das Glück versuchen
//			}
//			
//			//zurückrudern
//			while (index != iterator.previousIndex())
//				iterator.previous();
//		}
//		
//		//einfache Abkürzung
//		if (abbreviations.contains(name)) {
//			logger.finer("Einfache Abkürzung " + name + " entdeckt.");
//			return true;
//		}
//
//		logger.finest("Keine Abkürzung: " + name + ".");
//		return false;
//	}
	
	public boolean subTag(Token currentToken, ListIterator<Token> iterator)
		throws TestelTaggerException {
		if (checked) { //schon ein Durchlauf mit dem selben Token?
			checked = false;
			return false;
		}
		
		if (currentToken == null) return false; //am Ende der Liste
		
		//nur PunctuationToken berücksichtigen
		if (!(currentToken instanceof PunctuationToken)) return false;
		
		//Nur Punkte in Erwägung ziehen
		if (currentToken.getName().charAt(0) != '.') return false;
		
		//so, beim vorherigen Token anfangen und prüfen
		iterator.previous();
		if (!iterator.hasPrevious()) {
			iterator.next(); //wieder auf Ausgangslage
			return false; //Am Anfang
		}
		Token first = iterator.previous();
		
		//falls kein SomeToken oder der Punkt nicht direkt an das Token
		//anschließt
		if (!(first instanceof SomeToken) ||
				currentToken.getTextPosition().getBpos() != first.getTextPosition().getEpos()) {
			iterator.next();
			iterator.next(); //wieder auf Ausgangslage
			return false;
		}
		
//		//so - prüfen auf einfache Abkürzung
//		if (abbreviations.contains(first.getName() + '.')) {
//			first.simpleJoin(currentToken);
//			first = iterator.next();
//			first = iterator.next();
//			iterator.remove();
//			return true;
//		}
//		
		//nicht der Fall, dann vielleicht komplexe Abkürzung
		int index = 0;
		StringBuffer concat = new StringBuffer();
		
		outer:
		while (iterator.hasNext()) {
			Token word = iterator.next();
			index++;
			Token abbr = null;
			if (iterator.hasNext()) {
				abbr = iterator.next();
				index++;
			}
			//nur falls erster Teil SomeToken, 2. Teil Punkt ist
			if (!(word instanceof SomeToken && abbr instanceof PunctuationToken &&
					abbr.getName().charAt(0) == '.')) break outer;
			//hinzufügen
			concat.append(word.getName() + '.');
			if (abbreviations.contains(concat.toString())) {
				//System.out.println(concat.toString());
				joinAbbrevs(concat.toString(), index, iterator);
			}
		}
		
		for (int i = 1; i < index; i++) {
			iterator.previous();
		}
		
		checked = true; //nicht noch einmal prüfen
		return false;
	}

	/**Abkürzungen zusammenfügen
	 * @param name
	 * @param index
	 * @param it
	 */
	protected void joinAbbrevs(String string, int index, ListIterator<Token> iterator) {
		//zum Anfang der Kette
		for (int i = 0; i < index; i++)
			iterator.previous();
		
		Token first = iterator.next();
		//nach mal von vorn
		for (int i = 0; i < index-1; i++) {
			first.simpleJoin(iterator.next()); //hinzufügen zum ersten Element
			iterator.remove(); //dieses Element aus der Liste nehmen
		}
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#getName()
	 */
	public String getType() {
		return "Abkürzungs-SubTagger";
	}

}

/**
 * Datei: PunctuationSubTaggerUTF.java
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
import java.util.ListIterator;
import java.util.logging.Level;

import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.impl.SomeToken;
import de.beimax.testel.token.impl.PunctuationToken;

/**SubTagger für Satzzeichen für den vollen USC-Bereich - leider langsamer, da
 * Zugriff per HashMap erfolgt.
 * @author mkalus
 *
 */
public class PunctuationSubTaggerUTF  extends PunctuationSubTagger {
	/**
	 * String[] punctuation enthält die Typen der Punktuation
	 */
	protected HashMap<Character, String> punctuation;

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#init()
	 */
	public void init() throws TestelTaggerException {
		logger.info(getType() + " initialisiert");
		punctuation = new HashMap<Character, String>();
		
		//Lade punctuation.txt aus Sprachabhängigem Hintergrund
		BufferedReader reader;
		File file = new File(getLangDir(), "punctuation.txt");
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new TestelTaggerException("Konnte Satzzeichendatei " + file + " nicht laden:\n" + e.getLocalizedMessage());
		}
		
		String line;
		try {
			while ((line = reader.readLine()) != null)
				if (!line.trim().equals("") && line.trim().charAt(0) != '#') { //Kommentarzeilen
					String[] keyval = line.split("=");
					if (keyval.length != 2) throw new IOException("Schlüsselwert " + line + " nicht korrekt!");
					punctuation.put(keyval[0].trim().charAt(0), keyval[1].trim());
				}
		} catch (IOException e) {
			throw new TestelTaggerException("Fehler beim Bearbeiten der Satzzeichendatei " + file + ":\n" + e.getLocalizedMessage());
		}
		logger.finer("Punktuationsdatei geladen.");
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.lang.PunctuationSubTagger#getPunctuationSet()
	 */
	public HashMap<Character,String> getPunctuationSet() {
		return punctuation;
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.lang.PunctuationSubTagger#getPunctFile()
	 */
	public File getPunctFile() throws TestelTaggerException {
		return new File(getLangDir(), "punctuation.txt");
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#subTag(de.beimax.testel.token.Token, de.beimax.testel.token.TokenList)
	 */
	public boolean subTag(Token currentToken, ListIterator<Token> iterator)
			throws TestelTaggerException {
		if (currentToken == null) return false; //am Ende der Liste
		
		//nur sometokens berücksichtigen
		if (!(currentToken instanceof SomeToken)) return false;

		//ersten und letzten Buchstaben checken
		String name = currentToken.getName();
		char firstchar = name.charAt(0);
		String type = punctuation.get(firstchar);
		//zu kurz, um noch gesplittet zu werden, einzelner Buchstabe/Character
		if (name.length() == 1) {
			if (type != null) {
				//neuer Tokentyp wird erstellt, falls Einzelbuchstabe hier interessant ist.
				Token newtok = new PunctuationToken(name, type);
				newtok.initTextPosition(currentToken.getTextPosition().copy());
				iterator.set(newtok);

				if (logger.getLevel() == Level.FINER)
					logger.finer("Einzelnes Punktuationszeichen gefunden: " + name + " " + currentToken.getTextPosition().toString() + ".");
			}
			return true; //auf jeden Fall weiter, der Rest ist dann uninteressant
		}
		
		//erster Buchstabe?
		if (type != null) {
			//Token-Splitten
			Token resttok = currentToken.split(1);
			//ersetze erstes Token
			Token newtok = new PunctuationToken(String.valueOf(firstchar), type);
			newtok.initTextPosition(currentToken.getTextPosition());
			
			//Einsetzen in Liste
			iterator.set(newtok);
			iterator.add(resttok);
			if (iterator.hasPrevious()) iterator.previous(); //zum Element zurück, um es noch einmal zu parsen
			
			if (logger.getLevel() == Level.FINER)
				logger.finer("Trenne erstes Zeichen von: " + name + " " + currentToken.getTextPosition().toString() + ".");
			
			return true;
		}
		
		//letzter Buchstabe
		char lastchar = name.charAt(name.length()-1);
		type = punctuation.get(lastchar);
		//nur die Tokens nehmen, deren Ende als Punktuation gekennzeichnet wurde
		//Trennstriche werden ausgenommen
		if (type != null && lastchar != '-') {
			//Token-Splitten
			Token resttok = currentToken.split(name.length()-1);
			Token newtok = new PunctuationToken(String.valueOf(lastchar), type);
			newtok.initTextPosition(resttok.getTextPosition());
			
			iterator.add(newtok);

			//2x zurück, um das Token noch einmal zu parsen. Wichtig bei Konstruktionen
			//wie "Hallo!" -> 2 Satzzeichen am Ende des Tokens!
			if (iterator.hasPrevious()) iterator.previous();
			if (iterator.hasPrevious()) iterator.previous();
			
			if (logger.getLevel() == Level.FINER)
				logger.finer("Trenne letztes Zeichen von: " + name + " " + currentToken.getTextPosition().toString() + ".");

			return true;
		}
		
		return false;
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#getName()
	 */
	public String getType() {
		return "Satzzeichen-SubTagger (UTF/UCS)";
	}

}

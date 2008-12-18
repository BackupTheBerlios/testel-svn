/**
 * Datei: AbstractNumberParser.java
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
import java.util.logging.Logger;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.impl.NumberToken;
import de.beimax.testel.token.impl.SomeToken;

/**Abstrakte Klasse eines Nummern-Parsers - sollte in den Implementationen als
 * Singleton (pro Sprache) implementiert werden!
 * @author mkalus
 *
 */
public abstract class AbstractNumberParser extends AbstractLangSubTagger {
	//Logger
	protected static final Logger logger = Logger.getLogger(AbstractNumberParser.class.getName());

	/**
	 *Mapping
	 */
	protected HashMap<String, Integer> map;

	/**
	 * protected Konstruktor... Singleton :-)
	 */
	protected AbstractNumberParser(TestElHandler handler) throws TestelException {
		this.handler = handler;
		logger.info(getType() + " initialisiert");
		try {
			createNumberMap();
		} catch (Exception e) {
			logger.warning("Konnte Nummerische Daten nicht laden");
			throw new TestelException("Konnte Nummerische Daten nicht laden:\n" + e.getLocalizedMessage());
		}
	}
	
	/**Singleton-Getter
	 * @return
	 * @throws Exception
	 */
	public static AbstractNumberParser getInstance(TestElHandler handler) throws Exception {
		throw new NotImplementedException();
	}
	
	/**Parse numerischen Wert
	 * @param strnum
	 * @return
	 * @throws Exception
	 */
	public abstract String parseNumeric(String strnum) throws Exception;
	
	/**
	 * Erstellt eine Zahlenmap
	 */
	protected void createNumberMap() throws TestelTaggerException {
		//neue Map erstellen
		map = new HashMap<String, Integer>();
		
		//Abkürzungsdatei laden
		BufferedReader reader;
		File file = getNumberFile();
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new TestelTaggerException("Konnte Nummerndatei " + file + " nicht laden!");
		}
		
		String line;
		try {
			while ((line = reader.readLine()) != null)
				if (!line.trim().equals("") && line.trim().charAt(0) != '#') { //Kommentarzeilen
					String[] keyval = line.split("=");
					if (keyval.length != 2) throw new TestelTaggerException("Schlüsselwert " + line + " nicht korrekt!");
					Integer val;
					try {
						val = Integer.parseInt(keyval[1].trim());
					} catch (Exception e) {
						throw new TestelTaggerException("Konnte Wert von " + line + " nicht in Zahl umwandeln!");
					}
					
					map.put(keyval[0].trim().toLowerCase(), val);
				}
		} catch (IOException e) {
			throw new TestelTaggerException("Fehler beim Lesen von " + file);
		}
	}

	/**Gibt das Nummernset heraus - wird vom Trainer zur Überprüfung von Doubletten
	 * benötigt
	 * @return
	 */
	public HashMap<String, Integer> getNumberSet() {
		return map;
	}
	
	/**Gibt die Datei der numerics zurück
	 * @return
	 * @throws TestelTaggerException
	 */
	public File getNumberFile() throws TestelTaggerException {
		return new File(getLangDir(), "numerics.txt");
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#init()
	 */
	public void init() throws TestelTaggerException {
		//leer!
	}

	public boolean subTag(Token currentToken, ListIterator<Token> iterator) throws TestelTaggerException {
		if (currentToken == null) return false; //am Ende der Liste
		
		//nur sometokens berücksichtigen
		if (!(currentToken instanceof SomeToken)) return false;
		
		//Name holen
		String name = new String(currentToken.getName());
		String numeric;
		try {
			numeric = parseNumeric(name);
		} catch (Exception e) { return false; } //keine Nummer...
		
		//neuer Nummern-Token
		Token newtok = new NumberToken(name, numeric);
		newtok.initTextPosition(currentToken.getTextPosition());
		iterator.set(newtok);
		
		if (logger.getLevel() == Level.FINEST)
			logger.finest("Zahl erkannt: " + newtok.toString());
		
		return true;
	}
}

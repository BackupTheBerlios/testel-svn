/**
 * Datei: SingleLetterCounterSubTagger.java
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

import java.util.ListIterator;
import java.util.logging.Level;

import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.impl.PunctuationToken;
import de.beimax.testel.token.impl.SomeToken;
import de.beimax.testel.token.impl.NumberToken;

/**Erkennt einzelne Buchstaben, die vor Klammern stehen als Zahlenwerte an.
 * @author mkalus
 *
 */
public class SingleLetterCounterSubTagger extends AbstractLangSubTagger {
	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#init()
	 */
	public void init() throws TestelTaggerException {
		logger.info(getType() + " initialisiert");
		//sonst nix machen
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#subTag(de.beimax.testel.token.Token, de.beimax.testel.token.TokenList)
	 */
	public boolean subTag(Token currentToken, ListIterator<Token> iterator)
			throws TestelTaggerException {
		if (currentToken == null) return false; //am Ende der Liste
		
		//nur sometokens berücksichtigen
		if (!(currentToken instanceof SomeToken)) return false;

		//nur Tags mit einzelnen Buchstaben
		String name = currentToken.getName();
		if (name.length() > 1) return false; //nur einzelne Buchstaben

		char letter = name.charAt(0);
		//Nur Buchstaben zulassen
		if (letter < 'A' || letter > 'z' || (letter > 'Z' && letter < 'a')) return false;
		
		//Ok, folgt eine Klammerstruktur auf dieses Element?
		if (!iterator.hasNext()) return false;
		Token next = iterator.next();
		
		//Satzzeichen oder SomeToken?
		if (!(next instanceof PunctuationToken || next instanceof SomeToken)) {
			iterator.previous(); //zurückrudern...
			return false;
		}
		
		//Klammer hinter der Struktur?
		char nextname = next.getName().charAt(0);
		if (nextname != ')' && nextname != ']' && nextname != '}') {
			iterator.previous(); //zurückrudern...
			return false;
		}
		
		//Ok, das ganze scheint eine Buchstaben-Klammer-Kombi zu sein, dann ist wohl das
		//Token eine Zahl
		//logger.finer("Trenne erste Zahl von: " + name + ".");
		int num;
		if (letter <= 90) num = letter - 64; //Großbuchstaben
		else num = letter - 96; //Kleinbuchstaben
		
		//neues Token erstellen
		Token newtok = new NumberToken(String.valueOf(letter), String.valueOf(num));
		newtok.initTextPosition(currentToken.getTextPosition());
		
		//zurück und ersetzen und wieder weiter
		next = iterator.previous();
		next = iterator.previous();
		iterator.set(newtok);
		next = iterator.next();
		
		if (logger.getLevel() == Level.FINEST)
			logger.finest("Neues einzelnes Zeichen als Zahl mit Klammer erkannt : " + newtok.toString());

		return true;
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#getName()
	 */
	public String getType() {
		return "SubTagger für einzelne Buchstaben als Zähler";
	}

}

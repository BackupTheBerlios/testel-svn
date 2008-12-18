/**
 * Datei: NumberPointCombinationSubTagger.java
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

import java.util.HashMap;
import java.util.ListIterator;

import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.impl.SomeToken;
import de.beimax.testel.token.impl.PunctuationToken;
import de.beimax.testel.token.impl.NumberToken;

/**
 * @author mkalus
 *
 */
public class NumberPointCombinationSubTagger extends AbstractLangSubTagger {
	//geklaut vom PunctuationSubTaggerUTF
	private HashMap<Character, String> punctuation;
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#init()
	 */
	public void init() throws TestelTaggerException {
		logger.info(getType() + " initialisiert (Initialisiere eigenen temporären PunctuationSubTaggerUTF mit)");
		//Nicht sehr schön, aber dafür muss man die Kapselung nicht brechen...
		try {
			PunctuationSubTaggerUTF subTaggerUTF = new PunctuationSubTaggerUTF();
			subTaggerUTF.setHandler(handler);
			subTaggerUTF.init();
		
			punctuation = subTaggerUTF.punctuation;
		} catch (Exception e) { //Fange alle Fehler ab und nehme Default... ist robuster
			punctuation = new HashMap<Character, String>();
		}
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#subTag(de.beimax.testel.token.Token, de.beimax.testel.token.TokenList)
	 */
	public boolean subTag(Token currentToken, ListIterator<Token> iterator)
			throws TestelTaggerException {
		if (currentToken == null) return false; //am Ende der Liste
		
		//nur sometokens berücksichtigen
		if (!(currentToken instanceof SomeToken)) return false;

		String name = currentToken.getName();
		char[] chars = name.toCharArray();
		
		//check des Tokens nach erlaubten Zeichen
		for (int i = 0; i < chars.length; i++)
			if (chars[i] < '.' || chars[i] == '/' || chars[i] > '9') return false;

		//Ok, ab jetzt gibt es nur noch Tokens mit Zahlen und Punkten, d.h. wir
		//können jetzt die Trennungen anfangen...
		int pos = name.indexOf('.');
		if (pos == -1) return false; //keinen Punkt gefunden
		
		//erstes Element ein Punkt
		if (pos == 0) {
			char firstchar = name.charAt(0);
			//Token-Splitten
			Token nexttok = currentToken.split(1);
			//ersetze erstes Token
			String type = punctuation.get(firstchar);
			if (type == null) type = "NUMBERPOINT";
			Token newtok = new PunctuationToken(String.valueOf(firstchar), type);
			newtok.initTextPosition(currentToken.getTextPosition());
			iterator.set(newtok);
			//nexttok anfügen 
			iterator.add(nexttok);
			
			//zum Element zurück, um es noch einmal zu parsen
			if (iterator.hasPrevious()) iterator.previous();
			
			logger.finest("Trenne erstes Zeichen von: " + name + " " + currentToken.getTextPosition() + ".");
		} else {
			//Token-Splitten
			Token nexttok = currentToken.split(pos);
			//ersetze erstes Token
			Token newtok = new NumberToken(name.substring(0, pos));
			newtok.initTextPosition(currentToken.getTextPosition());
			iterator.set(newtok);
			//nexttok anfügen 
			iterator.add(nexttok);

			//zum Element zurück, um es noch einmal zu parsen
			if (iterator.hasPrevious()) iterator.previous();

			logger.finest("Trenne erste Zahl von: " + name + " " + currentToken.getTextPosition() + ".");
		}
		
		return true;
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#getName()
	 */
	public String getType() {
		return "Nummer/Punkt-Kombinations-SubTagger";
	}

}

/**
 * Datei: NumberToken.java
 * Paket: de.beimax.testel.token.impl
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
package de.beimax.testel.token.impl;

import de.beimax.testel.lang.AbstractNumberParser;
import de.beimax.testel.token.Token;

/**
 * @author mkalus
 *
 */
public class NumberToken extends Token {
	private String number = null;
	
	public NumberToken(String name, String value) {
		super(name);
		this.number = value;
		//addAttribute("_TOK:number", value);
	}
	
	public NumberToken(String name) {
		super(name);
		parseNumber(name);
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#getType()
	 */
	public String getType() {
		return "NUMBER";
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#isTextToken()
	 */
	public boolean isTextToken() {
		return true;
	}

	/**<code>name</code> oder ein anderer String wird auf numerische Elemente hin überprüft.
	 * @param strnum
	 * @return Nummer
	 */
	public String parseNumber(String strnum) {
		AbstractNumberParser numParser = null;
		try {
			numParser = handler.getLangFactory().createNumberParser(handler);
		} catch (Exception e) {
			logger.warning("NumberToken: Konnte NumberParser nicht erstellen! Nummer wurde nicht geparst!");
			return null;
		}
		
		//Nummer parsen
		String number;
		try {
			number = numParser.parseNumeric(strnum);
		} catch (Exception e) {
			logger.warning("NumberToken: Konnte " + strnum + " nicht in eine Zahl umwandeln!");
			return null;
		}
		
		//addAttribute("_TOK:number", number);
		this.number = number; 
		
		return number;
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#getAttributeValue(java.lang.String)
	 */
	public String getAttributeValue(String key) {
		if (key.equals("_TOK:number")) return this.number;
		else return super.getAttributeValue(key);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.tokens.Token#nameToString()
	 */
	protected String nameToString() {
		//String num = getAttributeValue("_TOK:number");
		if (this.number.equals("")) return getName();
		return getName() + "[=" + this.number + "]";
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.tokens.Token#copy()
	 */
	public Token copy() {
		NumberToken newtok = new NumberToken(new String(getName()), this.number);
		
		copyAttribs(this, newtok);
		newtok.initTextPosition(getTextPosition());
		newtok.setReference(getReference());
		
		return newtok;
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#getClassifierName()
	 */
	public String getClassifierName() {
		//return "N_" + getAttributeValue("_TOK:number") + getClassifierByAttributeName("texttype");
		//hier ist die eigentliche Zahl egal
		return "N" + getClassifierByAttributeName("texttype");
	}
}

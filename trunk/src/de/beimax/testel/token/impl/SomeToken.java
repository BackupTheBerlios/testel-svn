/**
 * Datei: SomeToken.java
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

import de.beimax.testel.token.Token;

/**Dummy-Token für alle Tokens, die keinen speziellen Typ haben. Generell werden
 * SOMETOKENs ignoriert.
 * @author mkalus
 *
 */
public class SomeToken extends Token {

	/** Konstruktor
	 * @param name
	 */
	public SomeToken(String name) {
		super(name);
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#getType()
	 */
	public String getType() {
		return "SOMETOKEN";
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#isTextToken()
	 */
	public boolean isTextToken() {
		return true;
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.tokens.Token#copy()
	 */
	public Token copy() {
		SomeToken newtok = new SomeToken(new String(getName()));
		
		copyAttribs(this, newtok);
		newtok.initTextPosition(getTextPosition());
		newtok.setReference(getReference());
		
		return newtok;
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#compare(de.beimax.testel.token.Token)
	 */
	public double compare(Token tok) {
		//SomeTokens sind immervergleichbar
		if (tok instanceof SomeToken) return 1;
		return 0;
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#getClassifierName()
	 */
	public String getClassifierName() {
		return getClassifierByAttributeName("texttype");
	}
}

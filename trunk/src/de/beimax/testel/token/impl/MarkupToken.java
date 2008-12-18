/**
 * Datei: MarkupToken.java
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

/**Allgemeines Token zur Beschreibung von Markup
 * @author mkalus
 *
 */
public class MarkupToken extends Token {

	/** Konstruktor
	 * @param name
	 */
	public MarkupToken(String name) {
		super(name);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#getType()
	 */
	public String getType() {
		return "MARKUP";
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.tokens.Token#copy()
	 */
	public Token copy() {
		MarkupToken newtok = new MarkupToken(new String(getName()));
		
		copyAttribs(this, newtok);
		newtok.initTextPosition(getTextPosition());
		newtok.setReference(getReference());
		
		return newtok;
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#sameName(de.beimax.testel.token.Token)
	 */
	public boolean sameName(Token tok) { //eigentlich HTML-spezifisch...
		String n1 = getName();
		String n2 = tok.getName();
		if (n1.charAt(0) == '/') n1 = n1.substring(1);
		if (n2.charAt(0) == '/') n2 = n2.substring(1);
		return n1.equals(n2);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#compare(de.beimax.testel.token.Token)
	 */
	public double compare(Token tok) {
		if (this.getClass() != tok.getClass()) return 0;

		String n1 = getName();
		String n2 = tok.getName();
		//einer von beiden ein Endtoken
		if ((n1.charAt(0) == '/' && n2.charAt(0) != '/') ||
				(n1.charAt(0) != '/' && n2.charAt(0) == '/')) {
			//nur Namen und Typ prüfen
			if (!sameType(tok)) return 0;
			if (!sameClassName(tok)) return 0;
			if (!sameName(tok)) return 0;
			return 1;
		}
		else return super.compare(tok);
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#getClassifierName()
	 */
	public String getClassifierName() {
		return "M_" + getName() + getClassifierByAttributeName("texttype");
	}
}

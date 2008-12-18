/**
 * Datei: ImageToken.java
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

/**
 * @author mkalus
 *
 */
public class ImageToken extends Token {
	/** Konstruktor - std-Name NN
	 */
	public ImageToken() {
		this("NN");
	}

	/** Konstruktor
	 * @param url
	 */
	public ImageToken(String url) {
		super(url);
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#getType()
	 */
	public String getType() {
		return "IMAGE";
	}

	/**wird durch den ImageTagger aufgerufen und weist den Bildern verschiedene wichtige Typen zu
	 * @param type
	 */
	public void setImageClassName(String type) {
		setClassName(type);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.tokens.Token#copy()
	 */
	public Token copy() {
		ImageToken newtok = new ImageToken(new String(getName()));
		
		copyAttribs(this, newtok);
		newtok.initTextPosition(getTextPosition());
		newtok.setReference(getReference());
		
		return newtok;
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#getClassifierName()
	 */
	public String getClassifierName() {
		if (isClassNameSet())
			return "IMG_" + getClassName();
		return "IMG";
	}
}

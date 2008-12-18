/**
 * Datei: TestElTag.java
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

/**Auszeichnung für ein TestElTag
 * @author mkalus
 *
 */
public class TestElTag extends Token {
	/**
	 * Konstanten
	 */
	public static final String[] TAG_NAMES = {
		"tag", "match", "ref", "word", "punct", "number", "abbrev"
	};
	
	public static final int NO_TAG = -1;
	public static final int TAG_TAG = 0;
	public static final int TAG_MATCH = 1;
	public static final int TAG_REF = 2;
	public static final int TAG_WORD = 3;
	public static final int TAG_PUNCT = 4;
	public static final int TAG_NUMBER = 5;
	public static final int TAG_ABBREV = 6;
	
	public static final int FIRST_LANGSTUFF_ELEM = TAG_WORD;

	/**Gibt einen numerischen Typ eines Tokens wieder
	 * @param tok
	 * @return
	 */
	public static int getTestElTagType (Token tok) {
		String name = tok.getName();
		if (name.charAt(0) == '/') name = name.substring(1); //End-Tag-Erkenner weg
		for (int i = 0; i < TAG_NAMES.length; i++)
			if (TAG_NAMES[i].equalsIgnoreCase(name)) return i;
		
		return -1;
	}

	/** Konstruktor
	 * @param name
	 */
	public TestElTag(String name, String className) {
		super(name, className);
	}

	/** Konstruktor ohne Variety
	 * @param name
	 */
	public TestElTag(String name) {
		this(name, null);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#getType()
	 */
	public String getType() {
		return "TESTELTAG";
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.tokens.Token#copy()
	 */
	public Token copy() {
		String var = getClassName();
		if (var != null) var = new String(var);
		String nam = getName();
		if (nam != null) nam = new String(nam);
		
		TestElTag newtok = new TestElTag(nam, var);
		
		copyAttribs(this, newtok);
		newtok.initTextPosition(getTextPosition());
		newtok.setReference(getReference());
		
		return newtok;
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#sameName(de.beimax.testel.token.Token)
	 */
	public boolean sameName(Token tok) {
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
	 * @see de.beimax.testel.token.Token#classNameToString()
	 */
	protected String classNameToString() {
		String var = getClassName();
		if (isClassNameSet() && var != null) return "/" + var;
		return "";
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.token.Token#getClassifierName()
	 */
	public String getClassifierName() {
		String name = getName();
		//if (name.endsWith("match")) return null; //keine Matches in den Klassifizierern
		if (name.charAt(0) == '/') return "T_" + name + getClassifierByAttributeName("_TOK:variety");
		return "T_" + name + '_' + getClassName();
	}
}

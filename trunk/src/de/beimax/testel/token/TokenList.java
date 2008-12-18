/**
 * Datei: TokenList.java
 * Paket: de.beimax.testel.token
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
package de.beimax.testel.token;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import de.beimax.testel.token.impl.NumberToken;
import de.beimax.testel.token.impl.TestElTag;
/**
 * @author mkalus
 *
 */
public class TokenList extends LinkedList<Token> {
	private static final long serialVersionUID = -3046024476621444921L;

	/* (Kein Javadoc)
	 * @see de.beimax.testel.tokens.list.TextElement#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer("*TOKENLIST:BEGIN\n");
		
		Iterator<Token> li = iterator();
		while (li.hasNext())
			buffer.append(li.next().toString() + "\n");
		buffer.append("*TOKENLIST:END\n");
		
		return buffer.toString();
	}
	
	/**Gibt eine Kopie der Liste zurück
	 * @return
	 */
	public TokenList copy() {
		TokenList newlist = new TokenList();
		
		Iterator<Token> li = iterator();
		while (li.hasNext())
			newlist.add(li.next().copy());
		
		return newlist;
	}
	
	/**Gibt die Zahl der TestEl-Elemente innerhalb der Liste zurück. Gezählt werden nur
	 * Anfangselemente, die voneinander unterscheidbar sein müssen (Doubletten werden also
	 * nicht gezählt).
	 * @return
	 */
	public int countTestElTags() {
		//Menge zur Reduzierung der Doubletten
		HashSet<String> set = new HashSet<String>();
		
		Iterator<Token> li = iterator();
		while (li.hasNext()) {
			Token tok = li.next();
			//nur TestElTags und keine EndTags
			if (tok instanceof TestElTag && tok.getName().charAt(0) != '/') {
				set.add(tok.getClassifierName());
			}
		}

		return set.size(); //Mächtigkeit der Menge zurückgeben
	}
	
	/**Vergleicht zwei Listen und gibt true zurück, wenn die Tokens beider Listen vergleichbar sind.
	 * Dies gilt nur für den TextContent der Tokens, d.h. ihre Namen!
	 * @param compareList
	 * @return
	 */
	public boolean hasSameTextContent(TokenList compareList) {
		//beide Listen müssen die selbe Länge haben
		if (compareList.size() != this.size()) return false;
		
		Iterator<Token> it1 = iterator();
		Iterator<Token> it2 = compareList.iterator();
		
		//die jeweils selben Tags müssen den gleichen Namen haben
		while (it1.hasNext()) {
			Token t1 = it1.next();
			Token t2 = it2.next();
			//Sonderfall: Zahlen -> werden nicht überprüft
			if (t1 instanceof NumberToken && t2 instanceof NumberToken) continue;
			if (!t1.getName().equals(t2.getName())) return false;
		}
		
		return true;
	}
	
	/**Gibt die Liste als klassifizierte Liste zurück - es werden nur abstrahierte Elemente
	 * aufgenommen und keine Doubletten.
	 * 
	 * @return
	 */
	public String getClassifiedList() {
//		//zuerst alles in eine Menge von Elementen, damit doppelte Elemente nicht mehr zählen
//		HashSet<String> set = new HashSet<String>();
//		
//		Iterator<Token> it = iterator();
//		while (it.hasNext()) {
//			String classfName = it.next().getClassifierName();
//			if (classfName != null) set.add(classfName); //null-Klassifier werde ignoriert
//		}
//		
//		//jetzt alles in einen Puffer speichern
//		StringBuffer buffer = new StringBuffer();
//		Iterator<String> iter = set.iterator();
//		while (iter.hasNext()) {
//			if (buffer.length() > 0) buffer.append(' ');
//			buffer.append(iter.next());
//		}
//		
//		return buffer.toString();
		
		//alles in einen Puffer speichern
		StringBuffer buffer = new StringBuffer();
		
		//Liste durchlaufen
		Iterator<Token> it = iterator();
		
		while (it.hasNext()) { //einzelne Klassifizierer an die Liste anhängen
			String classified = it.next().getClassifierName();
			if (classified != null) {
				if (buffer.length() > 0) buffer.append(' ');
				buffer.append(classified);
			}
		}
		
		return buffer.toString();
	}
	
	/* (Kein Javadoc)
	 * @see java.util.AbstractList#subList(int, int)
	 */
	public TokenList subList(int start, int stop) {
		TokenList backList = new TokenList();
		
		Iterator<Token> it = super.subList(start, stop).iterator();
		
		while (it.hasNext())
			backList.add(it.next());
		
		return backList;
	}
}

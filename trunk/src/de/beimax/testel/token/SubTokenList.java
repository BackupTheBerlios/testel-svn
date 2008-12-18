/**
 * Datei: SubTokenList.java
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

/**Einfache Klasse, die während des Matching genommen wird, um Unterlisten der
 * Gesamtliste darzustellen
 * @author mkalus
 *
 */
public class SubTokenList {
	private int startPosition, stopPosition;
	private TokenList tokenList;
	
	/** Konstruktor
	 * @param tokenList
	 * @param startPosition
	 * @param endPosition
	 */
	public SubTokenList(TokenList tokenList, int startPosition, int endPosition) {
		this.tokenList = tokenList;
		this.startPosition = startPosition;
		this.stopPosition = endPosition;
	}

	/** Getter für endPosition
	 * @return endPosition
	 */
	public int getStopPosition() {
		return stopPosition;
	}

	/** Getter für startPosition
	 * @return startPosition
	 */
	public int getStartPosition() {
		return startPosition;
	}

	/** Getter für tokenList
	 * @return tokenList
	 */
	public TokenList getTokenList() {
		return tokenList;
	}
	
	/* (Kein Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "SubTokenList: " + startPosition + '-' + stopPosition + '\n' + tokenList.toString();
	}
}

/**
 * Datei: TextPosition.java
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

/**Hilfsklasse zum Festhalten von Textpositionen.
 * @author mkalus
 *
 */
public class TextPosition {
	
	/**Positionsangaben der Textposition:
	 * bpos = Anfangsposition (Bytes)
	 * brow = Anfangszeile
	 * bcol = Anfangsspalte
	 * epos = Endposition (Bytes)
	 * erow = Endzeile
	 * ecol = Endspalte
	 */
	private int bpos, brow, bcol, epos, erow, ecol;

	public TextPosition(int bpos, int brow, int bcol, int epos, int erow, int ecol) {
		setBpos(bpos);
		setBrow(brow);
		setBcol(bcol);
		setEpos(epos);
		setErow(erow);
		setEcol(ecol);
	}

	/**
	 * @return bcol
	 */
	public int getBcol() {
		return bcol;
	}

	/**
	 * @param bcol Festzulegender bcol
	 */
	public void setBcol(int bcol) {
		this.bcol = bcol;
	}

	/**
	 * @return bpos
	 */
	public int getBpos() {
		return bpos;
	}

	/**
	 * @param bpos Festzulegender bpos
	 */
	public void setBpos(int bpos) {
		this.bpos = bpos;
	}

	/**
	 * @return brow
	 */
	public int getBrow() {
		return brow;
	}

	/**
	 * @param brow Festzulegender brow
	 */
	public void setBrow(int brow) {
		this.brow = brow;
	}

	/**
	 * @return ecol
	 */
	public int getEcol() {
		return ecol;
	}

	/**
	 * @param ecol Festzulegender ecol
	 */
	public void setEcol(int ecol) {
		this.ecol = ecol;
	}

	/**
	 * @return epos
	 */
	public int getEpos() {
		return epos;
	}

	/**
	 * @param epos Festzulegender epos
	 */
	public void setEpos(int epos) {
		this.epos = epos;
	}

	/**
	 * @return erow
	 */
	public int getErow() {
		return erow;
	}

	/**
	 * @param erow Festzulegender erow
	 */
	public void setErow(int erow) {
		this.erow = erow;
	}
	
	/**Gibt true zurück, wenn übergebene Position vollständig enhalten ist.
	 * @param pos
	 * @return
	 */
	public boolean contains(TextPosition pos) {
		if (this.bpos <= pos.getBpos() && this.epos >= pos.getEpos()) return true;
		return false;
	}
	
	/**Gibt true zurück, wenn übergebene Position vollständig dieses Objekt enthält.
	 * @param pos
	 * @return
	 */
	public boolean containedBy(TextPosition pos) {
		return pos.contains(this);
	}
	
	/**True, falls Anfang dieses Objekts links vom Anfang von pos.
	 * @param pos
	 * @return
	 */
	public boolean leftOf(TextPosition pos) {
		if(this.bpos <= pos.getBpos()) return true;
		return false;
	}
	
	/**True, falls Ende dieses Objekts rechts vom Ende von pos.
	 * @param pos
	 * @return
	 */
	public boolean rightOf(TextPosition pos) {
		if(this.epos >= pos.getEpos()) return true;
		return false;
	}
	
	/**Erzeugt einen Klon der Textposition
	 * @return
	 */
	public TextPosition copy() {
		return new TextPosition(bpos, brow, bcol, epos, erow, ecol);
	}

	/**Gibt Delimiter-String (r:c-r:c) zurück.
	 * @return
	 */
	protected String delimToString() {
		return "(pos=" + this.bpos + "-" + this.epos + ";rowcol=" + this.brow + ":" + this.bcol + "-" + this.erow + ":" + this.ecol + ")";
	}

	/* (Kein Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return delimToString();
	}
}

/**
 * Datei: RowColumnNumerReader.java
 * Paket: de.beimax.testel.util
 * Projekt: TestEl
 *
 * Copyright (C) 2008 Maximilian Kalus.  All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package de.beimax.testel.util;

import java.io.*;
import java.nio.CharBuffer;

/**Ein Reader, mit dessen Hilfe man nicht nur Zeilennummern, sondern auch Spalten herauslesen kann.
 * Nur die Methode read() ist implementiert... die anderen Methoden geben Fehler zurück!!
 * @author mkalus
 *
 */
public class RowColumnNumerReader extends BufferedReader {
	private int row = 1;
	private int col = 1;
	private int lrow = 0; //last row/column
	private int lcol = 0;
	private int pos = 0; //Positionszähler allgemein
	
	public RowColumnNumerReader(Reader arg0) {
		super(arg0);
	}

	/**
	 * @return col
	 */
	public int getCol() {
		return this.col;
	}

	/**
	 * @return row
	 */
	public int getRow() {
		return this.row;
	}

	/**
	 * @return pos
	 */
	public int getPos() {
		return this.pos;
	}

	/**
	 * @return lcol
	 */
	public int getLastCol() {
		return this.lcol;
	}

	/**
	 * @return lrow
	 */
	public int getLastRow() {
		return this.lrow;
	}

	/* (Kein Javadoc)
	 * @see java.io.BufferedReader#read()
	 */
	@Override
	public int read() throws IOException {
		int back = super.read(); //abfangen
		//if (back == '\r') back = super.read();
		this.pos++;
		if (back == -1) return -1; //falls Ende, ok
		this.lcol = this.col; //last column/row festlegen
		this.lrow = this.row;
		if (back == '\n') {
			this.row++;
			this.col = 1;
		} else this.col++;
		
		return back;
	}

	/* (Kein Javadoc)
	 * @see java.io.BufferedReader#readLine()
	 */
	@Override
	public String readLine() throws IOException {
		throw new IOException("Not implemented! Use read() instead!");
	}

	/* (Kein Javadoc)
	 * @see java.io.BufferedReader#read(char[], int, int)
	 */
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		throw new IOException("Not implemented! Use read() instead!");
	}

	/* (Kein Javadoc)
	 * @see java.io.Reader#read(char[])
	 */
	@Override
	public int read(char[] arg0) throws IOException {
		throw new IOException("Not implemented! Use read() instead!");
	}

	/* (Kein Javadoc)
	 * @see java.io.Reader#read(java.nio.CharBuffer)
	 */
	@Override
	public int read(CharBuffer arg0) throws IOException {
		throw new IOException("Not implemented! Use read() instead!");
	}
}

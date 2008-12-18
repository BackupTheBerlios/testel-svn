/**
 * Datei: Statistician.java
 * Paket: de.beimax.testel.mime
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
package de.beimax.testel.mime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.token.TokenList;

/**Statistik-Aggregator für Dokumente, bzw. TagListen
 * @author mkalus
 *
 */
public abstract class Statistician {
	//Logger
	protected static final Logger logger = Logger.getLogger(Statistician.class.getName());
	
	/**
	 * TestElHandler handler Kontext
	 */
	protected TestElHandler handler;
	
	/**
	 * Enthält die Daten
	 */
	private HashMap<String, Object> map;
	
	/** Konstruktor
	 * @param handler
	 */
	protected Statistician(TestElHandler handler) {
		setHandler(handler);
		map = new HashMap<String, Object>();
	}
	
	/**Stellt einen Handler für den Statistiker ein
	 * @param handler
	 */
	public void setHandler(TestElHandler handler) {
		this.handler = handler;
	}
	
	/**Aggregiere Statistiken
	 * @param list
	 * @throws TestelException
	 */
	public abstract void aggregateStatistics(TokenList list) throws TestelException;
	
	/**Holt einen Statistikwert vom Statistiker
	 * @param key
	 * @return
	 */
	public String getStringStatistics(String key) {
		try {
			return (String) getStatistics(key);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**Holt einen numerischen Statistikwert vom Statistiker
	 * @param key
	 * @return
	 */
	public Double getDoubleStatistics(String key) {
		try {
			return (Double) getStatistics(key);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**Holt einen numerischen Statistikwert vom Statistiker
	 * @param key
	 * @return
	 */
	public Integer getIntegerStatistics(String key) {
		try {
			return (Integer) getStatistics(key);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**Kern des Statistik-Getters
	 * @param key
	 * @return
	 */
	public Object getStatistics(String key) {
		return map.get(key);
	}
	
	/**Fügt einen Wert zur Statistik hinzu
	 * @param key Schlüssel
	 * @param val Wert
	 */
	public void setStatistics(String key, Object val) {
		map.put(key, val);
	}
	
	/* (Kein Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer("Statistik:");
		
		Iterator<String> it = map.keySet().iterator();
		
		while (it.hasNext()) {
			String key = it.next();
			String val = map.get(key).toString();
			
			buffer.append("\n" + key + "=" + val);
		}
		
		return buffer.toString();
	}
}

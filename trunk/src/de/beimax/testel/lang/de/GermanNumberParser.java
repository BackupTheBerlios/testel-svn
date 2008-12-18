/**
 * Datei: GermanNumberParser.java
 * Paket: de.beimax.testel.lang.de
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
package de.beimax.testel.lang.de;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.lang.AbstractNumberParser;

/**
 * @author mkalus
 *
 */
public class GermanNumberParser extends AbstractNumberParser {
	private static GermanNumberParser singleton;
	private static boolean createerr = false; //kein Kreate err erzeugt
	
	/**Überschriebene Methode der Superklasse
	 * @return
	 * @throws Exception
	 */
	public static AbstractNumberParser getInstance(TestElHandler handler) throws Exception {
		//sprachspezifische Klasse zum Parsen von Nummern aufrufen, bzw. erzeugen
		if (singleton == null && !createerr) {
			try {
				singleton = new GermanNumberParser(handler);
			} catch (Exception e) {
				logger.warning("Fehler beim Erzeugen des Deutschen Nummernparsers:\n" + e.getLocalizedMessage());
				createerr = true; //Fehler nur einmal erzeugen...
				throw e;
			}
		}
		return singleton;
	}
	
	/** Konstruktor
	 * @param handler
	 * @throws TestelException
	 */
	protected GermanNumberParser(TestElHandler handler) throws TestelException {
		super(handler);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.lang.AbstractNumberParser#parseNumeric(java.lang.String)
	 */
	@Override
	public String parseNumeric(String strnum) throws Exception {
		//einfachsten Fall checken: Ganzzahl
		try {
			int check1 = Integer.parseInt(strnum);
			logger.fine(strnum + " wurde erkannt als Integer " + check1 + ".");
			
			return String.valueOf(check1);
		} catch (NumberFormatException e) {}
		
		//komplexerer Fall: Gleitzahl
		try {
			int index = strnum.indexOf(',');
			if (index > -1 && index != strnum.length()-1) {
				String num2 = new String(strnum).replace(",", ".");
				
				double check1 = Double.parseDouble(num2);
				
				logger.fine(strnum + " wurde erkannt als Gleitkommazahl " + check1 + ".");
				return String.valueOf(check1);
			}
		} catch (NumberFormatException e) {}
		
		//abgleich mit Zahlennamen
		if (map.containsKey(strnum.toLowerCase())) {
			Integer back = map.get(strnum.toLowerCase());
			
			logger.fine(strnum + " wurde erkannt als Gleitkommazahl " + back + ".");
			return String.valueOf(back);		
		}
		
		throw new Exception();
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#getName()
	 */
	public String getType() {
		return "Deutscher Nummern-Parser";
	}

}

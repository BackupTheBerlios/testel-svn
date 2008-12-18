/**
 * Datei: TaggerCollection.java
 * Paket: de.beimax.testel.general
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
package de.beimax.testel.general;

import java.util.LinkedList;
import java.util.logging.Logger;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.config.Config;
import de.beimax.testel.exception.TestelTaggerException;


/**Klasse enthält eine Liste von Taggern
 * @author mkalus
 *
 */
public class TaggerCollection extends LinkedList<Tagger> {
	//Logger
	static final Logger logger = Logger.getLogger(TaggerCollection.class.getName());

	private static final long serialVersionUID = 6304868362291593080L;

	/**Lädt eine TaggerCollection aufgrund von feststehenden Properties in
	 * testel.properties:
	 * Schlüssel in testel.properties: taggercollection_[mimetype] also
	 * z.B. taggercollection_text/html. Die Collection wird dort als
	 * Komma-Liste von Tagger-Klassen abgelegt
	 * @param mimetype
	 * @return
	 */
	public static TaggerCollection loadCollection(TestElHandler handler) throws TestelTaggerException {
		//Config-Eintrag holen
		String colstring = Config.getConfig("taggercollection_" + handler.getMimeType());
		
		if (colstring == null) throw new TestelTaggerException("Es wurde keine Tagger-Collection gefunden - existiert der Property-Eintrag taggercollection_" + handler.getMimeType() + "?");
		
		//Ok - jetzt aufteilen und Tagger erzeugen
		String[] colarray = colstring.split(",");
		TaggerCollection collection =  new TaggerCollection();
		
		for (int i = 0; i < colarray.length; i++) {
			Tagger tagger;
			try {
				Class taggerClass = Class.forName(colarray[i].trim());
				Object taggerObject = taggerClass.newInstance();
				tagger = (Tagger) taggerObject;
			} catch (Exception e) {
				logger.warning("Klasse " + colarray[i] + " existiert nicht oder ist keine " +
						"Implementation von Tagger!");
				throw new TestelTaggerException("Klasse " + colarray[i] + " existiert nicht oder ist keine " +
						"Implementation von Tagger!");
			}
			tagger.setHandler(handler);
			//zur Collection hinzufügen
			if (tagger != null) collection.add(tagger);
		}

		return collection;
	}

	/** Konstruktor
	 * ist privat, da Instantiierung über loadCollection funktioniert
	 */
	private TaggerCollection() {}
}

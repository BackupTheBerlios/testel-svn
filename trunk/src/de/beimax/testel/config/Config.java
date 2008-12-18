/**
 * Datei: Config.java
 * Paket: de.beimax.testel.config
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
package de.beimax.testel.config;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**Statische Klasse zur Konfiguration des Programms
 * @author mkalus
 *
 */
public class Config {
	//Logger
	private static final Logger logger = Logger.getLogger(Config.class.getName());

	//Properties für TestEl
	static private Properties config = new Properties();

	static public void init(String file) throws Exception {
		if (file == null) file = "testel.properties";
		try {
			config.load(new FileReader(file));
			logger.config("testel.properties geladen.");
		} catch (IOException e) {
			logger.warning("testel.properties (Datei " + file + ") konnten nicht geladen werden.");
			throw new Exception("testel.properties (Datei " + file + ") konnten nicht geladen werden.");
		}
	}
	
	/**Setzt eine Konfigurationseinstellung
	 * @param key
	 * @param value
	 */
	static public void setConfig(String key, String value) {
		logger.config("Property gesetzt: " + key + "=" + value + ".");
		config.setProperty(key, value);
	}
	
	/**Holt eine Konfigurationseinstellung
	 * @param key
	 * @return
	 */
	static public String getConfig(String key) {
		return config.getProperty(key);
	}
	
	/**Prüft einen Eintrag darauf, ob dieser einen bestimmten Wert hat oder nicht
	 * @param key
	 * @param value
	 * @return true, falls Wert (G/K-Schreibung ignoriert) richtig, false andernfalls (auch bei null) 
	 */
	static public boolean checkConfig(String key, String value) {
		String test = getConfig(key);
		if (test != null && test.equalsIgnoreCase(value)) return true;
		return false;
	}
}

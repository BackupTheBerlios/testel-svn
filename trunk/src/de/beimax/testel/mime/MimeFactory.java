/**
 * Datei: MimeFactory.java
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

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.config.Config;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.general.ReferenceTagger;
import de.beimax.testel.general.TaggerCollection;
import de.beimax.testel.general.TestElTagger;
import de.beimax.testel.util.IOHelper;

/**Abstrakte Fabrikklasse zur Erstellung Mime-abhängiger Klassen
 * @author mkalus
 *
 */
public abstract class MimeFactory {
	//Logger
	static final Logger logger = Logger.getLogger(MimeFactory.class.getName());

	/**Statische Methode zum Erzeugen einer spezifischen Fabrik-Instanz
	 * @param mimetype Mime-Typ (kurz) z.B. (text/html)
	 * @return
	 */
	public static MimeFactory buildFactory(String mimetype) throws TestelException {
		MimeFactory back = null;
		String mimefactoryimpl = Config.getConfig("mimefactory_" + mimetype);
		if (mimefactoryimpl == null) throw new TestelException("Kein Config-Eintrag für Mime-Type " + mimetype + " gefunden - existiert ein Eintrag mimefactory_" + mimetype + " in den Properties?");
		try {
			Class mimeParserClass = Class.forName(mimefactoryimpl);
			Object mimeParserObject = mimeParserClass.newInstance();
			back = (MimeFactory) mimeParserObject;
		} catch (Exception e) {
			String errormsg = "Konnte keine MimeFactory für Typ " + mimetype +
			" erzeugen (existiert ein Objekt " + mimefactoryimpl +
			"? Wenn nein, eines von MimeFactory ableiten!).";
			logger.warning(errormsg);
			throw new TestelException(errormsg);
		}
		if (back == null) throw new TestelException("Nach erfolgreicher Erzeugung von " + mimefactoryimpl + " ist das Objekt null - schwerer Fehler!");
		back.setMimeType(mimetype);

		logger.info("Sprach-Fabrik " + back.getClass() + " erzeugt");
		return back;
	}
	
	private String mimeType;
	private File mimeDir;
	
	/** Getter für mimeType
	 * @return mimeType
	 */
	public String getMimeType() {
		return mimeType;
	}

	/** Setter für mimeType
	 * @param mimeType Festzulegender mimeType
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**Erzeugt einen Normalisierer für diesen Mime-Typ
	 * @param handler
	 * @return
	 */
	public abstract Normalizer createNormalizer(TestElHandler handler);
	
	/**Erzeugt einen Parser für diesen Mime-Typ
	 * @param handler
	 * @return
	 */
	public abstract Parser createParser(TestElHandler handler);
	
	/**Erzeugt einen Statistiker für diesen Mime-Typ
	 * @param handler
	 * @return
	 */
	public abstract Statistician createStatistician(TestElHandler handler) throws TestelException;
	
	/**Erzeugt eine TaggerCollection für diesen Mime-Typ
	 * @param handler
	 * @return
	 */
	public abstract TaggerCollection createTaggerCollection(TestElHandler handler) throws TestelException;
	
	/**Erzeugt einen Referenz-Tagger für diesen Mime-Typ
	 * @param handler
	 * @param lang Sprachkürzel, z.B. "de"
	 * @return
	 */
	public abstract ReferenceTagger createReferenceTagger(TestElHandler handler, String lang) throws TestelException;

	/**Erzeugt einen TestElTagger für diesen Mime-Typ
	 * @param handler
	 * @param lang Sprachkürzel, z.B. "de"
	 * @return
	 */
	public abstract TestElTagger createTestElTagger(TestElHandler handler, String lang) throws TestelException;

	/**Gibt das Mime-Verzeichnis zurück
	 * @return
	 */
	public File getMimeDir() throws IOException {
		if (mimeDir == null) mimeDir = initMimeDir();
		return mimeDir;
	}
	
	/**Gibt eine Dateiendung samt . zurück.
	 * @return
	 */
	public abstract String getFileExtension();
	
	/**Konstruiere Mime-Verzeichnis
	 * @return Verzeichnis für Mime-Dateien
	 * @throws IOException falls Verzeichnis nicht existiert, nicht beschreibbar oder lesbar ist.
	 */
	private File initMimeDir() throws IOException {
		String dir = Config.getConfig("directory_" + mimeType);
		if (dir == null) dir = ".";
		File toCheck = new File(dir);
		logger.finer("Mime-Verzeichnis ist: " + toCheck.toString());
		
		IOHelper.checkDir(toCheck.getAbsolutePath());
		
		return toCheck;
	}
}

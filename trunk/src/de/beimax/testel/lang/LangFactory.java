/**
 * Datei: LangFactory.java
 * Paket: de.beimax.testel.lang
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
package de.beimax.testel.lang;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.config.Config;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.general.Tagger;
import de.beimax.testel.util.IOHelper;

/**Abstrakte Fabrikklasse zur Erstellung sprachabhängiger Klassen
 * @author mkalus
 *
 */
public abstract class LangFactory {
	//Logger
	static final Logger logger = Logger.getLogger(LangFactory.class.getName());

	/**Statische Methode zum Erzeugen einer spezifischen Fabrik-Instanz
	 * @param language Sprachkürzel, z.B. "de"
	 * @return
	 * @throws TestelException
	 */
	public static LangFactory buildFactory(String language) throws TestelException {
		LangFactory back = null;
		try {
			Class langParserClass = Class.forName("de.beimax.testel.lang." + language + ".LangFactoryImpl");
			Object langParserObject = langParserClass.newInstance();
			back = (LangFactory) langParserObject;
		} catch (Exception e) {
			String errormsg = "Konnte keine LangFactory für Sprache " + language +
			" erzeugen (existiert ein Objekt de.beimax.testel.lang." +
			language + ".LangFactoryImpl? Wenn nein, eines von LangFactory ableiten!).";
			logger.warning(errormsg);
			throw new TestelException(errormsg);
		}
		if (back == null) throw new TestelException("Nach erfolgreicher Erzeugung von de.beimax.testel.lang." + language + ".LangFactoryImpl ist das Objekt null - schwerer Fehler!");
		
		//Sprache einstellen
		back.setLang(language);

		logger.info("Sprach-Fabrik " + back.getClass() + " erzeugt");
		return back;
	}

	//=================================== Instanz
	
	private String lang;
	private File langDir = null;
	
	/** Getter für lang
	 * @return lang
	 */
	public String getLang() {
		return lang;
	}

	/** Setter für lang
	 * @param lang Festzulegender lang
	 */
	private void setLang(String lang) {
		this.lang = lang;
	}
	
	/**Gibt das Sprachverzeichnis zurück
	 * @return
	 */
	public File getLangDir() throws IOException {
		if (langDir == null) langDir = initLangDir();
		return langDir;
	}
	
	/**Konstruiere Sprachverzeichnis
	 * @return Verzeichnis für Sprachdateien
	 * @throws IOException falls Verzeichnis nicht existiert, nicht beschreibbar oder lesbar ist.
	 */
	private File initLangDir() throws IOException {
		String dir = Config.getConfig("languages-dir");
		if (dir == null) dir = ".";
		File toCheck = new File(dir, getLang());
		logger.finer("Sprach-Verzeichnis ist: " + toCheck.toString());
		
		IOHelper.checkDir(toCheck.getAbsolutePath());
		
		return toCheck;
	}

	/**Erzeugt einen LanguageTagger für diese Sprache
	 * @param handler
	 * @return
	 */
	public abstract Tagger createLangTagger(TestElHandler handler);
	
	/**Erzeugt einen Nummern-Parser für diese Sprache
	 * @param handler
	 * @return
	 */
	public abstract AbstractNumberParser createNumberParser(TestElHandler handler) throws Exception;
	
	/**Methode, die einen neuen AbbreviationSubTagger zurückgibt - kann sprachspezifisch
	 * überschrieben werden - Handler muss allerdings per Hand gesetzt werden!
	 * @return
	 */
	public AbbreviationSubTagger getAbbreviationSubTagger() {
		return new AbbreviationSubTagger();
	}

	/**Methode, die einen neuen PunctuationSubTagger zurückgibt - kann sprachspezifisch
	 * überschrieben werden - Handler muss allerdings per Hand gesetzt werden!
	 * Standardmäßig wird PunctuationSubTaggerArray zurückgegeben.
	 * @return
	 */
	public PunctuationSubTagger getPunctuationSubTagger() {
		return new PunctuationSubTaggerUTF();
	}

	/**Methode, die einen neuen MeaningfullWordsSubTagger zurückgibt - kann sprachspezifisch
	 * überschrieben werden - Handler muss allerdings per Hand gesetzt werden!
	 * @return
	 */
	public MeaningfulWordsSubTagger getMeaningfulWordsSubTagger() {
		return new MeaningfulWordsSubTagger();
	}
}

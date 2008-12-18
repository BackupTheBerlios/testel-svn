/**
 * Datei: TestElTagger.java
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

import java.io.File;
import java.io.IOException;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.classifier.Classifier;
import de.beimax.testel.classifier.ClassifierCollection;
import de.beimax.testel.classifier.ClassifierFactory;
import de.beimax.testel.config.Config;
import de.beimax.testel.exception.TestelClassifierException;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.token.SubTokenList;
import de.beimax.testel.token.TokenList;
import de.beimax.testel.util.XStreamHelper;

/**
 * @author mkalus
 *
 */
public class TestElTagger implements Tagger {
	/**
	 * statische Elemente, die bei mehreren Handlern gleich bleiben sollten...
	 */
	private static ClassifierCollection classifierCollection = null; //Referenz auf die KlassifiziereStruktur
	private static File fileRef = null; //statische Struktur hat welche DateiReferenz?

	private TestElHandler handler;
	private String language;
	private String classifierName;
	
	/** Konstruktor
	 * @param handler
	 * @param language
	 */
	public TestElTagger(TestElHandler handler, String language) throws TestelException {
		setHandler(handler);
		this.language = language;
		//Dateinamenerweiterung des Klassifizierers holen
		Classifier classifier = ClassifierFactory.getClassifier(handler);
		classifierName = classifier.getFileNameExt();
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Tagger#getType()
	 */
	public String getType() {
		return "TestElTagger für Mime " + handler.getMimeType() + " und Sprache " + language;
	}

	/** Getter für classifierName
	 * @return classifierName
	 */
	public String getClassifierName() {
		return classifierName;
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Tagger#setHandler(de.beimax.testel.TestElHandler)
	 */
	public void setHandler(TestElHandler handler) {
		this.handler = handler;
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Tagger#tag(de.beimax.testel.token.TokenList)
	 */
	public TokenList tag(TokenList list) throws TestelTaggerException {
		//Datenstruktur ggf. laden
		try {
			loadClassifiersFile();
		} catch (TestelException e) {
			throw new TestelTaggerException("Konnte Klassifiziererdaten nicht initialisiernen:\n" + e.getLocalizedMessage());
		}
		
//		//erweiterter Modus?
//		boolean includeExtendedTags;
//		if (handler.getMode() == TestElHandler.MODE_PRETRAIN) includeExtendedTags = true;
//		else includeExtendedTags = false;
//		
		//eigentliche Arbeit macht die Kollektion...
		String msg = "Verwende '" + classifierCollection.getClassifierName() + "' als Klassifizierungs-Engine";
		logger.info(msg);
		if (!handler.isQuiet()) System.out.println(msg);
		
		try {
			return classifierCollection.tag(list);
		} catch (TestelTaggerException e) {
			throw new TestelTaggerException("Fehler beim Taggen der TestEl-Tags:\n" + e.getLocalizedMessage());
		}
	}
	
	/**Fügt eine Tokenlist in die Klassifiziererkollektion ein und gibt true zurück, wenn
	 * sie wirklich eingefügt wurde, bzw. die Klassifiziererliste geädnert wurde
	 * (false, falls schon vorhanden war).
	 * @param subList Unterliste
	 * @param start Startposition der Unterliste in der Gesamtliste
	 * @param stop  Endposition der Unterliste in der Gesamtliste
	 * @param completeList Komplette Liste
	 * @return
	 * @throws TestelException
	 */
	public boolean addNewTokenList(SubTokenList subList, TokenList completeList) throws TestelException {
		//einfach an den Klassifizierer weiterreichen
		return classifierCollection.add(subList, completeList);
	}
	
	/**
	 * Lädt die Struktur für aktuellen Mime-Type und aktuelle Sprache in
	 * den Hauptspeicher
	 */
	public void loadClassifiersFile() throws TestelException {
		File referenceFile = getClassifiersFileName();
		if (referenceFile != null && referenceFile.equals(fileRef)) {
			logger.fine("Klassifiziererdatei muss nicht noch einmal neu geladen werden");
			return;
		}
		
		//neu laden
		fileRef = referenceFile;
		
		//existiert Datei überhaupt?
		if (referenceFile.exists()) { //ja: aus XML-Datei laden
			XStreamHelper<ClassifierCollection> xmlloader = new XStreamHelper<ClassifierCollection>();
			try {
				classifierCollection = xmlloader.loadXML(referenceFile, classifiersFileIsGzipped());
				logger.info("Klassifiziererdatei " + referenceFile + " erfolgreich geladen");
			} catch (IOException e) {
				throw new TestelException("Konnte " + referenceFile + " nicht in Datenstruktur umwandeln - Daten offenbar korrput:\n"+ e.getLocalizedMessage());
			}
		} else { //nein: neue Datei erstellen
			logger.warning("Keine Klassifiziererdatei " + referenceFile + " vorhanden - erstelle neue Kollektion");
			//Klasse des Klassifizierers holen, um die richtige Klasse zu holen
			Classifier classifier = ClassifierFactory.getClassifier(handler);
			classifierCollection = new ClassifierCollection(classifier.getClass());
		}
	}
	
	/**Speichert die im Tagger enthaltene Struktur in Datei ab
	 * @throws TestelException
	 */
	public void saveClassifiersFile() throws TestelException {
		if (classifierCollection == null) throw new TestelException("Klassifiziererdaten wurden nicht initialisiert");
		File referenceFile = getClassifiersFileName();
		
		//speichern
		XStreamHelper<ClassifierCollection> xmlsaver = new XStreamHelper<ClassifierCollection>();
		try {
			xmlsaver.saveXML(referenceFile, classifierCollection, classifiersFileIsGzipped());
			logger.info("Klassifiziererdatei " + referenceFile + " erfolgreich gespeichert");
		} catch (IOException e) {
			throw new TestelException("Konnte Klassifiziererdatei nicht speichern:\n" + e.getLocalizedMessage());
		}
	}

	/**Holt die Dateireferenz der Klassifizierer-XML-Datei
	 * @return
	 * @throws TestelException
	 */
	public File getClassifiersFileName() throws TestelException {
		//evt Endung?
		String gzip = "";
		if (classifiersFileIsGzipped()) gzip = ".gz"; 
		try {
			return new File(handler.getMimeFactory().getMimeDir(), "classifiers_" + classifierName + "_" + language + ".xml" + gzip);
		} catch (IOException e) {
			throw new TestelException("Konnte Klassifizierer-Dateinamen nicht erstellen:\n" + e.getLocalizedMessage());
		}
	}

	/**True, falls Einstellungen in testel.properties gzip verlangen
	 * @return
	 */
	private boolean classifiersFileIsGzipped() {
		String gzip = Config.getConfig("gzip_classifiers_" + handler.getMimeType());
		if (gzip == null || !gzip.equalsIgnoreCase("true")) return false;
		return true;
	}

	public Classifier getNewClassifier() throws TestelException {
		if (classifierCollection == null) throw new TestelException("Klassifiziererdaten wurden nicht initialisiert");
		try {
			return classifierCollection.getNewClassifier();
		} catch (TestelClassifierException e) {
			throw new TestelException("Neuer Klassifizierer konnte nicht erstellt werden:\n" + e.getLocalizedMessage());
		}
	}
}

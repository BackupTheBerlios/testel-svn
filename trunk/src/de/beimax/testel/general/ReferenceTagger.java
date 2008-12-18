/**
 * Datei: ReferenceTagger.java
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
import java.util.ListIterator;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.config.Config;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;
import de.beimax.testel.util.XStreamHelper;

/**Tagger für Referenzen, also Tags des Typs testel:ref
 * @author mkalus
 *
 */
/**
 * @author mkalus
 *
 */
/**
 * @author mkalus
 *
 */
public class ReferenceTagger implements Tagger {
	/**
	 * statische Elemente, die bei mehreren Handlern gleich bleiben sollten...
	 */
	private static ReferenceList referenceList = null; //Liste der Referenzlisten
	private static File fileRef = null; //statische Struktur hat welche DateiReferenz?

	private TestElHandler handler;
	private String language;
	
	/** Konstruktor
	 * @param handler
	 * @param language
	 */
	public ReferenceTagger(TestElHandler handler, String language) {
		setHandler(handler);
		this.language = language;
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Tagger#getType()
	 */
	public String getType() {
		return "Referenzen-Tagger für Mime " + handler.getMimeType() + " und Sprache " + language;
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
		//Referenzliste laden
		try {
			loadReferenceFile();
		} catch (TestelException e) {
			throw new TestelTaggerException("Konnte Referenzdatei nicht zum Taggen laden:\n" + e.getLocalizedMessage());
		}
		
		//Iteration der Liste
		ListIterator<Token> it = list.listIterator();
		while (it.hasNext()) {
			Token tok = it.next();
			//nächstes interessantes Token suchen und Match laufen lassen
			if (tok.isTextToken() && referenceList.match(list, tok, it))
					logger.info("Referenz-Match ab: " + tok);
		}

		return list;
	}

	/**
	 * Lädt die Struktur für aktuellen Mime-Type und aktuelle Sprache in
	 * den Hauptspeicher
	 */
	public void loadReferenceFile() throws TestelException {
		File referenceFile = getReferenceFileName();
		if (referenceFile != null && referenceFile.equals(fileRef)) {
			logger.fine("Referenzdatei muss nicht noch einmal neu geladen werden");
			return;
		}
		
		//neu laden
		fileRef = referenceFile;
		
		//existiert Datei überhaupt?
		if (referenceFile.exists()) { //ja: aus XML-Datei laden
			XStreamHelper<ReferenceList> xmlloader = new XStreamHelper<ReferenceList>();
			try {
				referenceList = xmlloader.loadXML(referenceFile, referenceFileIsGzipped());
				logger.info("Referenzdatei " + referenceFile + " erfolgreich geladen");
			} catch (IOException e) {
				throw new TestelException("Konnte " + referenceFile + " nicht in Datenstruktur umwandeln - Daten offenbar korrput:\n"+ e.getLocalizedMessage());
			}
		} else { //nein: neue Datei erstellen
			logger.warning("Keine Referenzdatei " + referenceFile + " vorhanden - erstelle neue Liste");
			referenceList = new ReferenceList();
		}
	}
	
	/**Speichert die im Tagger enthaltene Struktur in Datei ab
	 * @throws TestelException
	 */
	public void saveReferenceFile() throws TestelException {
		if (referenceList == null) throw new TestelException("Referenzdaten wurden nicht initialisiert");
		File referenceFile = getReferenceFileName();
		
		//speichern
		XStreamHelper<ReferenceList> xmlsaver = new XStreamHelper<ReferenceList>();
		try {
			xmlsaver.saveXML(referenceFile, referenceList, referenceFileIsGzipped());
			logger.info("Referenzdatei " + referenceFile + " erfolgreich gespeichert");
		} catch (IOException e) {
			throw new TestelException("Konnte Referenzliste nicht speichern:\n" + e.getLocalizedMessage());
		}
	}

	/**Holt die Dateireferenz der Referenz-XML-Datei
	 * @return
	 * @throws TestelException
	 */
	public File getReferenceFileName() throws TestelException {
		//evt Endung?
		String gzip = "";
		if (referenceFileIsGzipped()) gzip = ".gz"; 
		try {
			return new File(handler.getMimeFactory().getMimeDir(), "references_" + language + ".xml" + gzip);
		} catch (IOException e) {
			throw new TestelException("Konnte Referenz-Dateinamen nicht erstellen:\n" + e.getLocalizedMessage());
		}
	}
	
	/**Fügt neue Elemente zur Liste hinzu, falls diese noch nicht vorhanden
	 * @param list
	 * @param type
	 * @return true, falls neues Element eingetragen wurde
	 */
	public boolean addToList(TokenList list, String type) throws TestelException {
		if (referenceList == null) throw new TestelException("Referenzdaten wurden nicht initialisiert");
		
		boolean added = referenceList.add(list, type);
		
		if (added) logger.fine("Neues Referenz-Element in Liste eingetragen: " + type);
		else logger.fine("Referenz-Element schon in der Liste: " + type);
		
		return added;
	}

	/**True, falls Einstellungen in testel.properties gzip verlangen
	 * @return
	 */
	private boolean referenceFileIsGzipped() {
		String gzip = Config.getConfig("gzip_reference_" + handler.getMimeType());
		if (gzip == null || !gzip.equalsIgnoreCase("true")) return false;
		return true;
	}
}

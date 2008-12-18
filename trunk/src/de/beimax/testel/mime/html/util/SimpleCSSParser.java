/**
 * Datei: SimpleCSSParser.java
 * Paket: de.beimax.testel.mime.html.util
 * Projekt: Testel
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
package de.beimax.testel.mime.html.util;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Stack;
import java.util.logging.Logger;

import org.w3c.css.sac.*;
import org.w3c.css.sac.helpers.ParserFactory;

import com.steadystate.css.parser.*;

import de.beimax.testel.config.Config;
import de.beimax.testel.exception.TestelParserException;
import de.beimax.testel.util.IOHelper;

/**Einfacher CSS-Parser, der vom Normalisierer evt. aufgerufen wird.
 * @author mkalus
 *
 */
public class SimpleCSSParser extends HandlerBase {
	private static final Logger logger = Logger.getLogger(SimpleCSSParser.class.getName());
	private static final String PARSER = "com.steadystate.css.parser.SACParser";

	//enthält bereits geparste Listen
	private static ArrayList<URL> parsedStylesheets = new ArrayList<URL>();

	private int _propertyCounter = 0;

	private int _indentSize = 0;
	
	private String stylesheet;
	private URL styleURL;
	private String styleBasePath;
	private String mediatype; //default-Medientyp
	
	//enthält einen Stack, der verschachtelte Media-Komponenten enthält
	private Stack<Boolean> inMedia = new Stack<Boolean>();
	boolean acceptMedia = true;

	/**Statische Methode zum Verwalten der Liste schon bearbeiteter Methoden.
	 * @param url
	 * @return
	 */
	private static boolean addtoList(URL url) {
		if (parsedStylesheets.contains(url)) {
			return false;
		}
		parsedStylesheets.add(url);
		return true;
	}
	
	/**Statische Methode zum Leeren der Liste.
	 * 
	 */
	public static void clearList() {
		parsedStylesheets = new ArrayList<URL>();
	}
	
	/**
	 * Konstructor
	 */
	public SimpleCSSParser() {
		stylesheet = "";
		mediatype = Config.getConfig("CSS_mediatype"); //aus Konfiguration holen
		if (mediatype == null) mediatype = "screen";
	}

	/**Getter für Stylesheet
	 * @return stylesheet
	 */
	public String getStylesheet() {
		return stylesheet;
	}
	
	/**Medientyp wird zurückgegeben
	 * @return mediatype
	 */
	public String getMediatype() {
		return mediatype;
	}

	/**Setzt den Medientpyen, z.B. auf "screen"
	 * @param mediatype Festzulegender mediatype
	 */
	public void setMediatype(String mediatype) {
		this.mediatype = mediatype;
	}

	/** Hauptmethode zum Parsen eines Stylesheets - URL als Eingabe-Parameter
	 * @param url
	 * @throws TestelParserException
	 */
	public String parse(URL url) throws TestelParserException, IOException {
		String myurl = url.toString();
		
		//Stylesheet in Liste einfügen - check, ob schon gemacht
		if (!addtoList(url)) { //Aufruf statische Methode
			logger.info("parse() " + myurl + " wurde schon geparsed - überspringe.");
			return getStylesheet();
		}

		//Instanzvariablen belegen
		this.styleURL = url;
		try {
			this.styleBasePath = IOHelper.getPath(url);
		} catch (MalformedURLException e) {
			logger.warning("Basispfad von " + myurl + " konnte nicht bestimmt werden.");
			throw new TestelParserException("Basispfad von " + myurl + " konnte nicht bestimmt werden.");
		}
		this.stylesheet = ""; //neu anlegen
		
		//Stylesheet laden
		String sheet;
		
		try {
			sheet = IOHelper.streamtoString(IOHelper.loadURL(url));
		} catch (IOException e1) {
			logger.warning("Konnte Stylesheet " + myurl + " nicht laden.");
			throw e1;
		}
		logger.info("Stylesheet " + myurl + " wurde geladen.");
		
		//Überprüfung, ob die Datei tatsächlich CSS ist
		if (IOHelper.HTMLcheck(sheet)) {
			logger.warning(myurl + " ist eine HTML-Datei (evt. abgefangener 404-Fehler) - ignoriert.");
			return getStylesheet();
		}
		
		//Parser starten
		CSSOMParser.setProperty("org.w3c.css.sac.parser", PARSER);
		ParserFactory factory = new ParserFactory();
		
		try {
			Parser parser = factory.makeParser();
			
			parser.setDocumentHandler(this);
			
			parser.parseStyleSheet(new InputSource(new StringReader(sheet)));
		} catch (Exception e) {
			logger.severe("CSS-Parser erzeugte Ausnahmefehler in " + myurl + ".");
			throw new TestelParserException("CSS-Parser erzeugte Ausnahmefehler " + myurl + ".");
		}
		
		return getStylesheet();
	}
	
	/** Hauptmethode zum Parsen eines Stylesheets - String als Eingabe-Parameter
	 * @param uri
	 * @throws TestelParserException
	 */
	public String parse(String uri) throws TestelParserException, IOException {
		URL url;
		try {
			url = IOHelper.createURL(uri);
		} catch (MalformedURLException e) {
			logger.warning("Konnte URI " + uri + " nicht in URL umwandeln.");
			throw new TestelParserException("Konnte URI " + uri + " nicht in URL umwandeln.");
		}
		return parse(url);
	}
	
	
	/* (Kein Javadoc)
	 * @see com.steadystate.css.parser.HandlerBase#startDocument(org.w3c.css.sac.InputSource)
	 */
	public void startDocument(InputSource source) throws CSSException {
		logger.info("Stylesheet " + this.styleURL.toString() + " wird geparst.");
		if (stylesheet == null) {
			logger.warning("Stylesheet nicht definiert im CSSParser.");
			throw new CSSException("Stylesheet nicht definiert im CSSParser.");
		}
	}

	/* (Kein Javadoc)
	 * @see com.steadystate.css.parser.HandlerBase#endDocument(org.w3c.css.sac.InputSource)
	 */
	@Override
	public void endDocument(InputSource source) throws CSSException {
		logger.info("Stylesheet " + this.styleURL.toString() + " vollständig geparst.");
	}

	/* (Kein Javadoc)
	 * @see com.steadystate.css.parser.HandlerBase#importStyle(java.lang.String, org.w3c.css.sac.SACMediaList, java.lang.String)
	 */
	@Override
	public void importStyle(String uri, SACMediaList media, String defaultNamespaceURI) throws CSSException {
		//nur importieren, falls Medienart ok
		if (!checkMedia(media, getMediatype(), true)) {
			logger.fine("Stylesheet " + uri + " ist nicht Medientyp Screen - ignoriert.");
			return;
		}

		//zuerst URL erzeugen und checken...
		URL url;
		try {
			url = IOHelper.createURL(uri);
		} catch (MalformedURLException e) {
			logger.info("Fehlerhafte URL " + uri + " - ignoriert.");
			return;
		}
		
		//ist die Referenz nicht absolut => aktuellen Pfad hinzufügen
		if (!IOHelper.isAbsolute(url)) {
			try {
				url = IOHelper.createURL(this.styleBasePath + uri);
			} catch (MalformedURLException e) {
				logger.info("Fehlerhafte URL [nach Pfadanpassung] " + this.styleBasePath + uri + " - ignoriert.");
				return;
			}
		}
		
		//Ok, nun Sheet laden
		String newstylesheet = "";
		logger.info("@import url(" + url.toString() + ")");
		SimpleCSSParser parser = new SimpleCSSParser();
		try {
			newstylesheet = parser.parse(url);
		} catch (TestelParserException e) {
			logger.severe("Parse-Fehler in " + url.toString() + ".");
			throw new CSSException();
		} catch (IOException e) {
			logger.info("Konnte " + url.toString() + " nicht laden - ignoriert.");
			return;
		}
		
		//Stylesheet-Daten hinzufügen
		this.stylesheet += newstylesheet;
	}

	/* (Kein Javadoc)
	 * @see com.steadystate.css.parser.HandlerBase#startMedia(org.w3c.css.sac.SACMediaList)
	 */
	public void startMedia(SACMediaList media) throws CSSException {
		inMedia.push(new Boolean(acceptMedia)); //letztes Flag auf Stack
		acceptMedia = checkMedia(media, getMediatype(), true); //Bearbeitungsflag setzen
	}

	/* (Kein Javadoc)
	 * @see com.steadystate.css.parser.HandlerBase#endMedia(org.w3c.css.sac.SACMediaList)
	 */
	public void endMedia(SACMediaList media) throws CSSException {
		acceptMedia = inMedia.pop(); //Element entfernen
	}

	/* (Kein Javadoc)
	 * @see com.steadystate.css.parser.HandlerBase#startPage(java.lang.String, java.lang.String)
	 */
	public void startPage(String name, String pseudo_page) throws CSSException {
		inMedia.push(new Boolean(acceptMedia)); //letztes Flag auf Stack
		acceptMedia = false; //immer ignorieren
	}

	/* (Kein Javadoc)
	 * @see com.steadystate.css.parser.HandlerBase#endPage(java.lang.String, java.lang.String)
	 */
	public void endPage(String name, String pseudo_page) throws CSSException {
		acceptMedia = inMedia.pop(); //Element entfernen
	}

	/* (Kein Javadoc)
	 * @see com.steadystate.css.parser.HandlerBase#startFontFace()
	 */
	public void startFontFace() throws CSSException {
		if (!acceptMedia) return; //falls Bearbeitung ausgeschaltet ist, ignorieren
		//System.out.println(indent() + "@font-face {");
		stylesheet += indent() + "@font-face {\n";
		_propertyCounter = 0;
		incIndent();
	}

	/* (Kein Javadoc)
	 * @see com.steadystate.css.parser.HandlerBase#endFontFace()
	 */
	public void endFontFace() throws CSSException {
		if (!acceptMedia) return; //falls Bearbeitung ausgeschaltet ist, ignorieren
		//System.out.println();
		stylesheet += "\n";
		decIndent();
		//System.out.println(indent() + "}");
		stylesheet += indent() + "}\n";
	}

	/* (Kein Javadoc)
	 * @see com.steadystate.css.parser.HandlerBase#startSelector(org.w3c.css.sac.SelectorList)
	 */
	public void startSelector(SelectorList selectors) throws CSSException {
		if (!acceptMedia) return; //falls Bearbeitung ausgeschaltet ist, ignorieren
		//System.out.println(indent() + selectors.toString() + " {");
		stylesheet += indent() + selectors.toString() + " {\n";
		_propertyCounter = 0;
		incIndent();
	}

	/* (Kein Javadoc)
	 * @see com.steadystate.css.parser.HandlerBase#endSelector(org.w3c.css.sac.SelectorList)
	 */
	public void endSelector(SelectorList selectors) throws CSSException {
		if (!acceptMedia) return; //falls Bearbeitung ausgeschaltet ist, ignorieren
		//System.out.println();
		stylesheet += "\n";
		decIndent();
		//System.out.println(indent() + "}");
		stylesheet += indent() + "}\n";
	}

	/* (Kein Javadoc)
	 * @see com.steadystate.css.parser.HandlerBase#property(java.lang.String, org.w3c.css.sac.LexicalUnit, boolean)
	 */
	public void property(String name, LexicalUnit value, boolean important)
			throws CSSException {
		if (!acceptMedia) return; //falls Bearbeitung ausgeschaltet ist, ignorieren
		if (_propertyCounter++ > 0) {
			//System.out.println(";");
			stylesheet += ";\n";
		}
		//System.out.print(indent() + name + ":");
		stylesheet += indent() + name + ":";

		// Iterate through the chain of lexical units
		LexicalUnit nextVal = value;
		while (nextVal != null) {
			//            System.out.print(" " + nextVal.toString());
			//System.out.print(" " + ((LexicalUnitImpl) nextVal).toDebugString());
			stylesheet += " " + nextVal.toString();
			nextVal = nextVal.getNextLexicalUnit();
		}

		// Is it important?
		if (important) {
			//System.out.print(" !important");
			stylesheet += " !important";
		}
	}

	private String indent() {
		StringBuffer sb = new StringBuffer(16);
		for (int i = 0; i < _indentSize; i++) {
			sb.append(" ");
		}
		return sb.toString();
	}

	private void incIndent() {
		_indentSize += 4;
	}

	private void decIndent() {
		_indentSize -= 4;
	}
	
	/**
	 * Prüft, ob eine Medienliste einen bestimmten Eintrag enthält
	 * @param media Medienliste
	 * @param search Suchbegriff
	 * @param emptyok true, falls eine leere Liste ok ist (dann true zurück)
	 * @return
	 */
	private boolean checkMedia(SACMediaList media, String search, boolean emptyok) {
		//falls leere Liste
		if (media.getLength() == 0) {
			if (emptyok) return true;
			return false;
		}
		
		//sonst: check der Liste
		for (int i = 0; i < media.getLength(); i++)
			if (media.item(i).toLowerCase().equals(search.toLowerCase())) return true;
		
		return false;
	}
}

/**
 * Datei: HTMLNormalizer.java
 * Paket: de.beimax.testel.mime.html
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
package de.beimax.testel.mime.html;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.tidy.Tidy;
import org.xml.sax.SAXException;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.config.Config;
import de.beimax.testel.exception.TestelParserException;
import de.beimax.testel.mime.Normalizer;
import de.beimax.testel.mime.html.util.HTMLTransformer;
import de.beimax.testel.mime.html.util.SimpleCSSParser;
import de.beimax.testel.util.IOHelper;

/**
 * @author mkalus
 *
 */
public class HTMLNormalizer implements Normalizer {
	TestElHandler handler;
	
	/** Konstruktor
	 * @param handler
	 */
	public HTMLNormalizer(TestElHandler handler) {
		setHandler(handler);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Normalizer#setHandler(de.beimax.testel.TestElHandler)
	 */
	public void setHandler(TestElHandler handler) {
		this.handler = handler;
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Normalizer#normalize(java.io.File, java.net.URL)
	 */
	public String normalize(String htmldoc) throws Exception {
		//CSS-Tags in HTML-Datei mitaufnehmen, falls eingeschaltet
		if (Config.checkConfig("HTMLincludecss", "true"))
			htmldoc = inlcudeCSS(htmldoc);

		//Lobo-Transform laufen lassen, falls eingeschaltet
		if (Config.checkConfig("HTMLtransform", "true"))
			htmldoc = transformHTML(htmldoc);

		//Tidy laufen lassen, falls eingeschaltet
		if (Config.checkConfig("HTMLtidy", "true")) {
			htmldoc = preTidyHTML(htmldoc);
			htmldoc = tidyHTML(htmldoc);
			htmldoc = htmldoc.replace("&nbsp;", " "); //Leerzeichen anpassen
		}

		return htmldoc;
	}

	/**Vorgeschalteter Prozess, der Dinge säubert, mit denen Tidy nicht zurecht kommt und
	 * den Parser verwirren
	 * @param htmldoc
	 * @return
	 */
	protected String preTidyHTML(String htmldoc) {
		StringBuffer buffer = new StringBuffer(htmldoc);
		
		//Zeichen „ und “ innerhalb von Tags ersetzen
		Pattern p = Pattern.compile("<[^>]*?(„|“).*?>");
		Matcher m = p.matcher(buffer);
		while(m.find()) {
			String old = m.group(0);
			String newm = old.replace("„", "&quot;");
			newm = newm.replace("“", "&quot;");
			//ersetzen
			buffer.replace(m.start(), m.end(), newm);
			m.reset(); //damit die Positionen nicht verrutschen...
		}
		
		//fehlerhafte von iso-8859-1 Teile ausbessern
		String replace = buffer.toString();
		
		Character[] isoreplace = {'€', ' ', '‚', 'ƒ', '„', '…', '†',
				'‡', 'ˆ', '‰', 'Š', '‹', 'Œ', ' ', 'Ž', ' ', 
				' ', '‘', '’', '“', '”', '•', '–',
				'—', '˜', '™', 'š', '›', 'œ', ' ', 'ž', 'Ÿ'};
		
		for (int i = 0; i < isoreplace.length; i++)
			replace = replace.replace((char)(128 + i), isoreplace[i]);
		
		//NULL ersetzen
		replace = replace.replace((char)(0), ' ');
		
		return replace.replace("&nbsp;", " "); //Leerzeichen anpassen
	}
	
	/**Erwartet unsauberes HTML als Eingabe und lässt Tidy darüber laufen.
	 * @param htmldoc
	 * @return Saubere Stringrepräsentation des Dokuments.
	 */
	protected String tidyHTML(String htmldoc) {
		Tidy tidy = new Tidy();
		
		String tidyConfig = Config.getConfig("HTMLtidy_config");
		if (tidyConfig == null) {
			logger.warning("Property HTMLtidy_config ist leer! Versuche Fallback auf ./tidy.config.");
			tidyConfig = "./tidy.config.";
		}
		
		//Einstellungen
		if (handler != null)
			tidy.setInputStreamName(handler.getURL().toString());
		tidy.setForceOutput(true);
		logger.fine("Tidy-Konfigurationsdatei: " + tidyConfig + ".");
		if (tidyConfig.equals("default"))
			tidy.setConfigurationFromFile(getClass().getResource("tidy.config").getFile());
		else tidy.setConfigurationFromFile(tidyConfig);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        //Parsen
		tidy.parse(new ByteArrayInputStream(htmldoc.getBytes()), os);
		logger.info("Tidy abgeschlossen");
		
		return os.toString();
	}

	/**HTML-Transformer laden
	 * @param htmldoc
	 * @return
	 */
	protected String transformHTML(String htmldoc) {
		HTMLTransformer transformer;
		
		try {
			transformer = new HTMLTransformer(htmldoc, handler.getURL());

			return transformer.transform();
		} catch (IOException e) {
			logger.warning("Lobo konnte Dokument nicht einlesen - überspringe Lobo.\n" + e.getStackTrace());
		} catch (SAXException e) {
			logger.warning("Lobo konnte Dokument nicht parsen (SAX Exception) - überspringe Lobo.\n" + e.getStackTrace());
		} catch (InterruptedException e) {
			logger.warning("Lobo wurde unterbochen - überspringe Lobo.\n" + e.getStackTrace());
		}

		return htmldoc; //Bei Fehlern Original zurückliefern
	}
	
	/**SimpleCSS-Includer einbinden
	 * @param htmldoc
	 * @return
	 */
	protected String inlcudeCSS(String htmldoc) {
		//Pfad zum CSS finden
		String path;
		try {
			path = IOHelper.getPath(handler.getURL());
		} catch (MalformedURLException e) {
			logger.severe("Fehlerhafte URL " + handler.getURL().toString() + " beim CSS-Matching: breche es ab und kehre zurück.");
			return htmldoc;
		}
		
		//CSS-Einträge aus der Datei holen und bearbeiten
		Pattern p = Pattern.compile("<link.*?rel.*?=.*?\"stylesheet\".*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher m = p.matcher(htmldoc);
		SimpleCSSParser parser = new SimpleCSSParser();
		String styles = "";
		
		while (m.find()) {
			//System.out.println(m.group() + ":" + m.start() + ":" + m.end());
			Pattern elem = Pattern.compile("href=\".*?\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
			Matcher elm = elem.matcher(m.group());
			
			if (elm.find()) {
				String css = elm.group().substring(6, elm.group().length()-1);
				if (!IOHelper.isAbsolute(css)) {
					css = path + css;
				}
				//CSS-Parser über Stylesheets laufen lassen
				try {
					SimpleCSSParser.clearList(); //Liste löschen
					styles += parser.parse(css);
					//System.out.println("*******\n" + styles + "*******");
					logger.info("Hole CSS " + css + ".");
				} catch (TestelParserException e) {
					logger.warning("CSS " + css + " konnte nicht korrekt geparst werden -- ignoriere es deshalb.");
				} catch (IOException e) {}
			}
		}
		//System.out.println("Styles:\n" + styles);
		if (styles.equals("")) {
			logger.info("Keine CSS-Einträge gefunden - überspringe.");
			return htmldoc;
		}
		
		//CSS-Verweise löschen
		m.reset();
		htmldoc = m.replaceAll("");
		
		//Stylesheet einfügen
		p = Pattern.compile("<head.*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		m = p.matcher(htmldoc);
		m.find();
		StringBuffer buffer = new StringBuffer(htmldoc);
		buffer.insert(m.end(), "\n<style type=\"text/css\">\n/*<![CDATA[*/\n" + styles + "/*]]>*/\n</style>\n");
		//System.out.println(buffer.toString());
		
		logger.info("CSS-Einträge in " + handler.getURL().toString() + " wurden ins Dokument eingebunden.");
		return buffer.toString();
	}	
}

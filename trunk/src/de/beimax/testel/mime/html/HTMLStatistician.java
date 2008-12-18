/**
 * Datei: HTMLStatistician.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.logging.Level;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.mime.Statistician;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;
import de.beimax.testel.token.impl.MarkupToken;
import de.beimax.testel.token.impl.PunctuationToken;
import de.beimax.testel.token.impl.TestElTag;

/**HTML-Statistiker-Implementierung, holt sich diverse Daten aus der Statistik und
 * bewertet diese.
 * Der HTML-Statistiker vereinfacht auch die Fonts von Tags von speziellen Fonts auf
 * Font-Familien, damit die Statistik etwas besser behandelbar wird - diese fügt er im
 * speziellen Attribut "font-category"
 * Außerdem verbindet der Statistiker die einzelnen Tags miteinander (Referenzen).
 * @author mkalus
 *
 */
public class HTMLStatistician extends Statistician {
	/**
	 * Liste der Kriterien für die Statistik, d.h. diese Kriterien werden
	 * für die Statistik berücksichtigt
	 */
	public static final String[] STATCRITERIA = {
		"font-size", "font-category", "font-weight",
		"font-style", "text-decoration"
	};
//	protected static final String[] STATCRITERIA = {
//		"font-size", "font-category", "color", "font-weight",
//		"font-style", "text-decoration", "background-color"
//	};
	/**
	 * Indices für die Werte - werden von anderen Klassen wie den TextStyleTagger
	 * gebraucht.
	 */
	public static final int FONTSIZE = 0;
	public static final int FONTCATEGORY = 1;
	public static final int FONTWEIGHT = 2;
	public static final int FONTSTYLE = 3;
	public static final int FONTDECORATION = 4;
	
	/**
	 *Mapping
	 */
	protected HashMap<String, String> fontmap; //Mapping für Fonts auf Familien
	
	/**
	 * Wird für die Statistik gebraucht.
	 */
	private Stack<Token> markupstack;
	
	/**
	 * Grässliches Konstrukt: Statistik-Zähler als Array (s. STATCRITERIA für
	 * Liste, Map selbst nimmt pro Kriterium die einzelnen Elemente auf).
	 */
	protected LinkedList<HashMap<String, Integer>> statistics;
	
	/** Konstruktor
	 * @param handler
	 */
	public HTMLStatistician(TestElHandler handler) {
		super(handler);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Statistician#aggregateStatistics(de.beimax.testel.token.TokenList)
	 */
	@Override
	public void aggregateStatistics(TokenList list) throws TestelException {
		logger.info("HTML-Statistiker initialisiert und gestartet");
		
		//Initialisierung
		initStatistics();
		
		//Jetzt die TokenList abarbeiten
		Iterator<Token> it = list.iterator();
		
		Token tok = null;
		try {
			while (it.hasNext()) {
				tok = it.next();
				if (logger.getLevel() == Level.FINER || tok instanceof MarkupToken)
					logger.finer("Statistik für " + tok.toString());
				
				//Font vereinfachen
				changeFont(tok);
	
				//jetzt Statistiken sammeln
				callCounter(tok);
			}
		} catch (TestelException e) {
			throw new TestelException("Fehler beim Token " + tok + ":\n" + e.getLocalizedMessage());
		}
		
		//Ok, die Rohdaten sind jetzt aggregiert, jetzt Daten zusammenfassen
		condenseData();
		
		//Resourcen freigeben
		freeRessources();
	}
	
	/**
	 * Initialisierung des Statistikers
	 */
	protected void initStatistics() throws TestelException {
		//Font-Map erstellen
		createFontMap();

		//Neuen Statistik-Stack erzeugen
		markupstack = new Stack<Token>();
		
		//Statistik initialisieren
		statistics = new LinkedList<HashMap<String,Integer>>();
		for (int i = 0; i < STATCRITERIA.length; i++)
			statistics.add(new HashMap<String, Integer>());
	}
	
	/**
	 * Fasst die Rohdaten zusammen und speichert sie als statistische Daten
	 */
	protected void condenseData() {
		//Ok, Statistik ist jetzt fertig, nun müssen wir sie als Standart-Werte
		//in der Map speichern
		Iterator<HashMap<String, Integer>> it = statistics.iterator();
		for (int i = 0; i < STATCRITERIA.length; i++) {
			String max = getMaxEntryinMap(it.next());
			if (max == null || max.length() == 0) //Falls Werte aus irgendwelchen Gründen fehlen -> Standardwerte
				switch(i) {
				case FONTSIZE: max = "14"; break;
				case FONTCATEGORY: max = "serif"; break;
				case FONTWEIGHT: max = "normal"; break;
				case FONTSTYLE: max = "normal"; break;
				case FONTDECORATION: max = "none"; break;
				}
			//Maximum der Map in Statistik-Map speichern
			setStatistics(STATCRITERIA[i], max);
			
			logger.info("Statistik-Standardwert für " + STATCRITERIA[i] + "=" + max);
		}
	}
	
	/**
	 * nach der Aggregation können Ressourcen freigegeben werden
	 */
	protected void freeRessources() {
		fontmap = null;
		markupstack = null;
		statistics = null;
	}

	/**
	 * Erstellt die Font-Map für dieses Objekt
	 */
	protected void createFontMap() throws TestelException {
		File mapfile;
		try {
			mapfile = new File(handler.getMimeFactory().getMimeDir(), "fontmap.txt");
		} catch (IOException e) {
			logger.warning("Konnte Mime-Verzeichnis nicht erstellen");
			throw new TestelException("Konnte Mime-Verzeichnis nicht erstellen" + e.getLocalizedMessage());
		}
		
		//Testen...
		if (!mapfile.isFile()) throw new TestelException("Fontmap-Datei " + mapfile + " existiert nicht");
		if (!mapfile.canRead()) throw new TestelException("Fontmap-Datei " + mapfile + " kann nicht gelesen werden");
		
		//Ok, jetzt laden
		//neue Map erstellen
		fontmap = new HashMap<String, String>();
		
		//Abkürzungsdatei laden
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(mapfile));
		} catch (FileNotFoundException e) {
			throw new TestelException("Fontmap-Datei " + mapfile + " kann nicht gelesen werden:\n" + e.getLocalizedMessage());
		}
		
		String line;
		try {
			while ((line = reader.readLine()) != null)
				if (!line.trim().equals("") && line.trim().charAt(0) != '#') { //Kommentarzeilen
					String[] keyval = line.split("=");
					if (keyval.length != 2) throw new TestelException("Schlüsselwert " + line + " nicht korrekt!");
									
					fontmap.put(keyval[0].trim().toLowerCase(), keyval[1].trim().toLowerCase());
				}
		} catch (IOException e) {
			throw new TestelException("Ein-/Ausgabefehler beim Lesen von " + mapfile);
		}
		
		//System.out.println(fontmap.toString());
	}

	/**Holt den maximalen Wert aus einem Statistik-Eintrag 
	 * @param m
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private String getMaxEntryinMap(HashMap<String, Integer> m) {
		int max = 0;
		String maxkey = "";
		Iterator it = m.entrySet().iterator();
		
		while (it.hasNext()) {
			Entry<String, Integer> e = (Entry<String, Integer>) it.next();
			if (e.getValue() > max) {
				max = e.getValue();
				maxkey = e.getKey();
			}
		}
		
		return maxkey;
	}
	
	/**Behandelt das Attribut font-family. Dabei wird ein neues Attribut font-category
	 * zum Token hinzugefügt, der eine allgemeinere Familie der Font ausdrückt - die
	 * Liste wird in fontmap.txt gespeichert.
	 * Falls die Font nicht gefunden wird, wird die allgemeine Font-Kategorie
	 * "sans-serif" angenommen
	 * @param tok
	 */
	protected void changeFont(Token tok) {
		//nur Markup und keine Endtokens behandeln
		if (!(tok instanceof MarkupToken) || tok.getName().charAt(0) == '/') return; //nur Markup behandeln
		
		String font = tok.getAttributeValue("font-family");
		String fm = null;
		
		//falls Font-Attribut existiert und der Eintrag in der Font-Map eingetragen
		//ist, dann abstrakte font hinzufügen
		if (!(font == null || (fm = fontmap.get(font.toLowerCase())) == null))
			tok.addAttribute("font-category", fm);
		else tok.addAttribute("font-category", "sans-serif"); //Fall-Back
		
		//Überflüssige Attribut löschen
		tok.removeAttribute("font-family");
		
		//Logger evt. warnen lassen
		if (font != null && fm == null)
			logger.warning("Font " + font + " konnte durch den Fontmapper nicht gemappt werden - dies in der Datei fontmap.txt nachholen!");
	}
	
	/**Statistik für diesen Token aufnehmen...
	 * @param tok
	 */
	protected void callCounter(Token tok) throws TestelException {
		if (tok instanceof MarkupToken) { //Markup -> auf den Stack bzw. wieder 'runter
			if (tok.attributeExists("/")) { //Einzelmarkup wird ignoriert!
				tok.removeAttribute("/"); //Endkennzeichnung löschen, wird nicht mehr benötigt
				return;
			}
			//Endtag?
			if (tok.getName().charAt(0) == '/') {
				Token t;
				try {
					t = markupstack.pop();
				} catch (RuntimeException e) {
					throw new TestelException("Einige Tags scheinen nicht abgeschlossen zu sein!");
				}
				t.setReference(tok); //Zeiger auf Ende
				tok.setReference(t); //Ende-Zeiger auf Anfang
				if (logger.getLevel() == Level.FINEST)
					logger.finest("Tag <" + tok.getName() + "> beendet.");
			} else { //kein Endtag
				markupstack.push(tok); //Token auf Stack
				if (logger.getLevel() == Level.FINEST)
					logger.finest("Tag <" + tok.getName() + "> angefangen.");
			}
		} else { //kein Markuptoken -> hier zählen Statistiken!
			//Außnahmen: TestElTag
			if (tok instanceof TestElTag) return;
			//Kriterien durchlaufen
			Iterator<HashMap<String, Integer>> it = statistics.iterator();
			Token me = markupstack.peek(); //Oberstes Element im Stack ansehen
			tok.setReference(me); //Zeiger auf dieses Element
			//PunctuationToken nach der Referenz-Setzung ignorieren
			if (tok instanceof PunctuationToken) return;
			for (int i = 0; i < STATCRITERIA.length; i++) {
				HashMap<String, Integer> m = it.next();
				//TODO: Kaskadierung evt. übernehmen für background-color von
				//Blockelementen, etc.
				String val;
				if ((val = me.getAttributeValue(STATCRITERIA[i])) == null) continue;

				//schon im Stack?
				Integer count;
				if ((count = m.get(val)) == null) {
					m.put(val, 1); //erstes Element
					logger.finest(tok.getName() + ": " + STATCRITERIA[i] + "=" + val + " (1)");
				} else {
					m.put(val, count + 1);
					logger.finest(tok.getName() + ": " + STATCRITERIA[i] + "=" + val + " (" + (count + 1) + ")");
				}
			}
		}
	}
}

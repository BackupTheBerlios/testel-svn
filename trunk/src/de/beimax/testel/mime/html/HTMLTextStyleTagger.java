/**
 * Datei: HTMLTextStyleTagger.java
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

import java.util.Iterator;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.general.Tagger;
import de.beimax.testel.mime.Statistician;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;
import de.beimax.testel.token.impl.MarkupToken;

/**Die Hauptaufgabe des Style-Taggers ist die Vereinfachung von TextTags mit Hilfe der
 * Statistik. So werden Fonts nur noch als Font-Familien repräsentiert und die
 * Größen als big, normal, emphasis, special oder small im Attribut texttype abstrahiert.
 * @author mkalus
 *
 */
public class HTMLTextStyleTagger implements Tagger {
	/**
	 * Standardwerte der Statistik werden hier gespeichert 
	 */
	protected String[] stdValues;
	
	protected TestElHandler handler;

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Tagger#getType()
	 */
	public String getType() {
		return "HTML-TextStyle-Tagger";
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
		logger.info("HTML-Style-Tagger initialisiert");
		
		//Initialisieren
		initStyleTagger();
		
		//Die TokenList abarbeiten
		Iterator<Token> it = list.iterator();
		
		while (it.hasNext()) {
			Token tok = it.next();
			
			//Tag-Vereinfacher aufrufen
			simplifyTag(tok);
		}
		
		//Resourcen freigeben
		freeRessources();

		return list;
	}

	/**
	 * Initialisiert den Tagger
	 */
	protected void initStyleTagger() {
		//Statistik-Werte in Array, um diese schneller behandeln zu können
		stdValues = new String[HTMLStatistician.STATCRITERIA.length];
		
		//Statistiker holen
		Statistician statistician = handler.getStatistician();
		
		for (int i = 0; i < HTMLStatistician.STATCRITERIA.length; i++)
			stdValues[i] = statistician.getStringStatistics(HTMLStatistician.STATCRITERIA[i]);
	}

	/**
	 * nach der Abarbeitung können Ressourcen freigegeben werden
	 */
	protected void freeRessources() {
		stdValues = null;
	}
	
	/**Vereinfacht Tags aufgrund ihrer Zugehörigkeit - sind die Tags in der Gruppe des
	 * Standards, dann werden die Attribute gelöscht.
	 * Außerdem werden Mappings von Font-Größen vorgenommen und einige überflüssige
	 * Tag-Attribute gelöscht.
	 * @param tok
	 */
	protected void simplifyTag(Token tok) {
		//nur Markup und keine Endtokens behandeln
		if (!(tok instanceof MarkupToken) || tok.getName().charAt(0) == '/') return; //nur Markup behandeln
		
		String texttype = null;

		//die Abstraktion von Fonts wurde aus Performanz-Gründen schon vom Statistiker
		//vorgenommen.
		
//		big Alle Token, die eine Schriftgröße über der normalen besitzen. Die Wahrscheinlichkeit
//		  für Überschriften ist hier sehr hoch!
//		emphasis Fett oder kursiv gedruckte Schrifttypen, bzw. solche, die in einer Weise
//		  eine Hervorhebung (2.1.3) kennzeichnen.
//		special Schriften, die einen anderen Schrifttypus darstellen, also z. B. Codefragmente,
//		  u. ä.
//		small Token, die eine Schriftgröße unterhalb des normalen Schriftgrades haben.
//		normal Alle Texttoken, die keine besonderen Auszeichnungen oder Schriftgrößen besitzen.
		
		//zuerst auf Big-Schrifttyp prüfen
		String val = tok.getAttributeValue(HTMLStatistician.STATCRITERIA[HTMLStatistician.FONTSIZE]);
		double fontsize;
		double refsize = Double.valueOf(stdValues[HTMLStatistician.FONTSIZE]);
		if (val != null) {
			fontsize = Double.valueOf(val);
			if (fontsize > refsize) //größere Schrift
				texttype = "big";
		} else fontsize = refsize; //Fall-Back: bei keiner Annahme wird die Größe auf die normale Größe gesetzt
		
		//emphasis prüfen
		if (texttype == null) {
			String weight = tok.getAttributeValue(HTMLStatistician.STATCRITERIA[HTMLStatistician.FONTWEIGHT]);
			String style = tok.getAttributeValue(HTMLStatistician.STATCRITERIA[HTMLStatistician.FONTSTYLE]);
			String decoration = tok.getAttributeValue(HTMLStatistician.STATCRITERIA[HTMLStatistician.FONTDECORATION]);
			
			//leere Elemente abfangen
			if (weight == null) weight = "normal";
			if (style == null) style = "normal";
			if (decoration == null) decoration = "none";
			
			if (!weight.equals(stdValues[HTMLStatistician.FONTWEIGHT]) ||
					!style.equals(stdValues[HTMLStatistician.FONTSTYLE]) ||
					!decoration.equals(stdValues[HTMLStatistician.FONTDECORATION]))
				texttype = "emphasis";
		}
		
		//special prüfen
		if (texttype == null) {
			String fontcat = tok.getAttributeValue(HTMLStatistician.STATCRITERIA[HTMLStatistician.FONTCATEGORY]);
			if (fontcat != null && !fontcat.equals(stdValues[HTMLStatistician.FONTCATEGORY]))
				texttype = "f_" + fontcat;
		}

		//small prüfen
		if (texttype == null && fontsize < refsize)
			texttype = "small";
		
		if (texttype == null)
			texttype = "normal";
		
		//Kriterien durchlaufen und Werte löschen
		for (int i = 0; i < stdValues.length; i++)
			//if (i != HTMLStatistician.FONTCATEGORY) //font-category bestehen lassen
				tok.removeAttribute(HTMLStatistician.STATCRITERIA[i]);
		
		//Texttyp hinzufügen
		tok.addAttribute("texttype", texttype);
		
		//System.out.println(tok);
	}

}

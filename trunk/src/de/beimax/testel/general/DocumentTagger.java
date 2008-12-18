/**
 * Datei: DocumentTagger.java
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Logger;

import de.beimax.testel.config.Config;
import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.token.TextPosition;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;
import de.beimax.testel.token.impl.NumberToken;
import de.beimax.testel.token.impl.PunctuationToken;
import de.beimax.testel.token.impl.SomeToken;
import de.beimax.testel.token.impl.TestElTag;
import de.beimax.testel.token.impl.TextToken;
import de.beimax.testel.util.IOHelper;

/**Instanzen dieser Klasse erstellen entweder eine Taglist zu dem Dokument oder taggen
 * das Dokument selbst.
 * Der DocumentTagger nimmt dazu das Dokument und die Tokenliste, welche die schon
 * fertigen Tags anthalten muss
 * @author mkalus
 *
 */
public class DocumentTagger {
	//Logger
	static final Logger logger = Logger.getLogger(DocumentTagger.class.getName());
	
	/** Konstruktor
	 */
	public DocumentTagger() {
		logger.info("Dokument-Tagger geladen");
	}
	
	/**Gibt ein Dokument mit enthaltenden Tags zurück
	 * @param document
	 * @param list
	 * @param allTags PreTrain-Modus-Ausgabe
	 * @param fewertags true, falls --fewer-tags per Kommandozeile übergeben wurde
	 * @return
	 */
	public String createInlineDocument(String document, TokenList tokenList, boolean allTags, boolean fewertags) throws TestelTaggerException {
		//neuen String für Dokument erzeufen
		StringBuffer doc = new StringBuffer(document);

		//Token-Liste rückwärts durchlaufen
		Iterator<Token> it = tokenList.descendingIterator();
		
		while (it.hasNext()) {
			Token tok = it.next();
			if (tok instanceof TestElTag) {
				int type = TestElTag.getTestElTagType(tok);
				TextPosition pos = tok.getTextPosition();
				// Tags, die schon im Markup enthalten sind überspringen
				if (pos.getBpos() != pos.getEpos())
					continue;
				String name = tok.getName();
				//jetzt nach Typ unterscheiden
				String tag = null;
				if (allTags) { //alle Taggen
					if (type != TestElTag.TAG_TAG) {
						if (name.charAt(0) == '/') { //Endtag oder Starttag?
							tag = createEndTag(tok, type, name);
						} else tag = createStartTag(tok, type, name);
					}
				} else { //normal Taggen
					if (type == TestElTag.TAG_TAG || type == TestElTag.TAG_REF) {
						if (name.charAt(0) == '/') { //Endtag oder Starttag?
							tag = createEndTag(tok, type, name);
						} else tag = createStartTag(tok, type, name);
					}
				}
				//Tag in Puffer einfügen (falls nötig) - an der richtigen Stelle
				if (tag != null) doc.insert(pos.getBpos(), tag);
			} else if (allTags && !fewertags) { //weitere Tags checken
				String startTag = null, stopTag = null;
				if (tok instanceof NumberToken) { //Zahlwort?
					try {
						Integer.parseInt(tok.getName());
					} catch (NumberFormatException e) { //ist eines!
						startTag = createStartTag(tok, TestElTag.TAG_NUMBER, "number");
						stopTag = createEndTag(null, TestElTag.TAG_NUMBER, "/number");
					}
				} else if (tok instanceof PunctuationToken) { //Satzzeichen?
					startTag = createStartTag(tok, TestElTag.TAG_PUNCT, "punct");
					stopTag = createEndTag(null, TestElTag.TAG_PUNCT, "/punct");
				} else if (tok instanceof TextToken) { //bedeutungstragendes Wort?
					startTag = createStartTag(tok, TestElTag.TAG_WORD, "word");
					stopTag = createEndTag(null, TestElTag.TAG_WORD, "/word");
				} else if (tok instanceof SomeToken) {
					String name = tok.getName();
					if (name.charAt(name.length()-1) == '.') {
						startTag = createStartTag(tok, TestElTag.TAG_ABBREV, "abbrev");
						stopTag = createEndTag(null, TestElTag.TAG_ABBREV, "/abbrev");
					}
				}
				if (startTag != null && stopTag != null) {
					TextPosition pos = tok.getTextPosition();
					doc.insert(pos.getEpos(), stopTag);
					doc.insert(pos.getBpos(), startTag);
				}
			}
		}
		//System.exit(0);
		
		return doc.toString();
	}
	
	/**Gibt ein StartTag zurück
	 * @param token
	 * @param type
	 * @param name
	 * @return
	 */
	private String createStartTag(Token token, int type, String name) {
		StringBuffer tag = new StringBuffer();
		tag.append("<testel:");
		tag.append(name);
		if (type != TestElTag.TAG_ABBREV) {
			tag.append(" class=\"");
			if (type == TestElTag.TAG_NUMBER)
				tag.append(token.getAttributeValue("_TOK:number"));
			else tag.append(token.getClassName());
			tag.append('"');
		}
		if (type == TestElTag.TAG_MATCH) {
			String start = token.getAttributeValue("start");
			String stop = token.getAttributeValue("stop");
			String outer = token.getAttributeValue("outer");
			String exclstart = token.getAttributeValue("exclstart");
			String exclstop = token.getAttributeValue("exclstop");
			String donotshow = token.getAttributeValue("donotshow");
			String greedy = token.getAttributeValue("greedy");
			String nonesting = token.getAttributeValue("nonesting");
			if (start != null) tag.append(" start=\"" + start + "\"");
			if (stop != null) tag.append(" stop=\"" + stop + "\"");
			if (outer != null) tag.append(" outer=\"" + outer + "\"");
			if (exclstart != null) tag.append(" exclstart=\"" + exclstart + "\"");
			if (exclstop != null) tag.append(" exclstop=\"" + exclstop + "\"");
			if (donotshow != null) tag.append(" donotshow=\"" + donotshow + "\"");
			if (greedy != null) tag.append(" greedy=\"" + greedy + "\"");
			if (nonesting != null) tag.append(" nonesting=\"" + nonesting + "\"");
		} else {
			String donotshow = token.getAttributeValue("donotshow");
			if (donotshow != null) //tag.append(" donotshow=\"" + donotshow + "\"");
				return null; //nicht zeigen
		}
		tag.append('>');
		
		return tag.toString();
	}
	
	/**Gibt ein Endtag zurück
	 * @param token darf null sein
	 * @param type
	 * @param name
	 * @return
	 */
	private String  createEndTag(Token token, int type, String name) {
		//Referenz checken, ob do not-show gilt
		if (token != null) {
			Token ref = token.getReference();
			if (ref != null) { //hier evt. auch das Endtag nicht zeigen
				String donotshow = ref.getAttributeValue("donotshow");
				if (donotshow != null && donotshow.equalsIgnoreCase("yes")) return null;
			}
		}
		
		return "</testel:" + name.substring(1) + ">";
	}
	
	/**Gibt ein Dokument zurück, in dem die Beschreibungen
	 * enthalten sind
	 * @param document
	 * @param list
	 * @param origRef Ort, an dem die Datei ursprünglich lag
	 * @param fileRef Ort, an dem die Datei letztlich liegen wird
	 * @param xmlFileRef Ort, an dem die XML-Datei letztlich liegen wird
	 * @return
	 */
	public String createXMLDescription(String document, TokenList tokenList, URL origRef, File fileRef) throws TestelTaggerException {
		//neuen XML-String erzeugen
		StringBuffer xml = new StringBuffer();

		//Header-Zeug vom String
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
		//DTD einfügen
		String dtd = Config.getConfig("dtdforxml");
		if (dtd != null)
			try {
				URL dtdURL = IOHelper.createURL(dtd);
				if (IOHelper.isFile(dtdURL)) dtd = (new File(dtd)).getAbsolutePath();
			} catch (MalformedURLException e1) {
				logger.warning("Keine DTD angegeben (in den properties der Schlüssel dtdforxml)");
			}
		if (dtd != null)
			xml.append("<!DOCTYPE testel:externaldesc SYSTEM \"" + dtd + "\">\n");
		xml.append("<testel:externaldesc origURL=\"" + origRef.toString() + "\" saveURL=\"file:" + fileRef.getAbsoluteFile() + "\" >\n");

		//Token-Liste durchlaufen
		Iterator<Token> it = tokenList.iterator();
		
		while (it.hasNext()) {
			Token tok = it.next();
			 if (tok instanceof TestElTag) {
				 int type = TestElTag.getTestElTagType(tok);
				 //nur Tags und Refs und keine Endtags
				 if ((type == TestElTag.TAG_TAG || type == TestElTag.TAG_REF) &&
						 tok.getName().charAt(0) != '/') {
					TextPosition bpos = tok.getTextPosition();
					if (bpos == null) throw new TestelTaggerException("Startposition von " + tok + " war nicht definiert");
					Token ref = tok.getReference();
					if (ref == null) throw new TestelTaggerException("Im Token " + tok + " fehlte die Referenz auf das Endtoken");
					TextPosition epos = ref.getTextPosition();
					if (epos == null) throw new TestelTaggerException("Endposition von " + tok + " war nicht definiert");
					xml.append("  <testel:");
					xml.append(tok.getName());
					xml.append(" class=\"");
					xml.append(tok.getClassName());
					xml.append("\" bpos=\"");
					xml.append(bpos.getEpos()); //Ende, da hier der eigentliche Inhalt anfängt
					xml.append("\" brow=\"");
					xml.append(bpos.getErow()); //Ende, da hier der eigentliche Inhalt anfängt
					xml.append("\" bcol=\"");
					xml.append(bpos.getEcol()); //Ende, da hier der eigentliche Inhalt anfängt
					xml.append("\" epos=\"");
					xml.append(epos.getBpos()); //Anfang, da hier der eigentliche Inhalt aufhört
					xml.append("\" erow=\"");
					xml.append(epos.getBrow()); //Anfang, da hier der eigentliche Inhalt aufhört
					xml.append("\" ecol=\"");
					xml.append(epos.getBcol()); //Anfang, da hier der eigentliche Inhalt aufhört
					
					xml.append("\" />\n");
				 }
			 }
		}
		
		xml.append("</testel:externaldesc>\n");
		
		return xml.toString();
	}
}

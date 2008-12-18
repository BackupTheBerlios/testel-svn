/**
 * Datei: HTMLParser.java
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

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Level;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.exception.TestelParserException;
import de.beimax.testel.mime.Parser;
import de.beimax.testel.mime.html.util.EntityTransformer;
import de.beimax.testel.mime.html.util.HTMLTokenizer;
import de.beimax.testel.token.TextPosition;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;
import de.beimax.testel.token.impl.ImageToken;
import de.beimax.testel.token.impl.SomeToken;
import de.beimax.testel.token.impl.MarkupToken;
import de.beimax.testel.token.impl.TestElTag;

/**
 * @author mkalus
 *
 */
public class HTMLParser implements Parser {
	TestElHandler handler;
	/**Enthält einen Stack von TestEl-Tag-Varieties
	 * Stack<String> testElTagVarietyStack 
	 */
	Stack<String> testElTagVarietyStack;
	
	public HTMLParser(TestElHandler handler) {
		setHandler(handler);
		testElTagVarietyStack = new Stack<String>();
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Parser#setHandler(de.beimax.testel.TestElHandler)
	 */
	public void setHandler(TestElHandler handler) {
		this.handler = handler;
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Parser#parse(java.lang.String)
	 */
	public TokenList parse(String document) throws TestelParserException {
		//ersteinmal tokenize laufen lassen
		TokenList back;
		try {
			back = tokenize(document);
			if (back == null) return null; //falls leer
		} catch (IOException e) {
			logger.severe("Fehler im Tokenizer: " + e.getLocalizedMessage());
			throw new TestelParserException("Fehler im Tokenizer: " + e.getLocalizedMessage());
		}
		return back;
	}

	/**Aufruf des Tokenizers für HTML-Dokumente
	 * @param document
	 * @return
	 * @throws IOException
	 * @throws TestelParserException
	 */
	private TokenList tokenize(String document) throws IOException, TestelParserException {
		HTMLTokenizer tokenizer = new HTMLTokenizer(new StringReader(document));
		
		//Referenz-Stacks für TestElTags
		Stack<Token> testelTagStack = new Stack<Token>();
		
		//neue Liste erstellen
		TokenList list = new TokenList();
		
		boolean start = false;

		int type;
		while ((type = tokenizer.nextToken()) != -1) {
			String token = tokenizer.getToken();
			if (!start && type == HTMLTokenizer.TT_TAG && token.length() >= 4
					&& token.substring(0, 4).equals("body")) {
				start = true;
				//continue; //Bearbeitung ab <body>
			}
			if (start) {
				Token tok = null;
				int bpos = tokenizer.getBeginningPos();
				int brow = tokenizer.getBeginningRow();
				int bcol = tokenizer.getBeginningCol();
				int epos = tokenizer.getEndPos();
				int erow = tokenizer.getEndRow();
				int ecol = tokenizer.getEndCol();
				TextPosition pos = new TextPosition(bpos, brow, bcol, epos, erow, ecol);
				//System.out.println(pos.toString() + ":" + token);
				switch (type) {
				case HTMLTokenizer.TT_TAG:
					//Erstellen eines TagParsers (p. Klasse, s.u.)
					try {
						TagParser tagParser = new TagParser(token);
						//Art des Tags?
						if (tagParser.isTestelTag()) { //TestEl-Tag
							String tname = tagParser.getTestElTagName();
							logger.finest("TestEl-Tag erkannt: " + tname);
							tok = new TestElTag(tname);
							Entry<String, String> att; //Attribute hinzufügen
							boolean hadClass = false; 
							while ((att = tagParser.nextAttribute()) != null) {
								if (att.getKey().equals("class")) { //Typ -> auf Variety mappen
									String val = att.getValue().toUpperCase();
									tok.setClassName(val);
									testElTagVarietyStack.push(val);
									hadClass = true;
								}
								else tok.addAttribute(att.getKey(), att.getValue());
							}
							//Wurde Klasse als Attribut angegeben?
							if (!hadClass) {
								//Endtag, erhält letztes Tag
								if (tname.charAt(0) == '/')
									tok.setClassName(testElTagVarietyStack.pop());
								else throw new TestelParserException("TestEl-Tag " + tok + " an Postion " + pos + " enthält kein Attribut class");
							}
							//jetzt Anfangs und Endtoken checken und Referenzen setzen
							if (tname.charAt(0) == '/') {
								try {
									Token startTag = testelTagStack.pop();
									String startName = startTag.getName();
									if (!startName.equals(tname.substring(1)))
										throw new TestelParserException("TestEl-Tags passen nicht zusammen: " + startTag + " und " + tname);
									//gut, alles klar, dann Refrenzen bauen
									startTag.setReference(tok);
									tok.setReference(startTag);
								} catch (Exception e) {
									throw new TestelParserException("Fehler beim Parsen von TestEl-Tags:\n" + e.getLocalizedMessage());
								}
							} else //Anfangstags auf den Stack
								testelTagStack.add(tok);
						} else { //Kein TestEl-Tag -> normales HTML-Tag
							logger.finest("Tag erkannt: " + tagParser.getTagName());
							Entry<String, String> att; //Attribute hinzufügen
							//Bild?
							if (tagParser.getTagName().equalsIgnoreCase("img")) {
								tok = new ImageToken();
								String url = null;
								while ((att = tagParser.nextAttribute()) != null)
									if (att.getKey().equals("src")) url = att.getValue(); //src wird als Name genommen
									else tok.addAttribute(att.getKey(), att.getValue()); //sonst ok
								if (url != null) tok.setName(url); //ansonsten NN
							} else { //alle anderen
								tok = new MarkupToken(tagParser.getTagName());
								while ((att = tagParser.nextAttribute()) != null)
									tok.addAttribute(att.getKey(), att.getValue());
							}
						}
					} catch (TestelParserException e) {
						String msg = "Tag " + token + " konnte nicht geparst werden an Position " + pos.toString() + "\n" + e.getLocalizedMessage();
						logger.warning(msg);
						throw new TestelParserException(msg);
					}
					break;
				case HTMLTokenizer.TT_ENTITY:
					String entity = EntityTransformer.decode("&" + token + ";");
					if (entity == null || entity.length() == 0) {
						logger.warning("Konnte Entität &" + token + "; nicht auflösen - verwende stattdessen ENT_UNKNOWN");
						//throw new TestelParserException("Konnte Entität &" + token + "; nicht auflösen");
						entity = "ENT_UNKNOWN";
					}
					tok = new SomeToken(entity);
					if (possiblyJoin(list, tok, pos)) tok = null;
					break;
				case HTMLTokenizer.TT_TEXT:
					logger.finest("Text erkannt: " + token);
					tok = new SomeToken(token);
					if (possiblyJoin(list, tok, pos)) tok = null;
					break;
				default: logger.warning("Unbekanntes Token: " + tokenizer.toString());
				}
				//Token vervollständigen und an Liste anhängen
				if (tok != null) {
					tok.initTextPosition(pos);
					if (logger.getLevel() == Level.FINE)
						logger.fine("Token erzeugt und angehängt: " + tok.toString());
					list.add(tok);
				}
				if (type == HTMLTokenizer.TT_TAG && token.equals("/body"))
					break; //Bearbeitung mit </body> einstellen
			}
		}

		return list;
	}
	
	/**Hängt evt. Token zusammenhängen, wenn sie sometokens sind und hintereinander kommen.
	 * Das hängt Token zusammen, in denen z.B. Entities vorkommen.
	 * @param list
	 * @param token
	 * @param pos
	 */
	private boolean possiblyJoin(TokenList list, Token token, TextPosition pos) {
		if (list.size() == 0) return false; //erstes Element... weg
		Token last = list.getLast(); //letztes Element der Liste holen
		
		//sind die Tokens SomeTokens?
		if (!(last instanceof SomeToken && token instanceof SomeToken)) return false;
		
		//letztes oder dieses Token sind Entities
		if (last.getName().length() == 1 && last.getTextPosition().getBpos() - last.getTextPosition().getEcol() > 1) return false;
		if (token.getName().length() == 1 && pos.getBpos() - pos.getEcol() > 1) return false;
		
		//hängen die beiden Positionen zusammen?
		if (last.getTextPosition().getEpos() != pos.getBpos()) return false;
		
		//falls ok, dann Name & Positionen anpassen
		last.setName(last.getName() + token.getName());
		last.getTextPosition().setEpos(pos.getEpos());
		last.getTextPosition().setErow(pos.getErow());
		last.getTextPosition().setEcol(pos.getEcol());
		
		return true;
	}
	
	/**Private Klasse zum Auswerten von Tags
	 * @author mkalus
	 *
	 */
	protected class TagParser {
		private TreeMap<String, String> attributes;
		private String tagName;
		
		/** Konstruktor
		 * @param tag
		 * @throws TestelParserException
		 */
		public TagParser(String tag) throws TestelParserException {
			attributes = new TreeMap<String, String>();
			parseTag(tag);
		}
		
		/**Getter für tagName
		 * @return
		 */
		public String getTagName() {
			return tagName;
		}
		
		/**Getter für tagName - ohne testel:
		 * @return
		 */
		public String getTestElTagName() {
			if (tagName.charAt(0) == '/')
				return '/' + tagName.substring(8);
			else
				return tagName.substring(7);
		}
		
		/**Gibt true zurück, falls Tag-Name mit testel: beginnt
		 * @return
		 */
		public boolean isTestelTag() {
			if (tagName.length() <= 7) return false;
			if (tagName.substring(0, 7).equalsIgnoreCase("testel:")) return true;
			if (tagName.length() <= 8) return false;
			if (tagName.substring(0, 8).equalsIgnoreCase("/testel:")) return true;
			return false;
		}
		
		/**Gibt true zurück, falls Tag-Name mit testel: beginnt
		 * @return
		 */
		public boolean isTestelEndTag() {
			if (tagName.length() <= 8) return false;
			if (tagName.substring(0, 8).equalsIgnoreCase("/testel:")) return true;
			return false;
		}
		
		/**Holt sich Stück für Stück die Attribute aus der Liste
		 * @return AttributeSimple oder null, falls am Ende
		 */
		public Entry<String, String> nextAttribute() {
			if (attributes.size() == 0) return null; //leer

			//Neues Attribute erstellen und zurückgeben
			Entry<String, String> entry = attributes.firstEntry();
			attributes.remove(entry.getKey());
			return entry;
		}
		
		/**Parst einen Tag nach Attributen
		 * @param tag
		 * @throws TestelParserException
		 */
		private void parseTag(String tag) throws TestelParserException {
			//Tokens hier bilden
			StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(tag));
			tokenizer.wordChars(':', ':');
			tokenizer.wordChars('/', '/');
			tokenizer.quoteChar('"');

			try {
				tokenizer.nextToken(); //Tagname
				if (tokenizer.ttype != StreamTokenizer.TT_WORD)
					throw new TestelParserException("Tagname " + tokenizer.toString() + " im Tag " + tag + " ist nicht lesbar.\n");
				tagName = tokenizer.sval;
				
				//rest durchlaufen
				while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
					//zuerst key
					if (tokenizer.ttype != StreamTokenizer.TT_WORD)
						throw new TestelParserException("Attribut " + tokenizer.toString() + " im Tag " + tag + " ist nicht lesbar.\n");
					String key = tokenizer.sval;
					
					tokenizer.nextToken();
					String value;
					if (tokenizer.ttype != '=') { //einzelnes Tag, z.B.
						tokenizer.pushBack();
						value = "true"; //dummy-Wert
					} else {
						
						//ok = bestätigt, dann Wert auslesen
						tokenizer.nextToken();
						if (tokenizer.ttype != '"') { //Anführungszeichen am Anfang
							logger.warning("Attribute im Token " + tag + " sind nicht von Anführungszeichen umgeben!");
						}
						
						value = tokenizer.sval;
					}
					logger.finest("Neues Attribut: " + key + "=" + value);
					if (key.equalsIgnoreCase("style"))
						addStylesToMarkup(value); //Styles extra behandeln
					else attributes.put(key, value);
				}
			} catch (IOException e) {
				throw new TestelParserException("Tag " + tag + " konnte nicht gelesen werden.\n" + e.getLocalizedMessage());
			}
			
			//Testel:abbrev-Token? Klasse hier hinzufügen
			if (tagName.equalsIgnoreCase("testel:abbrev"))
				attributes.put("class", "ABBREV");

			//Einzeltag? Attribut / anfügen
			if (tag.charAt(tag.length()-1) == '/') attributes.put("/", "yes");
		}
		
		/**Eine Style-Liste in eine Attributsliste umwandeln
		 * @param stylelist
		 */
		private void addStylesToMarkup(String stylelist) throws TestelParserException {
			String[] keyvallist = stylelist.split(";"); //nach ; aufsplitten
			for(int i = 0; i < keyvallist.length; i++) { //und durchlaufen
				String[] detail = keyvallist[i].split(":"); //noch einmal nach : splitten
				if (detail.length != 2) throw new TestelParserException("Style " + keyvallist[i] + " wurde in der Liste " + stylelist + " nicht verstanden.");
				String key = detail[0].trim();
				String val = detail[1].trim();
				attributes.put(key, val); //zu den Attributen hinzufügen
				logger.finest("Neues Style-Attribut: " + key + "=" + val);
			}
		}
	}
}

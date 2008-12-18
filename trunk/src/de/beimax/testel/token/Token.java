/**
 * Datei: Token.java
 * Paket: de.beimax.testel.token
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
package de.beimax.testel.token;

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.logging.Logger;

import de.beimax.testel.TestElHandler;

/**Basisklasse für Tokens. Die Klasse besitzt drei Attribute, Typ, Name und Sorte (Variety, fakultativ).
 * Das Token wird nur beachtet, wenn <code>isMeaningful</code> auf True gesetzt wurde. Dann wird der Typ
 * überprüft und evt. noch der Name (falls <code>isNameImportant</code> true) und die Sorte (falls
 * <code>isVarietyImportant</code> true).
 * @author mkalus
 *
 */
public class Token {
	protected static Logger logger =  Logger.getLogger(Token.class.getName());
	
	protected static TestElHandler handler;
	
	/**Kopiert die AttributListe von einem Token zum anderen
	 * @param from
	 * @param to
	 */
	protected static void copyAttribs(Token from, Token to) {
		to.name = new String(from.name);
		if (from.className != null) to.className = new String(from.className);
		else to.className = null;

		//Attribute kopieren
		if (from.attributeSet != null)
			for (Entry<String, String> entry : from.attributeSet.entrySet())
				to.addAttribute(new String(entry.getKey()), new String(entry.getValue()));
	}
	
	/**Setzt Handler - wird von TestElHandler beim Starten aufgerufen
	 * @param handler
	 */
	public static void setHandler(TestElHandler handler) {
		Token.handler = handler;
	}
	
	/**
	 * Enthält die Attribute des Tokens
	 */
	private Hashtable<String, String> attributeSet;
	private String name, className;
	
	/**
	 * Enthält die Text-Position eines Tokens (oder null - muss initialisiert werden!)
	 */
	protected TextPosition pos = null;
	
	/**
	 * Referenz-Token (oder null - muss gesetzt werden!) 
	 */
	protected Token reference = null;

	/**Konstruktor mit Typ und Name (können null sein)
	 * @param type
	 * @param name
	 * @param pos
	 */
	protected Token(String name) {
		setClassName(null);
		setName(name);
	}
	
	/**Konstruktor mit Typ, Variety und Name (können null sein)
	 * @param type
	 * @param name
	 * @param pos
	 */
	protected Token(String name, String className) {
		setClassName(className);
		setName(name);
	}

	/**
	 * @return isTextToken
	 */
	public boolean isTextToken() {
		return false;
	}

	/**
	 * @return isTypeImportant
	 */
	public boolean isClassNameSet() {
		return (className != null);
	}

	/**Gibt Namen des Tokens oder null zurück
	 * @return
	 */
	public String getName() {
		return this.name;
	}
	
	/**Gibt Typ des Tokens oder null zurück
	 * @return
	 */
	public String getType() {
		return "TOKEN";
	}
	
	/**Gibt Sorte des Tokens oder null zurück
	 * @return
	 */
	public String getClassName() {
		return this.className;
	}
	
	/**Setzt einen neuen Namen (null = Name wird entfernt)
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**Setzt einen neuen Typen (null = Typ wird entfernt)
	 * @param type
	 */
	public void setClassName(String className) {
		this.className = className;
	}

	/**True, falls tok und this mit dem selben Namen
	 * @param tok
	 * @return
	 */
	public boolean sameName(Token tok) {
		return this.name.equals(tok.getName());
	}

	/**True, falls tok und this vom selben Typ
	 * @param tok
	 * @return
	 */
	public boolean sameType(Token tok) {
		return this.getType().equals(tok.getType());
	}
	
	/**True, falls tok und this mit dem selben Klassennamen
	 * @param tok
	 * @return
	 */
	public boolean sameClassName(Token tok) {
		if (className == null && tok.getClassName() == null) return true;
		return this.className.equals(tok.getClassName());
	}
	
	/**Fügt ein Attribut zum Token dazu
	 * @param key
	 * @param val
	 */
	public void addAttribute(String key, String val) {
		if (attributeSet == null) attributeSet = new Hashtable<String, String>();
		attributeSet.put(key, val);
	}
	
	/**Ändert das Attribut, falls es existiert (sonst nicht)
	 * @param key
	 * @param val
	 */
	public void changeAttribute(String key, String val) {
		if (attributeExists(key)) addAttribute(key, val);
	}
	
	/**Gibt true zurück, wenn Attribut im Token existiert
	 * @param key
	 * @return
	 */
	public boolean attributeExists(String key) {
		if (attributeSet == null) return false;
		return attributeSet.containsKey(key);
	}
	
	/**Gibt den Wert eines Attributs zurück oder null, falls nicht gefunden
	 * @param key
	 * @return
	 */
	public String getAttributeValue(String key) {
		if (attributeSet == null) return null;
		return attributeSet.get(key);
	}
	
	/**Entfernt ein Attribut aus der Attributmenge
	 * @param key
	 */
	public void removeAttribute(String key) {
		if (attributeSet != null) {
			attributeSet.remove(key);
		}
	}
	
	/**Vergleichsmethode - sind die Tokens inhaltlich gleich?
	 * @param tok
	 * @return
	 */
	public double compare(Token tok) {
		if (this.getClass() != tok.getClass()) return 0;

		//Namen und Typ prüfen
		if (!sameType(tok)) return 0;
		if (!sameClassName(tok)) return 0;
		if (!sameName(tok)) return 0;

		//ok, jetzt der Rest, das sind die Attribute
		return compareAttributes(tok.attributeSet);
	}

	/**Vergleicht zwei AttributMengen miteinander. Der Vergleich hier ist nich kommutativ, da die
	 * Größen sich unterscheiden können. <code>as</code> ist das ReferenzSet und wird in der Regel das größere
	 * der beiden (im Markov-Konstrukt) darstellen, während <code>this</code> das kleinere (in der Liste mit
	 * AttributeSimple-Objekten) sein wird.
	 * @param as
	 * @return
	 */
	protected double compareAttributes(Hashtable<String, String> compareSet) {
		double hits = 0;
		if (attributeSet.size() == 0) {
			if (compareSet.size() == 0) return 1;
			else return 0;
		}
		
		for (Entry<String, String> entry : attributeSet.entrySet()) {
			String myValue = entry.getValue();
			String theirValue = compareSet.get(entry.getKey());
			if (myValue.equals(theirValue)) hits++;
		}
		
		return hits / attributeSet.size();
	}

	/** Getter für reference
	 * @return reference
	 */
	public Token getReference() {
		return reference;
	}

	/** Setter für reference
	 * @param reference Festzulegender reference
	 */
	public void setReference(Token reference) {
		this.reference = reference;
	}

	/**Erzeugt einen Klon des Tokens
	 * @return
	 */
	public Token copy() {
		Token newtok = new Token("", "");
		
		copyAttribs(this, newtok);
		
		//Position und Referenz übergeben
		newtok.initTextPosition(pos);
		newtok.reference = getReference();

		return newtok;
	}
	
	/* (Kein Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getType() + classNameToString() + ":" + nameToString() + attribsToString() + posToString() + referenceToString();
	}
	
	/**Gibt den Namen des Tokens in [] aus.
	 * @return
	 */
	protected String nameToString() {
		return getName();
	}
	
	/**Gibt Attribute zurück: zum Überschreiben.
	 * @return
	 */
	protected String attribsToString() {
		if (attributeSet == null) return "";
		
		StringBuffer buffer = new StringBuffer();
		
		for (Entry<String, String> entry : attributeSet.entrySet()) {
			if (buffer.length() != 0) buffer.append(';');
			buffer.append(entry.getKey());
			buffer.append('=');
			buffer.append(entry.getValue());
		}
		
		if (buffer.length() == 0) return "";
		return "{" + buffer.toString() + "}";
	}
	
	/**Gibt Attribute zurück: zum Überschreiben.
	 * @return
	 */
	protected String classNameToString() {
		if (isClassNameSet()) return "/" + getClassName();
		return "";
	}
	
	/**Gibt Position zurück: zum Überschreiben.
	 * @return
	 */
	protected String posToString() {
		if (pos != null) return pos.toString();
		return "";
	}
	
	/**Gibt Referenz zurück: zum Überschreiben.
	 * @return
	 */
	protected String referenceToString() {
		if (reference != null) return "[Ref=" + reference.getName() + "]";
		return "";
	}
	
	/**Initialisiere Text-Position für Token
	 * @param bpos
	 * @param brow
	 * @param bcol
	 * @param epos
	 * @param erow
	 * @param ecol
	 */
	public void initTextPosition(int bpos, int brow, int bcol, int epos, int erow, int ecol) {
		pos = new TextPosition(bpos, brow, bcol, epos, erow, ecol);
	}
	
	/**Initialisiere Text-Position für Token
	 * @param position
	 */
	public void initTextPosition(TextPosition position) {
		if (position != null)
		initTextPosition(position.getBpos(), position.getBrow(), position.getBcol(),
				position.getEpos(), position.getErow(), position.getEcol());
	}
	
	/**
	 * Deinitialisiert Text-Position
	 */
	public void forgetTextPosition() {
		pos = null;
	}
	
	/**Gibt Text-Position des Tokens zurück (oder null, falls nicht initialisiert)
	 * @return
	 */
	public TextPosition getTextPosition() {
		return pos;
	}
	
	/**Gibt eine neue TextPosition zurück, die anhand des Index berechnet wird,
	 * der Übergeben wurde. Praktisch bei mehrzeiligen Tokens...
	 * @param index
	 * @return
	 */
	public TextPosition posAtIndex(int index) {
		String name = getName();
		TextPosition back = new TextPosition(pos.getBpos() + index,pos.getBrow(), pos.getBcol(), pos.getEpos(), pos.getBrow(), pos.getEcol());

		for (int i = 0; i < index; i++) {
			if (name.charAt(i) == 13) {
				back.setBcol(0); //Zurücksetzen
				back.setBrow(back.getBrow()+1); //Inkrement Zeile
			} else back.setBcol(back.getBcol()+1); //Inkrement Spalte
		}
		
		return back;
	}
	
	/**Teilt das Token in zwei neue Elemente. Das ausführende Element wird
	 * entsprechend gekürzt, das zurückgegebene ist das neue Element hinter dem
	 * gekürzten Token.
	 * @param index Stelle, an der die Teilung erfolgen soll.
	 * @return
	 */
	public Token split(int index) {
		//Kopie anfertigen
		Token newtok = copy();

		//neue Position
		TextPosition newpos = posAtIndex(index);
		String name = getName();
		//alte Position ändern
		pos.setEpos(newpos.getBpos());
		pos.setErow(newpos.getBrow());
		pos.setEcol(newpos.getBcol());
		//alten Token kürzen
		setName(name.substring(0, index));
		
		//neues Token anpassen
		newtok.setName(name.substring(index));
		newtok.forgetTextPosition();
		newtok.initTextPosition(newpos);
		
		return newtok;
	}
	
	/**Fügt den Namen und die Position des übergebenen Tokens einfach zu
	 * diesem hinzu - keine Typänderung!
	 * Um richtig zu funktionieren, muss das übergebene Element direkt nach diesem
	 * kommen - sonst kommen eigenartige Werte heraus!
	 * @param toJoin
	 * @param withSpace falls true, dann wird zwischen den Namen ein Leerzeichen eingefügt
	 */
	public void simpleJoin(Token toJoin, boolean withSpace) {
		//Namen anfügen
		if (withSpace) setName(getName() + " " + toJoin.getName());
		else setName(getName() + toJoin.getName());
		//Positionen verändern
		TextPosition p = toJoin.getTextPosition();
		pos.setEpos(p.getEpos());
		pos.setErow(p.getErow());
		pos.setEcol(p.getEcol());
	}

	public void simpleJoin(Token toJoin) {
		simpleJoin(toJoin, false);
	}
	
	/**Gibt den String für die Klassifizierung bei Text-basierten Klassifizierungen zurück.
	 * Default = Name
	 * @return
	 */
	public String getClassifierName() {
		return getName();
	}
	
	/**Gibt einen Klassifiziererzusatz über den Namen eines Attributs oder leer zurück.
	 * Beim Nicht-Finden wird auch die Referenz gefragt...
	 * Beim Überschreiben ist es übrigens möglich, die Rückgabe auf null zu setzen - solche
	 * Klassifizierer werden bei der Erstellung dann ignoriert.
	 * @param attrName
	 * @return
	 */
	protected String getClassifierByAttributeName(String attrName) {
		String val = getAttributeValue(attrName);
		
		//nicht gefunden, Referenz fragen
		if (val == null && reference != null) {
			val = reference.getAttributeValue(attrName);
		}
		
		if (val != null) return '_' + val;
		return "";
	}
}

/**
 * Datei: ImageTagger.java
 * Paket: de.beimax.testel.mime
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
package de.beimax.testel.mime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.general.Tagger;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;
import de.beimax.testel.token.impl.ImageToken;

/**Tagger, der Image-Tokens vereinfacht
 * @author mkalus
 *
 */
public class ImageTagger implements Tagger {
	protected TestElHandler handler;
	
	private static String mimeType = null;
	
	/**
	 * bei Laden gebrauchte Konstanten
	 */
	private static final int P_NAMEPARTS = 1;
	private static final int P_ICONSIZE = 2;
	private static final int P_ICONSIZES = 3;
	private static final int P_PICSIZES = 4;
	
	/**
	 * Namensbestandteile von Bildern linkes Feld Namensbestandteil, rechtes TYP des Bildes
	 */
	private static String[] nameParts;
	
	/**
	 * Maximalgröße von Icons + weitere Größen
	 */
	private static int ICONSIZE;
	private static int[] iconSizes;
	
	/**
	 * diverse Größen und Namen dazu (Breite/Höhe)
	 */
	private static int[] picSizes;
	private static String[] picTypes;
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Tagger#getType()
	 */
	public String getType() {
		return "Bild-Tagger";
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Tagger#setHandler(de.beimax.testel.TestElHandler)
	 */
	public void setHandler(TestElHandler handler) {
		this.handler = handler;
	}
	
	/**
	 * Lädt die Variablen in den Speicher des Objekts
	 */
	protected void loadVars() throws TestelException {
		//Variablen werden nicht neu geladen - spart Speicher und Rechenzeit
		if (mimeType != null && mimeType.equals(handler.getMimeFactory().getMimeType())) return;
		
		//vom Handler die Datei, etc. erfahren
		File file;
		try {
			file = new File(handler.getMimeFactory().getMimeDir(), "imagetaggerconf.txt");
			if (!file.exists()) throw new IOException("Konnte ImageTagger-Datei " + file + " nicht finden");
		} catch (IOException e) {
			throw new TestelException(e.getLocalizedMessage());
		}
		
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			throw new TestelException("Fehler beim Bearbeiten der ImageTagger-Datei " + file + ":\n" + e.getLocalizedMessage());
		}
		
		LinkedList<String> namePartsList = new LinkedList<String>();
		LinkedList<Integer> picSizesList = new LinkedList<Integer>();
		LinkedList<String> picTypesList = new LinkedList<String>();
		
		String line;
		try {
			int currPart = 0; //welcher Teil wird bearbeitet?
			while ((line = reader.readLine()) != null)
				if (!line.trim().equals("") && line.trim().charAt(0) != '#') {//Kommentarzeilen
					line = line.trim();
					//Änderungen im currentpart
					if (line.equals("[nameParts]")) currPart = P_NAMEPARTS;
					else if (line.equals("[ICONSIZE]")) currPart = P_ICONSIZE;
					else if (line.equals("[iconSizes]")) currPart = P_ICONSIZES;
					else if (line.equals("[picSizes]")) currPart = P_PICSIZES;
					else {
						//je nach current Part:
						switch (currPart) {
						case P_NAMEPARTS: //[nameParts]-Bereich der Datei
							String[] keyval = line.split("=");
							if (keyval.length != 2) throw new TestelException("Im [nameParts] müssen Werte nach key=val-Prinzip kommen:\n" + line);
							//beide Elemente in die Liste
							namePartsList.add(keyval[0].trim());
							namePartsList.add(keyval[1].trim().toUpperCase());
							break;
						case P_ICONSIZE: //[ICONSIZE]-Bereich der Datei
							try {
									ICONSIZE = Integer.valueOf(line);
								} catch (NumberFormatException e) {
									throw new TestelException("Konnte [ICONSIZE]-Element nicht in Zahl umwandeln:\n" + line);
								}
							break;
						case P_ICONSIZES: //[iconSizes]-Bereich der Datei
							String[] numbers = line.split(",");
							iconSizes = new int[numbers.length];
							for (int i = 0; i < numbers.length; i++)
								try {
									iconSizes[i] = Integer.valueOf(numbers[i]);
								} catch (NumberFormatException e) {
									throw new TestelException("Konnte [iconSizes]-Element " + numbers[i] + " nicht in Zahl umwandeln:\n" + line);
								}
							break;
						case P_PICSIZES: //[picSizes]-Bereich der Datei
							keyval = line.split("=");
							if (keyval.length != 2) throw new TestelException("Im [picSizes] müssen Werte nach breite x höhe=val-Prinzip kommen:\n" + line);
							String[] widthheight = keyval[0].split("x");
							if (widthheight.length != 2) throw new TestelException("Im [picSizes] müssen Werte nach breite x höhe=val-Prinzip kommen:\n" + line);
							try {
								int width = Integer.valueOf(widthheight[0]);
								int height = Integer.valueOf(widthheight[1]);
								picSizesList.add(width);
								picSizesList.add(height);
							} catch (NumberFormatException e) {
								throw new TestelException("Konnte [picSizes]-Elemente " + keyval[0] + " nicht in Zahlen umwandeln:\n" + line);
							}
							picTypesList.add(keyval[1].trim().toUpperCase());
							break;
						default:
							throw new TestelException("Es muss als erstes ein [nameParts]-Befehl kommen:\n" + line);
						}
					}
				}
		} catch (IOException e) {
			throw new TestelTaggerException("Fehler beim Bearbeiten der ImageTagger-Datei ImageTagger-Datei " + file + ":\n" + e.getLocalizedMessage());
		}
		
		//aggregierte Werte nun in die Instanvariablen kopieren, sofern nicht oben geschehen
		nameParts = namePartsList.toArray(new String[namePartsList.size()]);
		picSizes = new int[picSizesList.size()];
		Iterator<Integer> it = picSizesList.iterator();
		for (int i = 0; it.hasNext(); i++) picSizes[i] = it.next();
		picTypes = namePartsList.toArray(new String[picTypesList.size()]);
		
		//Mime-Typ setzen
		mimeType = handler.getMimeType();
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Tagger#tag(de.beimax.testel.token.TokenList)
	 */
	public TokenList tag(TokenList list) throws TestelTaggerException {
		logger.info("Vereinfache Bilder mit dem Bild-Tagger");
		try {
			loadVars();
		} catch (TestelException e1) {
			throw new TestelTaggerException("Konnte für den Bild-Tagger keine Daten laden:\n" + e1.getLocalizedMessage());
		}
		
		Iterator<Token> it = list.iterator();
		
		outer:
		while (it.hasNext()) {
			Token t = it.next();
			if (t instanceof ImageToken) {
				ImageToken tok = (ImageToken) t; //cast
				String name = new String(tok.getName());
				//nur Bilder mit width & height bearbeiten!
				if (!tok.attributeExists("width") || !tok.attributeExists("height")) {
					logger.warning("Konnte Bild " + name + " nicht bearbeiten, da width und/oder height-Attribute fehlen");
					continue;
				}
				//ok, alles gut - jetzt parsen der Bilderwerte
				int width, height;
				try {
					width = Integer.valueOf(tok.getAttributeValue("width"));
					height = Integer.valueOf(tok.getAttributeValue("height"));
				} catch (NumberFormatException e) {
					logger.warning("Konnte Bild " + name + " nicht bearbeiten, da width und/oder height-Attribute fehlerhaft sind (" + tok + ")");
					continue;
				}
				//diverse Bildergrößen und andere Heuristiken checken
				
				//einzelPixel
				if (width == 1 && width == 1) {
					tok.setImageClassName("PIXEL"); //neuer Typ für Image
					continue outer;
				}

				//Namensbestandteile prüfen (+=2, weil der Array key=val-sortiert ist)
				for (int i = 0; i < nameParts.length; i+=2)
					if (name.toLowerCase().contains(nameParts[i])) {
						tok.setImageClassName(nameParts[i+1]); //neuer Typ für Image
						continue outer;
					}

				//Icon-Größen?
				if (width <= ICONSIZE && height <= ICONSIZE) {
					tok.setImageClassName("ICON"); //neuer Typ für Image
					continue outer;
				}
				for (int i = 0; i < iconSizes.length; i++)
					if (width == iconSizes[i] && height == iconSizes[i]) {
						tok.setImageClassName("ICON"); //neuer Typ für Image
						continue outer;
					}
				
				//diverses
				for (int i = 0; i < picSizes.length; i+=2)
					if (width == picSizes[i] && height == picSizes[i+1]) {
						tok.setImageClassName(picTypes[i/2]); //neuer Typ für Image
						continue outer;
					}
			}
		}
//		
//		it = list.iterator();
//		
//		while (it.hasNext()) {
//			Token tok = it.next();
//			if (tok instanceof ImageToken)
//				System.out.println(tok);
//		}
		
		return list;
	}

}

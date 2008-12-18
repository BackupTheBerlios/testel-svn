/**
 * Datei: XStreamHelper.java
 * Paket: de.beimax.testel.util
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
package de.beimax.testel.util;

import java.io.File;
import java.io.IOException;

import com.thoughtworks.xstream.XStream;

import de.beimax.testel.exception.TestelException;

/**Wrapper für XStream mit Methoden zum Einfachen Speichern und Laden von Elementen
 * @author mkalus
 *
 */
public class XStreamHelper<E> {
	/**Speichert ein Objekt in einer XML-Datei - bei Bedarf wird diese gzipped
	 * @param file
	 * @param object
	 * @param gzipped
	 * @throws TestelException
	 */
	public void saveXML(File file, E object, boolean gzipped) throws IOException {
		XStream xstream = getXStream();
		
		//Serialisieren
		String xml = xstream.toXML(object);

		//Backup erstellen
		if (file.exists()) {
			file.renameTo(new File(file.toString() + ".backup"));
		}
		
		//Speichern
		if (gzipped) IOHelper.stringtoGzipFile(xml, file);
		else IOHelper.stringtoFile(xml, file);
	}
	
	/**Lädt ein Objekt von einer Datei
	 * @param file
	 * @param gzipped
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public E loadXML(File file, boolean gzipped) throws IOException {
		String data;
		if (gzipped) data = IOHelper.gzipFiletoString(file);
		else data = IOHelper.filetoString(file);
		
		E object;
		
		XStream xstream = getXStream();
		try {
			object = (E) xstream.fromXML(data);
		} catch (Exception e) { //auch Runtime-Exceptions abfangen
			throw new IOException("Umwandlung schlug eines geladenen XML-Stroms fehlt:\n" + e.getLocalizedMessage());
		}
		
		return object;
	}
	
	/**XStream-Getter - damit die Aliase auch richtig eingestellt sind, etc.
	 * @return
	 */
	private XStream getXStream() {
		XStream xstream = new XStream();
		xstream.alias("TokenList", de.beimax.testel.token.TokenList.class);
		xstream.alias("TextPosition", de.beimax.testel.token.TextPosition.class);
		xstream.alias("Token", de.beimax.testel.token.Token.class);
		xstream.alias("NumberToken", de.beimax.testel.token.impl.NumberToken.class);
		xstream.alias("PunctuationToken", de.beimax.testel.token.impl.PunctuationToken.class);
		xstream.alias("SomeToken", de.beimax.testel.token.impl.SomeToken.class);
		xstream.alias("TestElTag", de.beimax.testel.token.impl.TestElTag.class);
		xstream.alias("TextToken", de.beimax.testel.token.impl.TextToken.class);
		xstream.alias("ImageToken", de.beimax.testel.token.impl.ImageToken.class);
		xstream.alias("EOLToken", de.beimax.testel.token.impl.EOLToken.class);
		xstream.alias("MarkupToken", de.beimax.testel.token.impl.MarkupToken.class);
		xstream.alias("ReferenceList", de.beimax.testel.general.ReferenceList.class);
		xstream.alias("ClassifierCollection", de.beimax.testel.classifier.ClassifierCollection.class);
		xstream.alias("TestElVectorClassifier", de.beimax.testel.classifier.impl.TestElVectorClassifier.class);
		
		return xstream;
	}

}

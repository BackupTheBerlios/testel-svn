/**
 * Datei: SubTagger.java
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

import java.util.ListIterator;
import java.util.logging.Logger;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.token.Token;

/**Interface für SubTagger, die von anderen Taggern während des Taggings aufgerufen
 * werden können.
 * @author mkalus
 *
 */
public interface SubTagger {
	//Logger
	static final Logger logger = Logger.getLogger(SubTagger.class.getName());

	/**Handler-Kontext übergeben
	 * @param handler
	 */
	public void setHandler(TestElHandler handler);
	
	/**
	 * Initialisierung des SubTaggers
	 */
	public void init() throws TestelTaggerException;
	
	/**Holt den Namen des Taggers
	 * @return
	 */
	public String getType();
	
	/**Wird vom übergordneten Tagger pro Token aufgerufen - am Ende sollte der iterator
	 * aber wieder am Ausgangspunkt stehen!
	 * @param currentToken
	 * @param iterator
	 * @return true, falls Element gefunden wurde
	 * @throws TestelTaggerException
	 */
	public boolean subTag(Token currentToken, ListIterator<Token> iterator) throws TestelTaggerException;
}

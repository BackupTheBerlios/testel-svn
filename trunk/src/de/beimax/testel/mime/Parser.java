/**
 * Datei: Parser.java
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

import java.util.logging.Logger;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.exception.TestelParserException;
import de.beimax.testel.token.TokenList;

/**Interface für Parser-Klasse. Der Parser ist dafür zuständig, ein normalisiertes
 * Dokument entgegenzunehmen und dieses in eine Tokenliste umzuwandeln.
 * @author mkalus
 *
 */
public interface Parser {
	//Logger
	static final Logger logger = Logger.getLogger(Parser.class.getName());
	
	/**Stellt einen Handler für den Parser ein
	 * @param handler
	 */
	public void setHandler(TestElHandler handler);
	
	/**Parst ein Dokument.
	 * @param document String-Repräsentation des Dokuments
	 * @return TokenListe des Dokuments mit Markup- und SomeText-Tokens.
	 */
	public TokenList parse(String document) throws TestelParserException;
}

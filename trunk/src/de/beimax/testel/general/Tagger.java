/**
 * Datei: Tagger.java
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

import java.util.logging.Logger;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.token.TokenList;

/**Tagger sind für die Bearbeitung einer Token-Liste zuständig. Generell werden
 * mehrere Tagger in einem Durchlauf aufgerufen, z.B. sprachabhängige Tagger etc.
 * @author mkalus
 *
 */
public interface Tagger {
	//Logger
	static final Logger logger = Logger.getLogger(Tagger.class.getName());

	/**Stellt einen Handler für den Tagger ein
	 * @param handler
	 */
	public void setHandler(TestElHandler handler);
	
	/**Gibt Typ des Taggers zurück
	 * @return
	 */
	public String getType();
	
	/**Taggt eine TokenListe.
	 * @param list Eingangsliste
	 * @return Ausgangsliste
	 */
	public TokenList tag(TokenList list) throws TestelTaggerException;
}

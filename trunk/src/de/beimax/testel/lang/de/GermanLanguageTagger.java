/**
 * Datei: GermanLanguageTagger.java
 * Paket: de.beimax.testel.lang.de
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
package de.beimax.testel.lang.de;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.lang.AbstractLanguageTagger;

/**
 * @author mkalus
 *
 */
public class GermanLanguageTagger extends AbstractLanguageTagger {
	/** Konstruktor
	 * @param handler
	 */
	public GermanLanguageTagger(TestElHandler handler) {
		super(handler);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Tagger#getType()
	 */
	public String getType() {
		return "Deutsch";
	}
}

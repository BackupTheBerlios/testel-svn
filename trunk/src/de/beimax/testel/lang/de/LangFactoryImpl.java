/**
 * Datei: LangFactoryImpl.java
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
import de.beimax.testel.general.Tagger;
import de.beimax.testel.lang.AbstractNumberParser;
import de.beimax.testel.lang.LangFactory;

/**Implementierung einer deutschen LangFactory
 * @author mkalus
 *
 */
public class LangFactoryImpl extends LangFactory {

	/* (Kein Javadoc)
	 * @see de.beimax.testel.lang.LangFactory#createLangTagger(de.beimax.testel.TestElHandler)
	 */
	@Override
	public Tagger createLangTagger(TestElHandler handler) {
		return new GermanLanguageTagger(handler);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.lang.LangFactory#createNumberParser(de.beimax.testel.TestElHandler)
	 */
	@Override
	public AbstractNumberParser createNumberParser(TestElHandler handler) throws Exception {
		return GermanNumberParser.getInstance(handler);
	}
}

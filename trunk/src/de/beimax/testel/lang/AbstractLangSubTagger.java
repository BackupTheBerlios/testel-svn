/**
 * Datei: AbstractLangSubTagger.java
 * Paket: de.beimax.testel.lang
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
package de.beimax.testel.lang;

import java.io.File;
import java.io.IOException;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.general.SubTagger;

/**Abstrakte Klasse für SubTagger
 * @author mkalus
 *
 */
public abstract class AbstractLangSubTagger implements SubTagger {
	protected TestElHandler handler;

	/**hole Sprachverzeichnis über Handler
	 * @return
	 */
	protected File getLangDir() throws TestelTaggerException {
		try {
			return handler.getLangFactory().getLangDir();
		} catch (IOException e) {
			throw new TestelTaggerException("Konnte Sprachverzeichnis nicht laden:\n" + e.getLocalizedMessage());
		}
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.SubTagger#setHandler(de.beimax.testel.TestElHandler)
	 */
	public void setHandler(TestElHandler handler) {
		this.handler = handler;
	}
}

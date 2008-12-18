/**
 * Datei: MimeFactoryImpl.java
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

import de.beimax.testel.TestElHandler;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.general.ReferenceTagger;
import de.beimax.testel.general.TaggerCollection;
import de.beimax.testel.general.TestElTagger;
import de.beimax.testel.mime.MimeFactory;
import de.beimax.testel.mime.Normalizer;
import de.beimax.testel.mime.Parser;
import de.beimax.testel.mime.Statistician;

/**
 * @author mkalus
 *
 */
public class MimeFactoryImpl extends MimeFactory {

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.MimeFactory#createNormalizer(de.beimax.testel.TestElHandler)
	 */
	@Override
	public Normalizer createNormalizer(TestElHandler handler) {
		return new HTMLNormalizer(handler);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.MimeFactory#createParser(de.beimax.testel.TestElHandler)
	 */
	@Override
	public Parser createParser(TestElHandler handler) {
		return new HTMLParser(handler);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.MimeFactory#createStatistician(de.beimax.testel.TestElHandler)
	 */
	@Override
	public Statistician createStatistician(TestElHandler handler) {
		return new HTMLStatistician(handler);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.MimeFactory#createTaggerCollection(de.beimax.testel.TestElHandler)
	 */
	@Override
	public TaggerCollection createTaggerCollection(TestElHandler handler) throws TestelException {
		return TaggerCollection.loadCollection(handler); //könnte man auch hardcoden, aber das hier ist flexibler...
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.MimeFactory#createReferenceTagger(de.beimax.testel.TestElHandler, java.lang.String)
	 */
	public ReferenceTagger createReferenceTagger(TestElHandler handler, String lang) {
		return new ReferenceTagger(handler, lang); //generischer Tagger
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.MimeFactory#createTestElTagger(de.beimax.testel.TestElHandler, java.lang.String)
	 */
	@Override
	public TestElTagger createTestElTagger(TestElHandler handler, String lang) throws TestelException {
		try {
			return new TestElTagger(handler, lang); //generischer Tagger
		} catch (TestelException e) {
			throw new TestelException("HTML-Mime-Factory konnte keinen TestEl-Tagger erzeugen:\n" + e.getLocalizedMessage());
		}
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.mime.MimeFactory#getFileExtension()
	 */
	@Override
	public String getFileExtension() {
		return ".html";
	}
}

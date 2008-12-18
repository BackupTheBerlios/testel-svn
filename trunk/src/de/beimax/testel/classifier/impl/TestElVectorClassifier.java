/**
 * Datei: TestElVectorClassifier.java
 * Paket: de.beimax.testel.classifier.impl
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
package de.beimax.testel.classifier.impl;

import java.util.Iterator;

import net.sf.classifier4J.ClassifierException;
import net.sf.classifier4J.bayesian.WordsDataSourceException;

import de.beimax.testel.classifier.AbstractBasicClassifier;
import de.beimax.testel.classifier.impl.c4j.VectorClassifierImpl;
import de.beimax.testel.exception.TestelClassifierException;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;

/**Implementierung eines Klassifizierers, der auf der Basis von Vektorräumen arbeitet
 * @author mkalus
 *
 */
public class TestElVectorClassifier extends AbstractBasicClassifier {
	
	/**
	 * 2 Klassifizeirer: einen positiven und einen negativen...
	 */
	VectorClassifierImpl classifier, negativeClassifier;
	
	public TestElVectorClassifier() {
		this.classifier = new VectorClassifierImpl();
		this.negativeClassifier = new VectorClassifierImpl();
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#getClassifierName()
	 */
	public String getClassifierName() {
		return "Vektorraum-Model";
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#getFileNameExt()
	 */
	public String getFileNameExt() {
		return "ivector";
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.AbstractBasicClassifier#classifyList(de.beimax.testel.token.TokenList)
	 */
	@Override
	public void classifyList(TokenList list, boolean unlearn) throws TestelClassifierException {
		//System.out.println("INIT [" + getClassName() + ", " + unlearn + "]: " + list.getClassifiedList());
		try {
			//Match oder nicht-Match lernen, je nach Angabe
			if (unlearn) {
				negativeClassifier.teachMatch(list.getClassifiedList());
				logger.info("Muster als Nicht-Match für " + getClassName() + " gelernt");
			}
			else {
				classifier.teachMatch(list.getClassifiedList());
				logger.info("Muster als Match für " + getClassName() + " gelernt");
			}
		} catch (WordsDataSourceException e) {
			throw new TestelClassifierException("Datengrundlage lief irgendwie schief:\n" + e.getLocalizedMessage());
		} catch (ClassifierException e) {
			throw new TestelClassifierException("Klassifizierer generierte einen Fehler:\n" + e.getLocalizedMessage());
		}
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#simplifyList(de.beimax.testel.token.TokenList)
	 */
	public TokenList simplifyList(TokenList list) throws TestelClassifierException {
		//Tokens vereinfachen
		//list = simplifyListSomeTokens(list);
		//list = simplifyListTestElTags(list); -- brachte nur Ärger mit sich
		
		//Tokens vereinfachen: Textpositionen löschen, u.ä.
		Iterator<Token> it = list.iterator();
		
		while (it.hasNext()) {
			Token tok = it.next();
			tok.forgetTextPosition(); //Textposition vergessen
		}
		
		return list;
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#classifierMatchesSubList(de.beimax.testel.token.TokenList)
	 */
	public boolean classifierMatchesSubList(TokenList list) {
		try {
			//Prüfung, ob überhaupt ein positiver Treffer vorliegen würde
			double posMatchProbability = classifier.classify(list.getClassifiedList());
			if (posMatchProbability < classifier.getMatchCutoff()) {
				logger.fine("Klassifizierer für " + getClassName() + ": Match=false" +
						", Score=+" + posMatchProbability + "/--, pos:" +
						list.getFirst().getTextPosition() + "  -" + list.getLast().getTextPosition());
				return false;
			}
			else { //ansonsten gegen das negative Matching abgleichen
				boolean isMatch = false;
				double negMatchProbability = negativeClassifier.classify(list.getClassifiedList());
				double diff = posMatchProbability - negMatchProbability;
	
				isMatch = diff > 0;
				
				logger.fine("Klassifizierer für " + getClassName() + ": Match=" + isMatch +
						", Score=+" + posMatchProbability + "/-" + negMatchProbability +
						", Differenz=" + diff + ", pos:" + list.getFirst().getTextPosition() +
						" - " + list.getLast().getTextPosition());
				return isMatch;
			}
		} catch (ClassifierException e) {
			logger.warning("Klassifiziererfehler bei " + list + "\n" + e.getLocalizedMessage());
			return false;
		}
	}

}

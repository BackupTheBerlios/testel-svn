/**
 * Datei: TestElNaiveBayesClassifier.java
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
import net.sf.classifier4J.DefaultTokenizer;
import net.sf.classifier4J.bayesian.BayesianClassifier;
import net.sf.classifier4J.bayesian.WordsDataSourceException;

import de.beimax.testel.classifier.AbstractBasicClassifier;
import de.beimax.testel.classifier.impl.c4j.StopWordProviderImpl;
import de.beimax.testel.classifier.impl.c4j.WordsDataSourceImpl;
import de.beimax.testel.exception.TestelClassifierException;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;

/**Implementierung eines Klassifizierers, der auf der Basis von Naive Bayes arbeitet
 * @author mkalus
 *
 */
public class TestElNaiveBayesClassifier extends AbstractBasicClassifier {
	BayesianClassifier classifier;
	
	public TestElNaiveBayesClassifier() {
		//TestElWordsDataSource und TestElStopWordProvider sind eigene Implementierungen
		classifier = new BayesianClassifier(new WordsDataSourceImpl(),
				new DefaultTokenizer(DefaultTokenizer.BREAK_ON_WHITESPACE),
				new StopWordProviderImpl());
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#getClassifierName()
	 */
	public String getClassifierName() {
		return "Naive Bayes";
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.classifier.Classifier#getFileNameExt()
	 */
	public String getFileNameExt() {
		return "naivebayes";
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
				classifier.teachNonMatch(list.getClassifiedList());
				logger.info("Muster als Nicht-Match gelernt");
			}
			else {
				classifier.teachMatch(list.getClassifiedList());
				logger.info("Muster als Match gelernt");
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
			double matchProbability = classifier.classify(list.getClassifiedList());
			double cutoff = classifier.getMatchCutoff();
			boolean isMatch = matchProbability >= cutoff;
			
			logger.fine("Klassifizierer für " + getClassName() + ": Match=" + isMatch +
					", Score=" + matchProbability + ", Benötigt=" + cutoff + ", Positionen: " +
					list.getFirst().getTextPosition() + " - " + list.getLast().getTextPosition());
			return isMatch;
		} catch (ClassifierException e) {
			logger.warning("Klassifiziererfehler bei " + list + "\n" + e.getLocalizedMessage());
			return false;
		}
	}

}

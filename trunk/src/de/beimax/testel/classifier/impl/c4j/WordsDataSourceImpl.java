/**
 * Datei: StopWordProviderImpl.java
 * Paket: de.beimax.testel.classifier.impl.c4j
 * Projekt: TestEl
 */
package de.beimax.testel.classifier.impl.c4j;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.classifier4J.bayesian.IWordsDataSource;
import net.sf.classifier4J.bayesian.WordProbability;

/**eigenene Implementierung von net.sf.classifier4J.bayesian.SimpleWordsDataSource;
 * @author mkalus
 *
 */
public class WordsDataSourceImpl implements IWordsDataSource, Serializable {
	//Logger
	static final Logger logger = Logger.getLogger(WordsDataSourceImpl.class.getName());
	
	public static final long weightPositive = 100;
	public static final long weightNegative = 1;
	
    /**
	 * long serialVersionUID 
	 */
	private static final long serialVersionUID = -513360497543791940L;
	private Map words = new HashMap();

    @SuppressWarnings("unchecked")
	public void setWordProbability(WordProbability wp) {
        words.put(wp.getWord(), wp);
    }

    /**
     * @see net.sf.classifier4J.bayesian.IWordsDataSource#getWordProbability(java.lang.String)
     */
    public WordProbability getWordProbability(String word) {
        if (words.containsKey(word)) {
            return (WordProbability) words.get(word);
        } else {
            return null;
        }
    }

    public Collection getAll() {
        return words.values();
    }

    /**
     * @see net.sf.classifier4J.bayesian.IWordsDataSource#addMatch(java.lang.String)
     */
    public void addMatch(String word) {
        WordProbability wp = (WordProbability) words.get(word);
        if (wp == null) {
           	logger.info("Lerne Match: " + word + " " + weightPositive + "/0");
            wp = new WordProbability(word, weightPositive, 0);
        } else {
           	long pos = wp.getMatchingCount() + weightPositive;
        	long neg = wp.getNonMatchingCount();
        	logger.info("Lerne Match: " + word + " " + pos + "/" + neg);
            wp.setMatchingCount(wp.getMatchingCount() + 1);
        }
        setWordProbability(wp);
    }

    /**
     * @see net.sf.classifier4J.bayesian.IWordsDataSource#addNonMatch(java.lang.String)
     */
    public void addNonMatch(String word) {
        WordProbability wp = (WordProbability) words.get(word);
        if (wp == null) {
        	logger.info("Lerne Nicht-Match: " + word + " 0/" + weightNegative);
            wp = new WordProbability(word, 0, weightNegative); //negative Gewichtung ist viel stärker
        } else {
        	long pos = wp.getMatchingCount();
        	long neg = wp.getNonMatchingCount() + weightNegative;
        	logger.info("Lerne Nicht-Match: " + word + " " + pos + "/" + neg);
            wp.setNonMatchingCount(neg); //negative Gewichtung ist viel stärker 
        }
        setWordProbability(wp);
    }
}

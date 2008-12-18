/**
 * Datei: StopWordProviderImpl.java
 * Paket: de.beimax.testel.classifier.impl.c4j
 * Projekt: TestEl
 */
package de.beimax.testel.classifier.impl.c4j;

import java.util.Arrays;

import net.sf.classifier4J.IStopWordProvider;
import net.sf.classifier4J.util.ToStringBuilder;

/**Implementation des IStopWordProvider mit Anpassungen für TestEl
 * @author mkalus
 *
 */
public class StopWordProviderImpl implements IStopWordProvider {
    // This array is sorted in the constructor
 	private String[] stopWords = { "" }; //geändert Max Kalus
    private String[] sortedStopWords = null;

    public StopWordProviderImpl() {
        sortedStopWords = getStopWords();
        Arrays.sort(sortedStopWords);
    }

    /**
     * getter method which can be overridden to 
     * supply the stop words. The array returned by this 
     * method is sorted and then used internally
     * 
     * @return the array of stop words
     */
    public String[] getStopWords() {
        return stopWords;
    }

    /**
     * @see net.sf.classifier4J.IStopWordProvider#isStopWord(java.lang.String)
     */
    public boolean isStopWord(String word) {
        if (word == null || "".equals(word)) {
            return false;
        } else {
            // search the sorted array for the word, converted to lowercase
            // if it is found, the index will be >= 0
            return (Arrays.binarySearch(sortedStopWords, word.toLowerCase()) >= 0);
        }
    }

    public String toString() {
        return new ToStringBuilder(this).append("stopWords.size()", sortedStopWords.length).toString();
    }
}

/**
 * Datei: VectorClassifierImpl.java
 * Paket: de.beimax.testel.classifier.impl.c4j
 * Projekt: TestEl
 */

package de.beimax.testel.classifier.impl.c4j;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;


import net.sf.classifier4J.AbstractCategorizedTrainableClassifier;
import net.sf.classifier4J.ClassifierException;
import net.sf.classifier4J.DefaultTokenizer;
import net.sf.classifier4J.IStopWordProvider;
import net.sf.classifier4J.ITokenizer;
import net.sf.classifier4J.Utilities;
import net.sf.classifier4J.vector.HashMapTermVectorStorage;
import net.sf.classifier4J.vector.TermVector;
import net.sf.classifier4J.vector.TermVectorStorage;
import net.sf.classifier4J.vector.VectorUtils;


/**Anpassung des VectorClassifier an TestEl-Bedürfnisse
 *
 */
public class VectorClassifierImpl extends AbstractCategorizedTrainableClassifier {
    public static double DEFAULT_VECTORCLASSIFIER_CUTOFF = 0.80d;
    
    
    private int numTermsInVector = 25;
    private ITokenizer tokenizer;
    private IStopWordProvider stopWordsProvider;
    private TermVectorStorage storage;    
    
    public VectorClassifierImpl() {
        tokenizer = new DefaultTokenizer(DefaultTokenizer.BREAK_ON_WHITESPACE); //Änderung hier
        stopWordsProvider = new StopWordProviderImpl();
        storage = new HashMapTermVectorStorage();
        
        setMatchCutoff(DEFAULT_VECTORCLASSIFIER_CUTOFF);
    }
    
    public VectorClassifierImpl(TermVectorStorage storage) {
        this();
        this.storage = storage;
    }
    
    /**
     * @see net.sf.classifier4J.ICategorisedClassifier#classify(java.lang.String, java.lang.String)
     */
    public double classify(String category, String input) throws ClassifierException {
        
        // Create a map of the word frequency from the input
        Map wordFrequencies = Utilities.getWordFrequency(input, false, tokenizer, stopWordsProvider);
        
        TermVector tv = storage.getTermVector(category);
        if (tv == null) {
            return 0;
        } else {
            int[] inputValues = generateTermValuesVector(tv.getTerms(), wordFrequencies);
            
            return VectorUtils.cosineOfVectors(inputValues, tv.getValues());
        }        
    }


    /**
     * @see net.sf.classifier4J.ICategorisedClassifier#isMatch(java.lang.String, java.lang.String)
     */
    public boolean isMatch(String category, String input) throws ClassifierException {
        return (getMatchCutoff() < classify(category, input));
    }

    

    /**
     * @see net.sf.classifier4J.ITrainable#teachMatch(java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
	public void teachMatch(String category, String input) throws ClassifierException {
        // Create a map of the word frequency from the input
        Map wordFrequencies = Utilities.getWordFrequency(input, false, tokenizer, stopWordsProvider);
        
        // get the numTermsInVector most used words in the input
        Set mostFrequentWords = Utilities.getMostFrequentWords(numTermsInVector, wordFrequencies);

        String[] terms = (String[]) mostFrequentWords.toArray(new String[mostFrequentWords.size()]);
        Arrays.sort(terms);
        int[] values = generateTermValuesVector(terms, wordFrequencies);
        
        TermVector tv = new TermVector(terms, values);
        
        storage.addTermVector(category, tv);        
        
        return;
    }

    /**
     * @param terms
     * @param wordFrequencies
     * @return
     */
    protected int[] generateTermValuesVector(String[] terms, Map wordFrequencies) {
        int[] result = new int[terms.length];
        for (int i = 0; i < terms.length; i++) {
            Integer value = (Integer)wordFrequencies.get(terms[i]);
            if (value == null) {
                result[i] = 0;
            } else {
                result[i] = value.intValue();
            }
            
        }        
        return result;
    }


    /**
     * @see net.sf.classifier4J.ITrainable#teachNonMatch(java.lang.String, java.lang.String)
     */
    public void teachNonMatch(String category, String input) throws ClassifierException {
        return; // this is not required for the VectorClassifier        
    }
}

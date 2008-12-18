package de.beimax.testel.mime.html.util;

import java.io.*;
import java.net.*;

import de.beimax.testel.util.RowColumnNumerReader;

/** This is an adapted class from:
 * http://www.cs.princeton.edu/courses/archive/spring04/cos444/assignments/a1/sources/HTMLTokenizer.java
 * 
  * The <code>HTMLTokenizer</code> class takes an input stream and
  * parses it into "tokens", allowing the tokens to be
  * read one at a time. The html tokenizer can recognize tags,
  * entitites, and raw text.
  * <p>
  * Whitespace that is ignored by browsers is also ignored by instances
  * of this class.
  * <p>
  * Each instance has two flags.  These flags indicate:
  * <ul>
  * <li>Whether the characters of the tokens are converted to lowercase.
  * <li>Whether HTML 'entities' should be parsed.
  * </ul>
  * Both flags are false by default. So you could safely ignore anything below 
  * about HTML 'entities', since you are not going to see them by default.
  * <p>
  * A typical application first constructs an instance of this class and
  * then repeatedly loops, calling the <code>nextToken</code> method in each
  * iteration of the loop until it returns the value <code>TT_EOF</code>
  * <p>
  * <B>note:</B> <code>TT_EOF</code> will also be returned in the case of
  * invalid HTML code
  * @see     java.io.StreamTokenizer
  */
public class HTMLTokenizer {
	private RowColumnNumerReader reader = null;

	private int peekc;
	private boolean pushedBack = false;
	private boolean forceLower = false;
	private boolean parseEntity = true;
	
	/**
	 * After a call to the <code>nextToken</code> method, the field ttpye
	 * contains the type of the token just read. Its value is one of the following:
	 * <ul>
	 * <li><code>TT_EOF</code> indicates that the end of the input stream
	 *     has been reached.
	 * <li><code>TT_TAG</code> indicates that the token is an HTML tag.
	 * <li><code>TT_ENTITY</code> indicates that the token is an HTML entity.
	 * <li><code>TT_TEXT</code> indicates that the token is text between HTML tags.
	 * </ul>
	 *
	 */

	private int ttype = TT_NOTHING;

	/**
	 * A constant indicating that the end of the stream has been read. In the case of
	 * an open-ended tag or open-ended entity (because of invalid HTML), this
	 * constant will be returned.
	 */
	public static final int TT_EOF = -1;
	/**
	 * A constant indicating that an HTML tag has been read.
	 */
	public static final int TT_TAG = -2;
	/**
	 * A constant indicating that an HTML entity has been read.
	 */
	public static final int TT_ENTITY = -3;
	/**
	 * A constant indicating that raw text has been read.
	 */
	public static final int TT_TEXT = -4;

	private static final int TT_NOTHING = -10;

	/**
	 * This field contains a string giving the characters of the current
	 * token. When the current token is an HTML tag, this field contains
	 * the contents of the tag (without the angle brackets.)
	 * <p>
	 * Likewise, if the current token is an entity, this field contains
	 * the text of the entity between the &amp; and ; characters.
	 * <p>
	 * All leading and trailing whitespace will be removed from the string.
	 */
	private String sval;	// string of current token
	
	/**
	 * Field variables that hold the token's start and stop positions
	 * added by Max Kalus
	 */
	private int brow, bcol, erow, ecol, bpos, epos;
	
	/**
	 * creates an HTMLTokenizer for a Reader input stream
	 */
	public HTMLTokenizer(Reader r) {
		reader = new RowColumnNumerReader(r);
		brow = bcol = erow = ecol = bpos = epos = 0; //initialize
	}

	/**
	 * creates an HTMLTokenizer for a file with a given url.  If the url string
	 * doesn't start with "http://", then the string is considered as a local file name.
	 */
	public HTMLTokenizer(String url) throws FileNotFoundException,
											MalformedURLException,
											IOException {

		if (url == null)
			throw new MalformedURLException("url string is null");

		if (url.indexOf("http://") != 0) { //open local file
			FileReader fr = new FileReader(url);
			reader = new RowColumnNumerReader(fr);
		}
		else { // fetch file at url
			URL u = new URL(url);
			BufferedReader br = new BufferedReader(new InputStreamReader(u.openStream()));
			reader = new RowColumnNumerReader(br);
		}
	}

	/**
	 * @return bcol
	 */
	public int getBeginningCol() {
		return bcol;
	}

	/**
	 * @return brow
	 */
	public int getBeginningRow() {
		return brow;
	}

	/**
	 * @return ecol
	 */
	public int getEndCol() {
		return ecol;
	}

	/**
	 * @return erow
	 */
	public int getEndRow() {
		return erow;
	}

	/**
	 * @return erow
	 */
	public int getBeginningPos() {
		return bpos;
	}

	/**
	 * @return erow
	 */
	public int getEndPos() {
		return epos;
	}

	/**
	 * entityMode(false) indicates that html entities should be treated as normal text.  The default is false.
	 */
	public void entityMode(boolean flag) {
		parseEntity = flag;
	}

	/**
	 * lowerCaseMode(true) indicates that all tokens should be returned as lower-case.
	 * The default is false.
	 */
	public void lowerCaseMode(boolean flag) {
		forceLower = flag;
	}

	/** Read the next character */
	private int read() throws IOException {
		return reader.read();
	}

	/**
	 * If the type is TT_TEXT, TT_TAG, or TT_ENTITY, the corresponding text will be returned.
	 * Otherwise, null will be returned. For type TT_TEXT, the text returned will be the
	 * text between tags and entities (or including entities if entityMode is false).
	 * For TT_TAG, the text between the opening and closing brackets will be returned.
	 * For TT_ENTITY, the text between the opening ampersand and the closing
	 * semi-colon will be returned.
	 */
	public String getToken() {
		if ((ttype == TT_NOTHING) || (ttype == TT_EOF))
     		return null;
     	return sval;
	}

	/**
	 * nextToken() returns the type of the token read.  To get the string of the token, use
	 * getToken().
	 */
	public int nextToken() throws IOException {
		if (pushedBack) {
			pushedBack = false;
			return ttype;
		}

		char buf[] = new char[20];
		int i = 0;

		int c, stop1 = -1, stop2 = -1;

		this.bcol = reader.getCol(); //preliminary
		this.brow = reader.getRow();
		this.bpos = reader.getPos();
		if (ttype == TT_NOTHING) c = read();
		else c = peekc;

		sval = null;

		if (c < 0)
			return ttype = TT_EOF;

		while (c <= ' ') { // ignore whitespace b/w tokens
			c = read();
			if (c < 0)	// check for EOF
				return ttype = TT_EOF;
		}

		if (c == '<') {
			ttype = TT_TAG;
			stop1 = '>';
		}
		else if ((parseEntity) && (c == '&')) {
			ttype = TT_ENTITY;
			stop1 = ';';
		}
		else {
			ttype = TT_TEXT;
			buf[i++] = (char) c;
			stop1 = '<';
			if (parseEntity) stop2 = '&';
		}
		this.bcol = reader.getLastCol();
		this.brow = reader.getLastRow();
		this.bpos = reader.getPos()-1;

		c = read();	// move past previous c
		while ((c > 0) && (c != stop1) && (c != stop2)) {
			if (ttype == TT_TEXT && c <= ' ') break;
			else {
				this.ecol = reader.getCol();
				this.erow = reader.getRow();
				this.epos = reader.getPos();				
			}
			if (i >= buf.length) {
				char nb[] = new char[buf.length * 2];
				System.arraycopy(buf, 0, nb, 0, buf.length);
				buf = nb;
			}

			buf[i++] = (char) c;
			c = read();
		}

		if ((c < 0) && (ttype != TT_TEXT)) return ttype = TT_EOF;
	    sval = String.copyValueOf(buf, 0, i);
	    sval.trim();
	    if (forceLower)
			sval = sval.toLowerCase();
		if (ttype == TT_TEXT) {
			if (sval.length() == 1) { //Fehlerfix bei einzelnen Buchstaben, etc.
				this.ecol = reader.getLastCol();
				this.erow = reader.getLastRow();
				this.epos = reader.getPos() - 1;
			}
			peekc = c;
		}
		else {
			this.ecol = reader.getCol();
			this.erow = reader.getRow();
			this.epos = reader.getPos();
			peekc = read();
		}

	    return ttype;
	}

	/**
	 * nextTextToken repeatedly calls nextToken() until EOF is reached or a text type
	 * token is found.  If the token is found, the text is returned.  Otherwise,
	 * null is returned.
	 */
	public String nextTextToken() throws IOException {
		while (nextToken() != TT_EOF) {
			if (ttype == TT_TEXT)
				return sval;
		}

		return null;
	}

	/**
	 * nextTagToken repeatedly calls nextToken() until EOF is reached or a tag type
	 * token is found.  If the token is found, the text of the tag is returned.  Otherwise,
	 * null is returned.
	 */
	public String nextTagToken() throws IOException {
		while (nextToken() != TT_EOF) {
			if (ttype == TT_TAG)
				 return sval;
		}

		return null;
	}

	/**
	 * nextEntityToken repeatedly calls nextToken() until EOF is reached or an entity type
	 * token is found.  If the token is found, the text of the entity is returned.  Otherwise,
	 * null is returned.
	 */
	public String nextEntityToken() throws  IllegalStateException, 
											IOException {
		if (!parseEntity)
			throw new IllegalStateException("parseEntity is false");

		while (nextToken() != TT_EOF) {
			if (ttype == TT_TAG)
				return sval;
		}

		return null;
	}

	/**
	 * nextTextMatch repeatedly calls nextToken() until EOF is reached or a an exact match
	 * is found between the specified phrase and a type TT_TEXT token.  Use the method lowerCaseMode()
	 * to set the case sensitivity.  The method returns a boolean value indicating whether a match
	 * was found.
	 */
	public boolean nextTextMatch(String phrase) throws IOException, IllegalArgumentException {
		if (phrase == null)
			throw new IllegalArgumentException("phrase is null");

		while (nextToken() != TT_EOF) {
			if (ttype == TT_TEXT)
				if ((sval).equals(phrase)) return true;
		}

		return false;
	}

	/**
	 * nextTextSubstring repeatedly calls nextToken() until EOF is reached or the specified phrase
	 * is found in the html text.  Use the method lowerCaseMode() to set the case sensitivity.
	 * The method returns the entire String token in which it was found.  For example, nextTextSubstring("auction")
	 * could return the string, "The auction began on Monday, July 5, 2000".  If the phrase is not found,
	 * null is returned.
	 */
	public String nextTextSubstring(String phrase) throws IOException, IllegalArgumentException {
		if (phrase == null)
			throw new IllegalArgumentException("phrase is null");

		while (nextToken() != TT_EOF) {
			if (ttype == TT_TEXT)
				if ((sval).indexOf(phrase) > -1) return sval;
		}

		return null;
	}

	/**
	 * nextTagMatch repeatedly calls nextToken() until EOF is reached or a an exact match
	 * is found between the specified tag and a type TT_TAG token.  Use the method lowerCaseMode()
	 * to set the case sensitivity.  The method returns a boolean value indicating whether a match
	 * was found.
	 */
	public boolean nextTagMatch(String tag) throws IOException, IllegalArgumentException {
		if (tag == null)
			throw new IllegalArgumentException("tag is null");

		while (nextToken() != TT_EOF) {
			if (ttype == TT_TAG)
				if ((sval).equals(tag)) return true;
		}

		return false;
	}

	/**
	 * nextTagSubstring repeatedly calls nextToken() until EOF is reached or the specified tag
	 * is found as a substring of an html tag.  Use the method lowerCaseMode() to set the case sensitivity.
	 * The method returns the entire text of the tag in which it was found or null if it isn't found.
	 */
	public String nextTagSubstring(String tag) throws IOException, IllegalArgumentException {
		if (tag == null)
			throw new IllegalArgumentException("tag is null");

		while (nextToken() != TT_EOF) {
			if (ttype == TT_TAG)
				if ((sval).indexOf(tag) > -1) return sval;
		}

		return null;
	}

	/**
	 * nextEntityMatch repeatedly calls nextToken() until EOF is reached or a an exact match
	 * is found between the specified entity and a type TT_ENTITY token.  Use the method lowerCaseMode()
	 * to set the case sensitivity.  The method returns a boolean value indicating whether a match
	 * was found.
	 */
	public boolean nextEntityMatch(String entity) throws IOException, IllegalArgumentException, IllegalStateException {
		if (!parseEntity)
			throw new IllegalStateException("parseEntity is false");

		if (entity == null)
			throw new IllegalArgumentException("entity is null");

		while (nextToken() != TT_EOF) {
			if (ttype == TT_ENTITY)
				if ((sval).equals(entity)) return true;
		}

		return false;
	}

	/**
	 * nextEntitySubstring repeatedly calls nextToken() until EOF is reached or the specified entity
	 * is found as a substring of an html entity.  Use the method lowerCaseMode() to set the case sensitivity.
	 * The method returns the entire text of the entity in which it was found or null if it isn't found.
	 */
	public String nextEntitySubstring(String entity) throws IOException, IllegalArgumentException, IllegalStateException {
		if (!parseEntity)
			throw new IllegalStateException("parseEntity is false");

		if (entity == null)
			throw new IllegalArgumentException("entity is null");

		while (nextToken() != TT_EOF) {
			if (ttype == TT_ENTITY)
				if ((sval).indexOf(entity) > -1) return sval;
		}

		return null;
	}

	/**
	 * The pushBack() method allows you to 'unread' the last token so
	 * that the next call to nextToken() will return the same value
	 */
	public void pushBack() {
		if (ttype != TT_NOTHING)
			pushedBack = true;
	}
	
	/**
	 * The method toString() returns a string representation of the current token.
	 */
	public String toString() {
		String ret = new String();
		switch (ttype) {
			case TT_EOF:
				ret = "EOF";
				break;
			case TT_TAG:
				ret = "TAG[" + sval + "]" + getDelim();
				break;
			case TT_ENTITY:
				ret = "ENTITY[" + EntityTransformer.decode("&" + sval + ";") + "]" + getDelim();
				break;
			case TT_TEXT:
				ret = "TEXT[" + sval + "]" + getDelim();
				break;
			case TT_NOTHING:
				ret = "NOTHING";
				break;
		}
		return ret;
	}
	
	private String getDelim() {
		return "(" + getBeginningPos() + ":" + getEndPos() + "=" + getBeginningRow() + ":" + getBeginningCol() + "-" + getEndRow() + ":" + getEndCol() + ")";
	}
}


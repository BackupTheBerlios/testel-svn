/**
 * Datei: Classifier.java
 * Paket: de.beimax.testel.classifier
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
package de.beimax.testel.classifier;

import java.util.ListIterator;
import java.util.logging.Logger;

import de.beimax.testel.exception.TestelClassifierException;
import de.beimax.testel.token.SubTokenList;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;

/**Schnittstelle für Klassifiziererklassen
 * @author mkalus
 *
 */
public interface Classifier {
	//Logger
	static final Logger logger = Logger.getLogger(Classifier.class.getName());
	
	/**Gibt einen für Menschen sinnvollen Klassifizierernamen zurück
	 * @return
	 */
	public String getClassifierName();

	/**Gibt einen eindeutigen Bezeichner für die Erweiterung des Dateinamens zurück
	 * @return
	 */
	public String getFileNameExt();
	
	/**Fügt eine Token-Liste ein - dies ist die eigentliche Lernroutine des
	 * Klassifizierers
	 * @param subList Unterliste, die eingefügt werden soll (Liste selbst wird nicht verändert in der Methode)
	 * @param start StartPosition der Unterliste innerhalb der Liste
	 * @param stop StopPosition der Unterliste innerhalb der Liste
	 * @param completeList Komplette TokenListe
	 * @throws TestelClassifierException
	 */
	public void insertTokenList(SubTokenList subList, TokenList completeList) throws TestelClassifierException;	
	public void insertTokenList(TokenList subList, int start, int stop, TokenList completeList) throws TestelClassifierException;
	
	/**Klasse des Klassifizierers zurückgeben, als z.B. ÜBERSCHRIFT, etc.
	 * @return
	 */
	public String getClassName();
	
	/**Gibt eine Repräsentation des Start-Tokens zurück
	 * @return
	 */
	public String getStartTokenRep();
	
	/**Gibt eine Repräsentation des Stop-Tokens zurück
	 * @return
	 */
	public String getStopTokenRep();
	
	/**Gibt eine Repräsentation des Start- und Stop-Tokens zurück (gemeinsames Merkmal)
	 * @return
	 */
	public String getStartStopTokenRep();
	
	/**Gibt true zurück, wenn die übergebene Token-Liste zu einer bestehenden Liste hinzugefügt
	 * werden könnte, d.h., wenn sie die selben Anfangs- und End-Tokens hat, die selbe Klasse
	 * besitzt und die selben Parameter...
	 * @param list Liste, deren Inhalt evt. hinzugefügt werden soll (mit TestEl-Tags)
	 * @param classifier Klassifizierer-Repräsentation der selben Liste
	 * @return
	 */
	public boolean isAddable(TokenList list, Classifier classifier);
	
	/**Gibt true zurück, wenn die übergebene Token-Liste von Klassifizierer korrekt klassifiziert
	 * werden konnte. Bei der TokenListe handelt es sich um eine Unterliste innerhalb der
	 * Grenztags.
	 * @param list
	 * @return
	 */
	public boolean classifierMatchesSubList(TokenList list);
	
	/**true, falls der Klassifizierer Greedy ist...
	 * @return
	 */
	public boolean isGreedy();
	
	/**True, falls kein Nesting erlaubt wird
	 * @return
	 */
	public boolean noNesting();

	/**Vereinfacht die Liste insofern, dass z.B. doppelt vorkommende SOMETOKENS herausgenommen
	 * werden können - die Implementierung erfolgt in den Methoden unten:
	 * simplifyListSomeTokens bzw. simplifyListTestElTags
	 * @param list
	 * @return
	 * @throws TestelClassifierException
	 */
	public TokenList simplifyList(TokenList list) throws TestelClassifierException;

	/**Findet eine Unterliste die auf die Parameter dieses Klassifizierers passen oder
	 * null, falls innere Tags nicht gefunden wurden.
	 * @param subList Unterliste ohne gefundene Start- und End-Tags (die Liste selbst wird nicht verändert)
	 * @param start Start-Position der Unterliste
	 * @param stop Start-Position der Unterliste
	 * @param completeList
	 * @return
	 */
	public SubTokenList getInnerOuterListToMatch(TokenList subList, int start, int stop, TokenList completeList) throws TestelClassifierException;
	
	/**Führt ein Match aus: Übergeben wird die Liste und Anfangs und Endpunkt eines möglichen
	 * Matches - es wird von der Methode erwartet, dass sie die Tokenliste über den Iterator
	 * entsprechend verändert, d.h. Tokens vorher oder nachher einfügt und zwar sowohl die
	 * testel:match-Token als auch die testel:tag-Token.
	 * @param list Token-Liste
	 * @param startPos Anfangsposition des möglichen Matches
	 * @param stopPos Endposition des möglichen Matches
	 * @param it Iterator, der Liste verändern darf
	 * 	vermeiden - sollte bei externen Aufrufen auf true gesetzt werden
	 * @throws TestelClassifierException
	 * @return Position des EndTokens, falls es einen Treffer gab, ansonsten -1
	 */
	public int match(TokenList list, int startPos, int stopPos, ListIterator<Token> it) throws TestelClassifierException;
}

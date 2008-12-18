/**
 * Datei: AbstractLanguageTagger.java
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

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.logging.Level;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.config.Config;
import de.beimax.testel.exception.TestelConfigException;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.general.SubTagger;
import de.beimax.testel.general.Tagger;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;

/**
 * @author mkalus
 *
 */
public abstract class AbstractLanguageTagger implements Tagger {
	protected TestElHandler handler;
	
	protected SubTagger[] subTaggers; //Liste der Subtaggers
	
	/** Konstruktor (ruft setHandler und getSubTaggers auf)
	 * @param handler
	 */
	public AbstractLanguageTagger(TestElHandler handler) {
		setHandler(handler);
	}
	
	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Tagger#setHandler(de.beimax.testel.TestElHandler)
	 */
	public void setHandler(TestElHandler handler) {
		this.handler = handler;
	}
	
	/**
	 * Gibt die SubTaggers zurück, erster Durchlauf
	 * @return
	 */
	public SubTagger[] getSubTaggersFirstPass() throws TestelTaggerException {
		try {
			return getTaggersFromProperties("subtaggers_1_" + handler.getLangFactory().getLang());
		} catch (TestelException e) {
			throw new TestelTaggerException("Fehler beim Erstellen der SubTagger-Liste für den zweiten Durchlauf:\n" + e.getLocalizedMessage());
		}
	}

	/**
	 * Gibt SubTaggers für einen zweiten durchlauf zurück - oder null, falls es keinen
	 * 2. Durchlauf gibt
	 * @return
	 */
	public SubTagger[] getSubTaggersSecondPass() throws TestelTaggerException {
		try {
			return getTaggersFromProperties("subtaggers_2_" + handler.getLangFactory().getLang());
		} catch (TestelConfigException e) {
			return null; //kein Eintrag vorhanden - das ist ok!
		} catch (TestelTaggerException e) {
			throw new TestelTaggerException("Fehler beim Erstellen der SubTagger-Liste für den zweiten Durchlauf:\n" + e.getLocalizedMessage());
		}
	}
	
	/**Holt sich aus den Properties (Schlüssel key) eine SubTagger-Liste
	 * @param key
	 * @return
	 * @throws TestelTaggerException 
	 */
	protected SubTagger[] getTaggersFromProperties(String key) throws TestelTaggerException, TestelConfigException {
		//Config-Eintrag holen
		String colstring = Config.getConfig(key);
		
		if (colstring == null) throw new TestelConfigException("Es wurde keine Liste für SubTagger gefunden - existiert der Property-Eintrag " + key + "?");
		
		//Ok - jetzt aufteilen und Tagger erzeugen
		String[] colarray = colstring.split(",");
		LinkedList<SubTagger> list = new LinkedList<SubTagger>();
		
		for (int i = 0; i < colarray.length; i++) {
			SubTagger subTagger;
			if (colarray[i].trim().equalsIgnoreCase("NUMPARSER"))
				try {
					subTagger = handler.getLangFactory().createNumberParser(handler);
				} catch (Exception e) {
					logger.warning("Nummern-Parser konnte nicht erzeugt werden:\n" + e.getLocalizedMessage());
					continue;
				}
			else
				try {
					Class taggerClass = Class.forName(colarray[i].trim());
					Object taggerObject = taggerClass.newInstance();
					subTagger = (SubTagger) taggerObject;
				} catch (Exception e) {
					logger.warning("Klasse " + colarray[i] + " existiert nicht oder ist keine " +
							"Implementation von SubTagger!");
					throw new TestelTaggerException("Klasse " + colarray[i] + " existiert nicht oder ist keine " +
							"Implementation von SubTagger!");
				}
			list.add(subTagger);
		}
		return list.toArray(new SubTagger[list.size()]);
	}

	/* (Kein Javadoc)
	 * @see de.beimax.testel.general.Tagger#tag(de.beimax.testel.token.TokenList)
	 */
	public TokenList tag(TokenList list) throws TestelTaggerException {
		logger.info("SprachTagger '" + getType() + "' gestartet");
		
		subTaggers = getSubTaggersFirstPass();
		list = runTaggers(list, subTaggers);
		
		subTaggers = getSubTaggersSecondPass();
		if (subTaggers != null) {
			logger.info("Zweiter Durchlauf für SprachTagger '" + getType() + "' gestartet");
			list = runTaggers(list, subTaggers);
		}
		
		
		return list;
	}
	
	private TokenList runTaggers(TokenList list, SubTagger[] subTaggers) throws TestelTaggerException {
		//Subtaggers initialisieren
		for (int i = 0; i < subTaggers.length; i++)
			if (subTaggers[i] != null) { //falls einer der SubTaggers nicht erzeugt werden konnte
				logger.finer("Initialisiere " + subTaggers[i]);
				subTaggers[i].setHandler(handler);
				try {
					subTaggers[i].init();
				} catch (TestelTaggerException e) {
					throw new TestelTaggerException("Im SubTagger '" + subTaggers[i] + "' trat bei der Initialisierung ein Fehler auf:\n" + e.getLocalizedMessage());
				}
			}
		
		//Alle Subtagger bereit - jetzt Liste durchlaufen
		ListIterator<Token> iterator = list.listIterator();
		
		while (iterator.hasNext()) {
			Token tok = iterator.next();
			if (logger.getLevel() == Level.FINEST)
				logger.finest("Checke: " + tok.toString());
			//Subtaggers durchlaufen
			inner:
			for (int i = 0; i < subTaggers.length; i++)
				if (subTaggers[i] != null && subTaggers[i].subTag(tok, iterator)) break inner;
		}

		return list;
	}

}

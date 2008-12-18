/**
 * Datei: ClassifierFactory.java
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

import java.util.logging.Logger;

import de.beimax.testel.TestElHandler;
import de.beimax.testel.config.Config;
import de.beimax.testel.exception.TestelClassifierException;
import de.beimax.testel.exception.TestelException;

/**Factory-Klasse für Klassifizierer
 * @author mkalus
 *
 */
public class ClassifierFactory {
	//Logger
	static final Logger logger = Logger.getLogger(ClassifierFactory.class.getName());

	/**holt sich einen Klassifizierer - der Klassifizierer wird in der
	 * Properties-Datei abgelegt
	 * @return
	 */
	public static Classifier getClassifier(TestElHandler handler) throws TestelException {
		String nameOfClass = Config.getConfig("classifier_" + handler.getMimeType() + "_" + handler.getLangFactory().getLang()) ;
		
		if (nameOfClass == null)
			throw new TestelClassifierException("Kein Klassifizierer angegeben! Existiert ein Eintrag classifier_"
					+ handler.getMimeType() + "_" + handler.getLangFactory().getLang() + " in der properties-Datei?");
		
		Classifier back = null;
		try {
			Class classifierClass = Class.forName(nameOfClass);
			Object classifierObject = classifierClass.newInstance();
			back = (Classifier) classifierObject;
		} catch (Exception e) {
			String errormsg = "Konnte keinen Klassifizierer erzeugen! Der Name der Klasse war: " + nameOfClass + ".";
			logger.warning(errormsg);
			throw new TestelException(errormsg);
		}
		
		return back;
	}
}

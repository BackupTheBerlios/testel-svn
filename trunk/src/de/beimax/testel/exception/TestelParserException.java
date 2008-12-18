/**
 * Datei: TestelParserException.java
 * Paket: de.beimax.testel.exceptions
 * Projekt: Testel
 *
 * Copyright (C) 2008 Maximilian Kalus.  All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package de.beimax.testel.exception;

/**Ausnahme, die bei Parser-Fehlern aufgerufen wird
 * @author mkalus
 *
 */
public class TestelParserException extends TestelException {
	private static final long serialVersionUID = -9080843386042442111L;

	public TestelParserException() {
		super();
	}

	public TestelParserException(String description) {
		super(description);
	}
}

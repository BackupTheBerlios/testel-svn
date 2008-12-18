/**
 * Datei: TestElHandler.java
 * Paket: de.beimax.testel
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
package de.beimax.testel;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.logging.Logger;

import de.beimax.testel.config.Config;
import de.beimax.testel.exception.TestelConfigException;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.exception.TestelTaggerException;
import de.beimax.testel.general.DocumentTagger;
import de.beimax.testel.general.Tagger;
import de.beimax.testel.general.TaggerCollection;
import de.beimax.testel.general.Trainer;
import de.beimax.testel.lang.LangFactory;
import de.beimax.testel.mime.MimeFactory;
import de.beimax.testel.mime.Normalizer;
import de.beimax.testel.mime.Parser;
import de.beimax.testel.mime.Statistician;
import de.beimax.testel.token.Token;
import de.beimax.testel.token.TokenList;
import de.beimax.testel.util.IOHelper;

/**Der Handler ist der Kern der Anwendung -  diese Klasse (oder eine, die in der
 * Konfiguration unter dem Punkt testelhandler angegeben wurde), wird von TestEl.java
 * geladen und ausgeführt. 
 * @author mkalus
 *
 */
public class TestElHandler {
	//Logger
	static final Logger logger = Logger.getLogger(TestElHandler.class.getName());

	/**Stati der Kommandozeile
	 */
	public static final int MODE_PRETRAIN = 1;
	public static final int MODE_TRAIN = 2;
	public static final int MODE_TAG = 3;
	public static final int MODE_NORMALIZEONLY = 4;
	public static final String[] modename = {"NN", "PreTrain", "Train", "Tag", "Normalize Only"}; 
	
	/**Aktueller Status
	 */
	private int mode = 0; //Mode

	/**
	 * Dryrun, leiser und inline Modus, überspringe Normalisierer, input-Charset
	 */
	private boolean dryrun;
	private boolean quiet;
	private boolean inline = false;
	private boolean skipnormalizer = false;
	private boolean fewertags = false;
	private String charset = null;
	
	/**
	 * Informationen zum Dokument etc.
	 */
	private String document = null;
	private TokenList tokenList = null;
	private String mimetype = null;
	private URL url;
	private File designatedOutput;
	private File designatedOutputXML;
	private Statistician statistician = null;
	
	/**
	 * tokenLog-Datei
	 */
	private File tokenLog;
	
	/**
	 * Factory-Klassen
	 */
	private LangFactory langFactory;
	private MimeFactory mimeFactory;
	
	/**
	 * Konstruktor
	 * @param mode
	 * @param input
	 * @param output
	 * @param quiet
	 * @param dryrun
	 */
	public TestElHandler(int mode, URL input, File output, boolean quiet, boolean dryrun, boolean fewertags) {
		this.mode = mode;
		this.quiet = quiet;
		this.dryrun = dryrun;
		this.fewertags = fewertags;
		this.url = input;
		this.designatedOutput = output;
		logger.info("Neuer TestElHandler für " + input + " im " + modename[mode] + "-Modus initialisiert");
	}
	
	/**
	 * Aktiviere Inline-Modus
	 */
	public void activateInline() {
		inline = true;
	}
	
	/**
	 * Aktiviere Normalizer
	 */
	public void activateNormalizer() {
		skipnormalizer = false;
	}
	
	/**
	 * De-aktiviere Normalizer
	 */
	public void deactivateNormalizer() {
		skipnormalizer = true;
	}
	
	/**Stelle Mime-Type auf einen bestimmten ein
	 * @param mime Kurzform eines Mime-Typs, z.B. text/html
	 */
	public void setMimeType(String mime) {
		mimetype = mime;
		logger.fine("Mime-Typ auf " + mime + " gesetzt");
	}
	
	/** Getter für mimetype
	 * @return mimetype
	 */
	public String getMimeType() {
		return mimetype;
	}

	/** Getter für document
	 * @return document
	 */
	public String getDocument() {
		return document;
	}

	/** Getter für TokenList
	 * @return tokenList
	 */
	public TokenList getTokenList() {
		return tokenList;
	}

	/** Getter für Statistician
	 * @return statistician
	 */
	public Statistician getStatistician() {
		return statistician;
	}

	/** Getter für dryrun
	 * @return dryrun
	 */
	public boolean isDryrun() {
		return dryrun;
	}

	/** Getter für inline
	 * @return inline
	 */
	public boolean isInline() {
		return inline;
	}

	/** Getter für mode
	 * @return mode
	 */
	public int getMode() {
		return mode;
	}

	/** Getter für quiet
	 * @return quiet
	 */
	public boolean isQuiet() {
		return quiet;
	}

	/** Getter für fewertags
	 * @return fewertags
	 */
	public boolean isFewertags() {
		return fewertags;
	}

	/** Getter für url
	 * @return url
	 */
	public URL getURL() {
		return url;
	}

	/** Getter für langFactory
	 * @return langFactory
	 */
	public LangFactory getLangFactory() {
		return langFactory;
	}

	/** Getter für mimeFactory
	 * @return mimeFactory
	 */
	public MimeFactory getMimeFactory() {
		return mimeFactory;
	}
	
	/**Setzt XML-Ziel 
	 * @param output
	 */
	public void setXMLOutput(File output) {
		designatedOutputXML = output;
	}
	
	/**Setzt das Token-Log-Ziel
	 * @param output
	 */
	public void setTokenLog(File output) {
		tokenLog = output;
	}

	/**Setzt das Charset/Eincoding der Eingabedatei
	 * @param output
	 */
	public void setEncoding(String encoding) {
		charset = encoding;
	}

	/**
	 * Diese Methode wird zum Starten des eigentlichen Parsing/Tagging-Prozesses
	 * aufgerufen.
	 */
	public void handle() throws TestelException {
		long starttime = System.currentTimeMillis();
		
		if (!quiet)
			System.out.println("Bearbeite URL " + url);
		
		//Tokens über aktuellen Handler informieren
		Token.setHandler(this);
		
		//Dokument laden und Mime-Type feststellen
		try {
			loadRawDocument();
		} catch (Exception e) {
			throw new TestelException("Fehler beim Laden von " + url + ":\n" + TestEl.getStackTrace(e));
		}
		//Dokument geladen?
		if (document == null) throw new TestelException("Fehler beim Laden von " + url + ":\nDokument war leer");
		
		//Factories laden
		try {
			loadFactories();
		} catch (Exception e) {
			throw new TestelException("Fehler beim Laden der Factory-Klassen:\n" + TestEl.getStackTrace(e));
		}
		
		//normalsieren, wenn Modus & Einstellung stimmt
		if (mode == MODE_PRETRAIN || mode == MODE_TAG || mode == MODE_NORMALIZEONLY) {
			if (skipnormalizer)
				logger.info("Überspringe Normalisierer, wie auf der Kommandozeile gewünscht");
			else
				try {
					Normalizer normalizer = mimeFactory.createNormalizer(this);
					if (normalizer != null) document = normalizer.normalize(document);
				} catch (Exception e) {
					throw new TestelException("Fehler beim Normalisieren von " + url + ":\n" + TestEl.getStackTrace(e));
				}
		}
		if (document == null) throw new TestelException("Fehler beim Normalisieren von " + url + ":\nDokument wurde leer");
		
		//nur-Normalisierungsmodus:
		if (mode == MODE_NORMALIZEONLY) {
			//Ausgabedaten richtig setzen
			makeOutputFiles();
			//Dateien speichern
			saveDocs(document);
			return;
		} //Ende nur-Normalisierungsmodus - Rückkehr in die aufrufende Methode
		
		//Parser laden
		try {
			Parser parser = mimeFactory.createParser(this);
			if (parser == null) throw new TestelConfigException("Parser ist null - MimeFactory muss einen Parser erzeugen");
			tokenList = parser.parse(document);
		} catch (Exception e) {
			throw new TestelException("Fehler beim Parsen von " + url + ":\n" + TestEl.getStackTrace(e));
		}
		if (tokenList == null || tokenList.size() == 0) throw new TestelException("Fehler beim Parsen von " + url + ":\nTokenliste ist leer");
		doTokenLog();

		//SprachTagger laden
		try {
			Tagger langTagger = langFactory.createLangTagger(this);
			if (langTagger != null) tokenList = langTagger.tag(tokenList);
		} catch (Exception e) {
			throw new TestelException("Fehler beim Taggen mit dem Sprachtagger in " + url + ":\n" + TestEl.getStackTrace(e));
		}
		doTokenLog();
		
		//Statistiker laden und Statistiken aggregieren
		try {
			statistician = mimeFactory.createStatistician(this);
			if (statistician != null) statistician.aggregateStatistics(tokenList);
		} catch (Exception e) {
			throw new TestelException("Fehler beim Erstellen/Bearbeiten des Statistik-Moduls in " + url + ":\n" + TestEl.getStackTrace(e));
		}
		doTokenLog();
		
		//TaggerCollection laden
		TaggerCollection coll;
		try {
			coll = TaggerCollection.loadCollection(this);
		} catch (Exception e) {
			throw new TestelException("Fehler beim Laden der TaggerCollection für Mime-Typ " + mimetype + " in " + url + ":\n" + TestEl.getStackTrace(e));
		}
		
		//TaggerCollection abarbeiten
		try {
			workCollection(coll);
		} catch (Exception e) {
			throw new TestelException("Fehler beim Bearbeiten der TaggerCollection für Mime-Typ " + mimetype + " in " + url + ":\n" + TestEl.getStackTrace(e));
		}
		doTokenLog();
		
		//fertig ist die Tag-Liste - zumindest vor dem Taggen - das passiert unten

		//Ausgabedaten richtig setzen
		makeOutputFiles();

		//nun Mode-spezifische Dinge tun
		if (mode == MODE_TRAIN) {
			//Trainer starten
			Trainer trainer = new Trainer(this);
			try {
				trainer.train(tokenList);
			} catch (TestelException e) {
				throw new TestelException("Fehler beim Trainieren von " + url + ":\n" + TestEl.getStackTrace(e));
			}
			
			//im Train-Modus die pretrain-Datei verschieben
			if (!dryrun) {
				String file, path;
				File toPath;
				try {
					path = IOHelper.getPath(url);
					file = IOHelper.getFileName(url);
					toPath = new File(getMimeFactory().getMimeDir(), "trained");
				} catch (MalformedURLException e) {
					throw new TestelException("Konnte URL " + url + " nicht in Datei auflösen");
				} catch (IOException e) {
					throw new TestelException("Konnte Zielverzeichnis 'trained' für Mime-Typ " + getMimeType() + " nicht finden, um die pretrained-Datei zu verschieben\n" + TestEl.getStackTrace(e));
				}
				File from = new File(path, file);
				File to = new File(toPath, file);
				
				//gzip für Ziel?
				boolean gzip;
				String gzipC = Config.getConfig("gzip_trained_" + getMimeType());
				if (gzipC != null && gzipC.equalsIgnoreCase("true")) gzip = true;
				else gzip = false;
				
				if (gzip) {
					to = new File(toPath, file + ".gz");
					try {
						IOHelper.stringtoGzipFile(document, to);
					} catch (IOException e) {
						throw new TestelException("Konnte " + to + " nicht speichern:\n" + TestEl.getStackTrace(e));
					}
					if (!from.delete())
						throw new TestelException("Konnte " + from + " nicht löschen");
				} else //kein gzip: Einfach umbenennen
					if (!from.renameTo(to))
						throw new TestelException("Konnte " + from + " nicht nach " + to + " verschieben");
			}
		} else { //alle anderen Modi
			//Referenz-Tagger laden
			try {
				tokenList = mimeFactory.createReferenceTagger(this, langFactory.getLang()).tag(tokenList);
			} catch (Exception e) {
				throw new TestelException("Fehler beim Taggen mit dem Referenz-Tagger in " + url + ":\n" + TestEl.getStackTrace(e));
			}
			doTokenLog();
			
			//TestEl-Tagger laden
			try {
				tokenList = mimeFactory.createTestElTagger(this, langFactory.getLang()).tag(tokenList);
			} catch (Exception e) {
				throw new TestelException("Fehler beim Taggen mit dem TestEl-Tagger in " + url + ":\n" + TestEl.getStackTrace(e));
			}
			doTokenLog();
			
			//DocumentTagger laden und getaggedes Dokument holen
			String taggedDoc;
			try {
				DocumentTagger documentTagger = new DocumentTagger();
				//komplett taggen
				boolean allTags;
				if (mode == MODE_PRETRAIN) allTags = true;
				else allTags = false;
				if (inline) taggedDoc = documentTagger.createInlineDocument(document, tokenList, allTags, fewertags);
				else taggedDoc = documentTagger.createXMLDescription(document, tokenList, url, designatedOutput);
			} catch (Exception e) {
				throw new TestelException("Fehler beim Erstellen der Tag-Daten:\n" + TestEl.getStackTrace(e));
			}
				
			//Dateien speichern
			saveDocs(taggedDoc);
		}
		
		//die wesentlichen Ressourcen freigeben, falls mehrere Handler aufgerufen werden
		//(Speicherplatz kann freigegeben werden).
		document = null;
		tokenList = null;
		statistician = null;
		
		long timelength = System.currentTimeMillis() - starttime;
		String msg = "URL " + url + " wurde in " + timelength + "ms abgearbeitet (Modus: " + modename[mode] + ").";
		if (!quiet) System.out.println(msg);
		logger.info(msg);
	}
	
	/**
	 * Lädt ein Dokument in seiner Rohform
	 */
	private void loadRawDocument() throws TestelException {
		//private Dokumenten-Lade-Klasse instantiieren (s.u.)
		DocumentLoader loader = null;
		try {
			loader = new DocumentLoader(url, charset);
		} catch (Exception e) {
			throw new TestelException("Fehler beim Laden von " + url + ":\n" + TestEl.getStackTrace(e));
		}
		if (loader == null) throw new TestelException("Fehler beim Laden von " + url + ":\nDokumenten-Lader konnte nicht instantiiert werden");

		//Zeiger auf geladenes Dokument!
		this.document = loader.dldocument;
		
		//Ok, was ist mit dem Mime-Type
		if (mimetype == null) {
			mimetype = loader.dlmimetype;
			logger.info("Mimetype von " + url + " ist " + mimetype);
		} else { //Mimetype festgelegt
			if (!mimetype.equals(loader.dlmimetype)) //nur Warnung ausgeben
				logger.warning("Vom Benutzer festgelegter Mimetype von " + url + " (" + mimetype + ") stimmt nicht mit dem erkannten Mimetype " + loader.dlmimetype + " überein - könnte zu Problemen führen");
		}
	}
	
	/**
	 * Lade Factory-Instanzen
	 */
	private void loadFactories() throws TestelException {
		//Zuerst Sprache...
		String lang = Config.getConfig("lang");
		if (lang == null || null == "") throw new TestelConfigException("Konnte keine Sprachkonfiguration finden - ist die Property lang in der Property-Datei festgelegt?");
		
		langFactory = LangFactory.buildFactory(lang);
		if (langFactory == null) throw new TestelConfigException("Konnte keine Sprach-Fabrik für Sprache '" + lang + "' laden");
		
		//...dann Mime-Typ
		mimeFactory = MimeFactory.buildFactory(mimetype);
		if (mimeFactory == null) throw new TestelConfigException("Konnte keine Mimetyp-Fabrik für MimeTyp '" + mimetype + "' laden");
	}

	/**TaggerCollection für den angegebenen Mime-Typen abarbeiten
	 * @param coll
	 * @throws TestelException
	 */
	private void workCollection(TaggerCollection coll) throws TestelException {
		if (coll == null) return; //bei null ignorieren
		
		//Iterator holen...
		Iterator<Tagger> it = coll.iterator();
		
		//...und durchlaufen
		while (it.hasNext()) {
			Tagger t = it.next();
			try {
				tokenList = t.tag(tokenList);
			} catch(Exception e) {
				throw new TestelTaggerException("Fehler im " + t.getType() + " (Tagger):\n" + TestEl.getStackTrace(e));
			}
		}
	}
	
	/**
	 * Prüft die Ausgabedateien und stellt diese ggf. auf richtige Dateinamen.
	 * Wichtig ist das deshalb, weil designatedOutput und designatedOutputXML
	 * evt. auf Verzeichnissse gesetzt wurden. Außerdem werden in bestimmten Modi
	 * Dateinamen automatisch generiert.
	 */
	private void makeOutputFiles() throws TestelException {
		String filename;
		switch (mode) {
		case MODE_PRETRAIN:
			filename = System.currentTimeMillis() + getMimeFactory().getFileExtension();
			File pretrained;
			try {
				pretrained = new File(getMimeFactory().getMimeDir(), "pretrained");
			} catch (IOException e1) {
				throw new TestelException("Konnte Mime-Corpus-Verzeichnis nicht öffnen:\n" + TestEl.getStackTrace(e1));
			}
			designatedOutput = new File(pretrained, filename);
			break;
		default: //alle anderen Fälle, falls designatedOutput ein Verzeichnis ist
			if (designatedOutput != null && designatedOutput.isDirectory()) {
				try {
					filename = IOHelper.getFileName(url);
				} catch (MalformedURLException e) {
					//Fallback-Dateiname
					filename = System.currentTimeMillis() + getMimeFactory().getFileExtension();
					logger.warning("Konnte keinen Dateinamen von " + url + " ableiten");
				}
				designatedOutput = new File(designatedOutput, filename);
				//falls designatedOutputXML auch Directory ist
				if (designatedOutputXML != null && designatedOutputXML.isDirectory())
					designatedOutputXML = new File(designatedOutputXML, filename + ".xml");
			}
			break;
		}
	}
	
	/**Speichert die Daten als Dateien ab
	 * @param taggedDoc getaggte Datei - alle anderen Variablen sind Instanzvariablen
	 * @throws TestelException
	 */
	protected void saveDocs(String taggedDoc) throws TestelException {
		String gzip;
		boolean docgzip = false, xmlgzip = false;
		String saveDoc = null, xmlDoc = null;
		switch (mode) {
		case MODE_PRETRAIN: //PreTag-Modus
		case MODE_TRAIN: //Train-Modus
			if (mode == MODE_PRETRAIN)
				gzip = Config.getConfig("pretrained-gzip");
			else
				gzip = Config.getConfig("trained-gzip");
			if (gzip != null && gzip.equalsIgnoreCase("true")) docgzip = true;
			saveDoc = taggedDoc;
			break;
		case MODE_TAG: //Im Tag-Modus
			gzip = Config.getConfig("tag-gzip");
			if (gzip != null && gzip.equalsIgnoreCase("true")) docgzip = true;
			String gzipxml = Config.getConfig("xml-gzip");
			if (gzipxml != null && gzipxml.equalsIgnoreCase("true")) xmlgzip = true;
			if (inline) //Nur das Dokument samt Tags speichern
				saveDoc = taggedDoc;
			else { //Dokument samt XML speichern
				saveDoc = document;
				xmlDoc = taggedDoc;
			}
			break;
		case MODE_NORMALIZEONLY: //Nur-Normalisiermodus
			saveDoc = document;
			break;
		default:
			throw new TestelException("Unbekannter Modus beim Speichern der Daten");
		}
		
		//speichern
		if (!dryrun) {
			//Kann saveDoc überhaupt gespeichert werden?
			if (saveDoc == null) throw new TestelException("Kein Ausgabedokument zum Speichern spezifiziert");

			//Standard-Ausgabe
			if (designatedOutput == null) {
				//Doc schicken
				System.out.println(saveDoc);
				//evt. XML-Daten
				if (xmlDoc != null) System.out.println(xmlDoc);
				return;
			}
			
			//Datei: entweder gzip oder normal speichern
			try {
				if (docgzip) {
					designatedOutput = new File(designatedOutput.toString() + ".gz");
					IOHelper.stringtoGzipFile(saveDoc, designatedOutput);
				} else IOHelper.stringtoFile(saveDoc, designatedOutput);
			} catch (IOException e) {
				throw new TestelException("Konnte Daten '" + designatedOutput + "' nicht speichern:\n" + TestEl.getStackTrace(e));
			}
			String msg = "Daten als Datei '" + designatedOutput + "' gespeichert";
			logger.info(msg);
			if (!quiet) System.out.println(msg);
			//evt. XML-Datei speichern
			if (xmlDoc != null) {
				try {
					if (xmlgzip) {
						designatedOutput = new File(designatedOutputXML.toString() + ".gz");
						IOHelper.stringtoGzipFile(xmlDoc, designatedOutputXML);
					} else IOHelper.stringtoFile(xmlDoc, designatedOutputXML);
				} catch (IOException e) {
					throw new TestelException("Konnte XML-Datei '" + designatedOutputXML + "' nicht speichern:\n" + TestEl.getStackTrace(e));
				}
				msg = "XML-Tags als Datei '" + designatedOutputXML + "' gespeichert";
				logger.info(msg);
				if (!quiet) System.out.println(msg);				
			}
		} else {
			String msg = "Dryrun - keine Daten gespeichert";
			logger.info(msg);
			if (!quiet) System.out.println(msg);
		}
	}
	
	/**
	 * Speichert die TokenListe als TokenLog ab
	 */
	protected void doTokenLog() {
		if (tokenLog != null)
			try {
				IOHelper.stringtoFile(tokenList.toString(), tokenLog);
			} catch (IOException e) {
				logger.warning("Konnte Token-Log " + tokenLog + " nicht schreiben:\n" + TestEl.getStackTrace(e));
			}
	}

	/**private Dokumenten-Lade-Klasse
	 * @author mkalus
	 *
	 */
	protected class DocumentLoader {
		protected String dldocument;
		protected String dlmimetype;
		protected String charset;
		
		/** Konstruktor
		 * @param url
		 * @throws Exception
		 */
		public DocumentLoader(URL url, String charset) throws Exception {
			this.charset = charset;
			loadURL(url);
		}
		
		/**Lädt ein Dokument in dldocument und bestimmt den MimeType
		 * @param url
		 * @throws Exception
		 */
		private void loadURL(URL url) throws Exception {
			//Connection öffnen
			URLConnection connection;
			try {
				connection = IOHelper.getConnection(url);
			} catch (IOException e) {
				throw new Exception("Konnte keine Verbindung zu '" + url.toString() + "' aufbauen:\n" + TestEl.getStackTrace(e));
			}

			//Mime-Typ eruieren
			try {
				dlmimetype = parseMimeType(IOHelper.getType(connection));
			} catch (IOException e) {
				throw new Exception("Fehler bei der Mime-Typ-Erkennung von '" + url.toString() + "':\n" + TestEl.getStackTrace(e));
			}
			
			if (charset != null) //falls Charset manuell eingestellt wurde
				dldocument = IOHelper.streamtoString(connection.getInputStream(), charset);
			else dldocument = IOHelper.streamtoString(connection.getInputStream());
		}
		
		/**parst einen rohen Mime-Type zu einem verständlichen...
		 * @param type
		 * @return
		 */
		private String parseMimeType(String type) {
			//Bei null den Standardtypen zurückgeben
			if (type == null) {
				logger.warning("URL '" + url.toString() + "' wurde nicht erkannt (null-Wert). Verwende Standardtyp '" + Config.getConfig("defaultcontent") + "'");
				return Config.getConfig("defaultcontent");
			}
			
			//evt kürzen z.B. bei "text/html; encoding=UTF-8"
			String realtype;
			int pos = type.indexOf(";");
			if (pos >= 0) realtype = type.substring(0, pos);
			else realtype = type;
			
			//existiert ein solcher Typ?
			if (Config.getConfig("mimefactory_" + realtype) == null) {
				//nein, dann Standardtypen zurückgeben
				logger.warning("URL '" + url.toString() + "' wurde als unbekannter Mime-Typ '" + realtype + "' erkannt. Verwende Standardtyp '" + Config.getConfig("defaultcontent") + "'");
				return Config.getConfig("defaultcontent");
			}
			logger.fine("URL '" + url.toString() + "' wurde als Mime-Typ '" + realtype + "' erkannt");
			return realtype;
		}
	}
}

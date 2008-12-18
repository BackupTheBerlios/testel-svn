/**
 * Datei: TestEl.java
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.beimax.testel.config.Config;
import de.beimax.testel.exception.TestelConfigException;
import de.beimax.testel.exception.TestelException;
import de.beimax.testel.util.CmdLineParser;
import de.beimax.testel.util.IOHelper;

/**Hauptklasse von TestEl - diese Datei enthält auch die main-Methode.
 * 
 * Aufruf mit TestEl help, um die Hilfe anzuzeigen.
 * @author mkalus
 *
 */
public class TestEl {
	//Logger
	static final Logger logger = Logger.getLogger(TestEl.class.getName());

	/**Aktueller Status
	 */
	protected static int mode = 0; //Mode
	
	/**
	 * Eindatei/-verzeichnis
	 */
	protected static URL input = null;
	protected static boolean inputIsDirectory = false;
	protected static File output = null;
	protected static boolean outputIsDirectory = false;
	protected static File tag = null;
	protected static boolean tagIsDirectory = false;
	protected static String mimetype = null; //evtler. Mime-Typ
	protected static boolean inline = false;
	protected static File tokenLog = null; //Token-Log-Datei
	
	/**
	 * Dryrun, leiser Modus, überspringe Normalisierer, Charset/Encoding-Einstellung
	 */
	protected static boolean dryrun = false;
	protected static boolean quiet = false;
	protected static boolean skipNormalizer = false;
	protected static boolean fewertags = false;
	protected static String charset = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long starttime = System.currentTimeMillis();
		
		//Hilfe bei help-Parameter bzw. ohne Parameter
		if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
			printBanner(); //Banner drucken
			printHelp(); //Hilfe drucken
			System.exit(0);
		}
		
		//Kommandozeile bearbeiten
		try {
			parseCommands(args);
		} catch (TestelException e) {
			logger.severe("Beim Bearbeiten der Kommandozeile trat ein Fehler auf:\n" + e.getLocalizedMessage());
			System.err.println("Beim Bearbeiten der Kommandozeile trat ein Fehler auf:\n" + e.getLocalizedMessage());
			printHelp(); //Hilfe drucken
			System.exit(2);
		}
		
		//Banner ausgeben
		if (!quiet) printBanner(); //Banner drucken
		
		//Handlers holen
		LinkedList<TestElHandler> handlers = null;
		try {
			handlers = getHandlers();
		} catch (TestelException e) {
			logger.severe("Beim Initialisieren der Handler von TestEl trat ein Fehler auf:\n" + e.getLocalizedMessage());
			System.err.println("Beim Initialisieren der Handler von TestEl trat ein Fehler auf:\n" + e.getLocalizedMessage());
			System.exit(1); //mit Fehler beenden
		}
		
		//Handler der Reihe nach Aufrufen
		Iterator<TestElHandler> it = handlers.iterator();
		TestElHandler h = null;
		int errorcount = 0; //Fehlerzähler
		while (it.hasNext()) {
			try {
				h = it.next();
				h.handle();
			} catch (Exception e) {
				errorcount++; //Fehlerzähler um eins inkrementieren
				String url;
				if (h == null) url = "[unbekannt]";
				else url = h.getURL().toString();
				String msg = e.getLocalizedMessage();
				if (msg == null) { //Message in String
					StringWriter w = new StringWriter();
					e.printStackTrace(new PrintWriter(w));
					msg = w.toString();
				}
				logger.severe("Beim Bearbeiten von " + url + " trat ein Fehler auf:\n" + msg);
				if (!quiet)
					System.err.println("Beim Bearbeiten von " + url + " trat ein Fehler auf:\n" + msg);
			} //Handle aufrufen
		}
		
		//ggf. Erfolgsmeldung am Ende
		String endmess;
		if (handlers.size() == 0) endmess = "Keine Dateien zum Bearbeiten gefunden!"; 
		else if (errorcount == 0) endmess = "TestEl wurde erfolgreich beendet!";
		else if (errorcount == 1)  endmess = "TestEl wurde mit 1 fehlerhaft bearbeitetem Dokument beendet!";
		else endmess = "TestEl wurde mit " + errorcount + " fehlerhaft bearbeiteten Dokumenten beendet!";
		logger.info(endmess);
		if (!quiet) {
			long timelength = System.currentTimeMillis() - starttime;
			System.out.println("Gesamte Bearbeitungszeit: " + timelength + "ms");
			System.out.flush();
			System.out.println(endmess);
			System.out.flush();
		}
		if (errorcount != 0) System.exit(3); //Fehler aufgetreten		
	}

	/**
	 * Druckt den Banner.
	 */
	public static void printBanner() {
		System.out.flush();
		System.out.println("TestEl Version 1.0\nCopyright 2008 Maximilian Kalus");
		System.out.flush();
	}

	/**
	 * Druckt die Kommando-Hilfe aus.
	 */
	public static void printHelp() {
		System.out.println("Verwendung: TestEl --switches|help\n" +
				"  help                     : Druckt diese Hilfe aus\n" +
				"  --mode,-m [mode]         : Selektiert einen Modus aus\n" +
				"       pretrain            : Pretrain-Modus (auch --pretrain)\n" + 
				"       train               : Train-Modus (auch --train)\n" + 
				"       tag                 : Tag-Modus (auch --tag)\n" + 
				"       normalize-only      : Nur-Normalisieren-Modus (auch --normalize-only)\n" + 
				"  --input,-i [uri]         : Eingabedaten\n" +
                "                             (URL, Datei, Verzeichnis, nicht im Train-Modus)\n" +
                "  --output,-o [uri]        : Ausgabedaten\n" +
                "                             (Datei oder Verzeichnis, nur im Tag-Modus und\n" +
                "                              normalize-only-Modus - fehlt der Operator wird\n" +
                "                              die Ausgabe zur Standardausgabe geschickt)\n" +
                "  --tags,-x [file]         : Ausgabe der Tagfiles (sonst --inline-Option)\n" +
                "                             (Datei oder Verzeichnis, nur im Tag-Modus)\n" +
                "  --inline                 : Inline-Modus (setzt TestEl-Tags in die Datei)\n" +
                "                             (nur im Tag-Modus, sonst --tags-Option)\n" +
                "  --mime-type,-t [mode]    : Setze Mime-Type (im Train-Modus, sonst Default!)\n" +
                "                             (PreTrain/Tag-Modus: erzwinge Mime-Typ)\n" +
                "  --encoding,-e [encoding] : Eingabe-Datei liegt in diesem Code-Format vor\n" +
                "                             (PreTrain/Tag-Modus, sonst Standard-Encoding)" +
                "  --skip-normalizer        : Überspringe Normalisierer\n" +
                "                             (nur im Pretrain- & Tag-Modus)\n" +
                "  --fewer-tags             : Nur match- und ref-Tags anzeigen (nur PreTrain)\n" +
				"  --config,-c [file]       : Properties-Datei (Standard: testel.properties)\n" +
				"  --logging-properties [f] : Properties-Datei für Logger\n" +
				"  --loglevel,-l [level]    : Loglevel\n" +
				"                             (SEVERE,WARNING,INFO,CONFIG,FINE,FINER,FINEST,ALL)\n" +
				"  --dryrun                 : Speichere/Verändere keine Daten!\n" +
				"  --tokenlog [file]        : Nach jedem Arbeitsschritt wird die TokenListe in\n" +
				"                             dieser Datei gespeichert - gut zum Prüfen von\n" +
				"                             Fehlern/Problemen!\n" +
				"  --quiet                  : Möglichst keine Ausgaben (Log ist extra!)\n" +
				"  [property]=[val]       : Setzt eine Property der Config auf Wert [val]\n" +
				"  z.B.\n" +
				"  Lobo-Sleep=[int]       : Einstellung wie Property Lobo-Sleep");
		System.out.flush(); //Damit Hilfe auf jeden Fall vor Fehlern angezeigt wird
	}
	
	/**Kommandozeilen-Parser - benötigt die Datei CmdLineParser.java aus dem util-Paket
	 * @param args
	 * @throws TestelException
	 */
	public static void parseCommands(String[] args) throws TestelException {
		//Kommandozeilenoptionen
		CmdLineParser parser = new CmdLineParser();
		CmdLineParser.Option modeO = parser.addStringOption('m', "mode");
		CmdLineParser.Option preTrainO = parser.addBooleanOption("pretrain");
		CmdLineParser.Option trainO = parser.addBooleanOption("train");
		CmdLineParser.Option tagO = parser.addBooleanOption("tag");
		CmdLineParser.Option normalizeOnlyO = parser.addBooleanOption("normalize-only");
		CmdLineParser.Option loggingO = parser.addStringOption('l', "loglevel");
		CmdLineParser.Option logpropO = parser.addStringOption("logging-properties");
		CmdLineParser.Option inputO = parser.addStringOption('i', "input");
		CmdLineParser.Option outputO = parser.addStringOption('o', "output");
		CmdLineParser.Option tagsO = parser.addStringOption('x', "tags");
		CmdLineParser.Option inlineO = parser.addBooleanOption("inline");
		CmdLineParser.Option typeO = parser.addStringOption('t', "mime-type");
		CmdLineParser.Option skipnormalizerO = parser.addBooleanOption("skip-normalizer");
		CmdLineParser.Option configO = parser.addStringOption('c', "config");
		CmdLineParser.Option dryrunO = parser.addBooleanOption("dry-run");
		CmdLineParser.Option quietO = parser.addBooleanOption("quiet");
		CmdLineParser.Option fewerTagsO = parser.addBooleanOption("fewer-tags");
		CmdLineParser.Option tokenLogO = parser.addStringOption("tokenlog");
		CmdLineParser.Option encodingO = parser.addStringOption('e', "encoding");

		//Kommandozeile abarbeiten
		try {
			parser.parse(args);

			//Quiet und Dryrun behandeln
			doQuietDryMime((Boolean) parser.getOptionValue(quietO),
					(Boolean) parser.getOptionValue(dryrunO),
					(String) parser.getOptionValue(typeO),
					(Boolean) parser.getOptionValue(fewerTagsO));
			
			//Modus erkennen
			doMode((String) parser.getOptionValue(modeO), modeO,
					(Boolean) parser.getOptionValue(preTrainO),
					(Boolean) parser.getOptionValue(trainO),
					(Boolean) parser.getOptionValue(tagO),
					(Boolean) parser.getOptionValue(normalizeOnlyO));
	
			//Logging
			doLoggingProperties((String) parser.getOptionValue(logpropO));
			doSetLogLevel((String) parser.getOptionValue(loggingO));
			doSetTokenLog((String) parser.getOptionValue(tokenLogO));
			
			//Konfiguration von TestEl
			doConfig((String) parser.getOptionValue(configO),
					parser.getRemainingArgs());
			
			//Input, Output, Mime-Type-Einstellungen, etc.
			doInputOutput((String) parser.getOptionValue(inputO),
					(String) parser.getOptionValue(outputO),
					(String) parser.getOptionValue(tagsO),
					(Boolean) parser.getOptionValue(inlineO),
					(Boolean) parser.getOptionValue(skipnormalizerO));
			//Encoding
			doEncoding((String) parser.getOptionValue(encodingO));
			
			//Parameter-Check: sind die Parameter für einen Modus alle vorhanden?
			doModeParamCheck();
		} catch (Exception e) {
			throw new TestelException(e.getLocalizedMessage());
		}
	}
	
	/**Quiet Dryrun und Mime behandeln
	 * @param quietly
	 * @param dryly
	 * @param mime
	 */
	private static void doQuietDryMime(Boolean quietly, Boolean dryly, String mime, Boolean dofewertags) {
		if (dryly != null && dryly == true) dryrun = true;
		else dryrun = false;
		
		if (quietly != null && quietly == true) quiet = true;
		else quiet = false;
		
		if (dofewertags != null && dofewertags == true) fewertags = true;
		else fewertags = false;
		
		//Input-Optionen und Mime-Type einstellen
		mimetype = mime;
	}

	/**Prüft, ob der Modus in der Kommandozeile richtig übergeben worden ist
	 * @param modestr String der per --mode übergeben wurde
	 * @param modeO Optionsobjekt von CmdLineParser
	 * @param pretrain Wurde --pretrain angegeben?
	 * @param train Wurde --train angegeben?
	 * @param tag Wurde --tag angegeben?
	 * @param normalizeOnly nur Normalisieren
	 * @throws Exception falls falscher oder kein Modus angegeben wurde
	 */
	private static void doMode(String modestr, CmdLineParser.Option modeO, Boolean pretrain, Boolean train, Boolean tag, Boolean normalizeOnly) throws Exception {
		//So, jetzt der Reihe nach vorgehen und Fehler abfangen
		//String-Modus
		if (modestr != null) {
			if (modestr.equalsIgnoreCase("pretrain")) mode = TestElHandler.MODE_PRETRAIN;
			else if(modestr.equalsIgnoreCase("train")) mode = TestElHandler.MODE_TRAIN;
			else if(modestr.equalsIgnoreCase("tag")) mode = TestElHandler.MODE_TAG;
			else if(modestr.equalsIgnoreCase("normalize-only")) mode = TestElHandler.MODE_NORMALIZEONLY;
			else throw new CmdLineParser.IllegalOptionValueException(modeO, modestr);
		}
		
		//Boolsche Optionen
		if (pretrain != null && pretrain == true) {
			if (mode != 0 && mode != TestElHandler.MODE_PRETRAIN) throw new TestelConfigException("Modus kann nicht zweimal definiert werden");
			else mode = TestElHandler.MODE_PRETRAIN;
		}
		
		if (train != null && train == true) {
			if (mode != 0 && mode != TestElHandler.MODE_TRAIN) throw new TestelConfigException("Modus kann nicht zweimal definiert werden");
			else mode = TestElHandler.MODE_TRAIN;
		}
		
		if (tag != null && tag == true) {
			if (mode != 0 && mode != TestElHandler.MODE_TAG) throw new TestelConfigException("Modus kann nicht zweimal definiert werden");
			else mode = TestElHandler.MODE_TAG;
		}
		
		if (normalizeOnly != null && normalizeOnly == true) {
			if (mode != 0 && mode != TestElHandler.MODE_NORMALIZEONLY) throw new TestelConfigException("Modus kann nicht zweimal definiert werden");
			else mode = TestElHandler.MODE_NORMALIZEONLY;
		}
		
		//Kein Modus definiert: Fehler!
		if (mode == 0) throw new TestelConfigException("Es wurde kein Modus definiert!");
	}

	/**Lädt die logging.properties-Datei, bei 
	 * @param propfile Angabe, wo die Properties liegen
	 */
	private static void doLoggingProperties(String propfile) throws Exception {
		if (propfile == null) {//leer -> Standard-Properties Datei laden
			propfile = "logging.properties";
			File test = new File(propfile);
			if (!test.exists()) return; //falls es nicht existiert, zurück
		}
		
		File file = new File(propfile);
		
		if (!file.exists()) throw new TestelConfigException("Logging-Property-Datei '" + propfile + "' existiert nicht");
		if (file.isDirectory()) throw new TestelConfigException("Logging-Property-Datei '" + propfile + "' ist ein Verzeichnis, keine Datei");
		if (!file.canRead()) throw new TestelConfigException("Logging-Property-Datei '" + propfile + "' kann nicht gelesen werden (stimmen die Berechtigungen?)");
		
		//Ok, hole Log-Manager
		LogManager mg = LogManager.getLogManager();
		
		FileInputStream is = new FileInputStream(file);
		mg.readConfiguration(is);
		is.close();
		logger.finest("Logging-Datei wurde eingelesen");
	}
	
	/**Bearbeitet Loglevel, falls -l/--loglevel eingestellt wurde
	 * @param loglevel
	 * @throws Exception
	 */
	private static void doSetLogLevel(String loglevel) throws Exception {
		if (loglevel == null) return; //leer
		
		String[] lvlsstr = {"SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST", "ALL"};
		Level[] lvls = {Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.ALL};
		Level found = null;
		
		//Suche
		for (int i = 0; i < lvls.length; i++)
			if (loglevel.equalsIgnoreCase(lvlsstr[i])) found = lvls[i];
		
		//nicht korrekt?
		if (found == null) throw new TestelConfigException("Unbekannter Log-Level '" + loglevel + "'");
		
		//Root-Logger einstellen
		Logger.getLogger("").setLevel(found);
		logger.finest("Logging-Level wurde auf " + loglevel + " eingestellt");
	}
	
	private static void doSetTokenLog(String tokenLog) throws Exception {
		if (tokenLog == null) return;
		//Tests
		try {
			File path = new File(IOHelper.getPath(tokenLog));
			if (!path.isDirectory() || !path.canWrite()) throw new TestelException("Konnte Pfad " + path + " zum Speichern der Token-Log-Datei nicht finden oder nicht beschreibbar");
		} catch (MalformedURLException e) {
			logger.warning("Token-Log-Pfad " + tokenLog + " konnte nicht in URL umgewandelt werden");
			return;
		}
		
		TestEl.tokenLog = new File(tokenLog);
	}
	
	/**Konfigurationsdatei und Lobosleep-Wert
	 * @param configfile
	 * @param lobosleep
	 * @throws Exception
	 */
	private static void doConfig(String configfile, String[] args) throws Exception {
		//Konfigurationsdatei initialisieren
		Config.init(configfile);
		//weitere Properties müssen key=val sein!
		if (args == null) return;
		for (int i = 0; i < args.length; i++) {
			int pos = args[i].indexOf('=');
			if (pos <= 0 || pos == args.length-1) throw new TestelConfigException("Unbekannter Parameter '" + args[i] + "' (Property-Parameter müssen dem Schema key=val entsprechend und keine Leerzeichen beim = enthalten!)");
			//Config-Wert setzen
			Config.setConfig(args[i].substring(0, pos), args[i].substring(pos+1));
		}
	}
	
	/**Prüft die diversen modusabhängigen Parameter
	 * @param input
	 * @param output
	 * @param tags
	 * @param inline
	 * @param skipNorm
	 */
	private static void doInputOutput(String infile, String outfile,
			String tagfile, Boolean inlinef, Boolean skipNorm) throws Exception {
		//Input
		if (infile != null) {
			input = IOHelper.createURL(infile);
			if (IOHelper.isFile(input)) {
				File test = new File(input.getPath());
				if (!test.exists())
					throw new TestelConfigException("Eingabe-Datei/-Verzeichnis '"
							+ infile + "' existiert nicht");
				if ((inputIsDirectory = test.isDirectory()) == true) {
					IOHelper.checkDir(input.getPath(), false);
				}
			}
		}
		
		//Output
		if (outfile != null) {
			output = new File(outfile);
			if ((outputIsDirectory = output.isDirectory()) == true) {
				IOHelper.checkDir(output.getPath(), false);
			}
		}
		
		//Tagfile
		if (tagfile != null) {
			tag = new File(tagfile);
			if ((tagIsDirectory = tag.isDirectory()) == true) {
				IOHelper.checkDir(tag.getPath(), false);
			}
		}
		
		//Inline & Skip-Normalizer
		if (inlinef != null) inline = inlinef;
		if (skipNorm != null) skipNormalizer = skipNorm;
	}

	/**Überprüfe Parameter für die Modi und wirf bei Fehlern eine Ausnahme
	 * @throws TestelConfigException
	 */
	private static void doModeParamCheck() throws TestelConfigException {
		switch (mode) {
		case TestElHandler.MODE_TAG:
			//Im Pretraining und Tagging wird die Input-Option erwartet, das ist
			// bei der Train-Option nicht so, da hier ja die entsprechenden Daten
			//schon in den Ordnern liegen
			if (input == null) throw new TestelConfigException("Option --input fehlt");
			//Im Tag-Modus muss ein Ausgabe-Verzeichnis/Datei existieren
			if (output == null && !inline)
				throw new TestelConfigException("Option --output fehlt (bei externer Tagdatei muss dieser Parameter angegeben werden)");
			if (output == null) //throw new TestelConfigException("Option --output fehlt");
				quiet = true; //wird automatisch angenommen
			//Eingbae ist Verzeichnis und Ausgabe ist Datei?
			if (inputIsDirectory && !outputIsDirectory)
				throw new TestelConfigException("Wenn --input ein Verzeichnis ist, muss --output das auch sein");
			if (inputIsDirectory && !tagIsDirectory && !inline)
				throw new TestelConfigException("Wenn --input ein Verzeichnis ist, muss --tag das auch sein");
			if (inline && tag != null)
				throw new TestelConfigException("Optionen --tags und --inline dürfen nicht gemeinsam vorkommen");
			if (!inline && tag == null)
				throw new TestelConfigException("Es muss eine der beiden Optionen --tags und --inline gesetzt sein");
			break;
		case TestElHandler.MODE_PRETRAIN:
			//Im Pretraining und Tagging wird die Input-Option erwartet, das ist
			// bei der Train-Option nicht so, da hier ja die entsprechenden Daten
			//schon in den Ordnern liegen
			if (input == null) throw new TestelConfigException("Option --input fehlt");
			//Überflüssige Optionen beanstanden
			if (output != null) throw new TestelConfigException("Überfüssige Option --output");
			if (tag != null) throw new TestelConfigException("Überfüssige Option --tags");
			//Da wir den Mime-Typ noch nicht kennen, kann hier die Ausgabe-Datei noch nicht
			//festgelegt werden - das mit dann der Handler machen
			inline = true; //immer Inline-Dokumente produzieren
			break;
		case TestElHandler.MODE_TRAIN:
			//Überflüssige Optionen beanstanden
			if (output != null) throw new TestelConfigException("Überfüssige Option --output");
			if (tag != null) throw new TestelConfigException("Überfüssige Option --tags");

			//Falls nicht gesetzt, dann wird Default-Content genommen.
			if (mimetype == null) mimetype = Config.getConfig("defaultcontent");
			//Jetzt Konfiguration für Mime-Type holen
			String dir = Config.getConfig("directory_" + mimetype);
			try {
				IOHelper.checkDir(dir, false);
			} catch (IOException e) {
				throw new TestelConfigException("Konnte Corpus-Verzeichnis '" + dir + "' nicht finden bzw. Verzeichnis ist nicht beschreibbar");
			}
			
			//Aus welchen Verzeichnissen kommen die Daten?
			File inputdir = new File(dir, "pretrained");
			try {
				IOHelper.checkDir(inputdir.toString(), false);
			} catch (IOException e) {
				throw new TestelConfigException("Konnte Pretrained-Verzeichnis '" + inputdir + "' nicht finden bzw. Verzeichnis ist nicht beschreibbar");
			}
			
			//Ok, nun sollte hier alles klar sein, dann Input setzen
			try {
				input = IOHelper.createURL(inputdir.toString());
			} catch (MalformedURLException e) {
				throw new TestelConfigException("Pretrained-Verzeichnis '" + inputdir + "' nicht in URL umgewandelt werden");
			}
			inputIsDirectory = true; //klar, wir wollen ja alle Dateien aus dem Verzeichnis

			//Output setzen
			output = new File(dir, "trained");
			try {
				IOHelper.checkDir(output.toString(), false);
			} catch (IOException e) {
				throw new TestelConfigException("Konnte Trained-Verzeichnis '" + output + "' nicht finden bzw. Verzeichnis ist nicht beschreibbar");
			}
			outputIsDirectory = true; //klar, wir wollen ja alle Dateien aus dem Verzeichnis
			skipNormalizer = true; //wird in diesem Modus immer übersprungen
			inline = true; //immer Inline-Dokumente produzieren
			break;
		case TestElHandler.MODE_NORMALIZEONLY:
			//Überflüssige Optionen beanstanden
			if (tag != null) throw new TestelConfigException("Überfüssige Option --tags");
			if (input == null) throw new TestelConfigException("Option --input fehlt");
			if (output == null) //throw new TestelConfigException("Option --output fehlt");
				quiet = true; //wird automatisch angenommen
			break;
		default: //sollte eigentlich nicht vorkommen...
			throw new TestelConfigException("Unbekannter Modus!");
		}
	}
	
	/**Manuelles Einstellen des Encodings der Eingabedatei
	 * @param encoding
	 * @throws TestelConfigException
	 */
	public static void doEncoding(String encoding) throws TestelConfigException {
		
		if (mode == TestElHandler.MODE_TRAIN) {
			if (encoding != null)
				throw new TestelConfigException("Überfüssige Option --encoding");
			encoding = "utf-8"; //im Trainingsmodus wird utf-8 erzwungen...
		}
			
		if (encoding == null) return; //automatische Einstellung...

		//gibt es das Charset?
		try {
			Charset.forName(encoding);
		} catch (IllegalCharsetNameException e) {
			throw new TestelConfigException("Unbekanntes Codeformat " + encoding);
		} catch (UnsupportedCharsetException e) {
			throw new TestelConfigException("Codeformat " + encoding + " wird von dieser Java-Plattform nicht unterstützt");
		}
		charset = encoding;
		logger.info("Eingabeformat ist " + encoding);
	}

	/**Holt sich die Liste der Handlers - ein Handler pro Datei
	 * @return
	 * @throws TestelException
	 */
	public static LinkedList<TestElHandler> getHandlers() throws TestelException {
		LinkedList<TestElHandler> list = new LinkedList<TestElHandler>();
		
		//Checken, ob Eingabe eine Liste ist
		if (inputIsDirectory) {
			//Hole Verzeichnis
			File dir = null;
			try {
				dir = new File(IOHelper.getPath(input));
			} catch (MalformedURLException e) {
				throw new TestelException("Konnte " + input + " nicht in ein Verzeichnis umwandeln");
			}
			
			//Inhalt des Verzeichnisses alphabetisch holen
			File[] files = IOHelper.getSortedFileList(dir);
			
			for (int i = 0; i < files.length; i++) {
				//nur lesbare Dateien hinzufügen
				if (!files[i].isFile()) continue;
				if (!files[i].canRead()) {
					logger.warning("Kann Datei " + files[i] + " nicht lesen - überspringe");
					continue;
				}
				//in URL umwandeln
				URL url;
				try {
					url = IOHelper.createURL(files[i].toString());
				} catch (MalformedURLException e) {
					logger.warning("Kann Datei " + files[i] + " nicht in einen URL übersetzen - überspringe");
					continue;
				}
				//zur Liste hinzufügen
				list.add(getSingleHandler(url));
			}

		} else list.add(getSingleHandler(input)); //einzelEintrag
		
		return list;
	}
	
	/**Gibt einen einzelnen Handler zurück
	 * @return
	 * @throws TestelException
	 */
	private static TestElHandler getSingleHandler(URL url) throws TestelException {
		//neuen Handler erstellen
		TestElHandler handler = new TestElHandler(mode, url, output, quiet, dryrun, fewertags);
		
		//Verschiedene Switches einstellen
		if (inline) handler.activateInline();
		else if (tag != null) handler.setXMLOutput(tag);
		if (mimetype != null) handler.setMimeType(mimetype);
		if (tokenLog != null) handler.setTokenLog(tokenLog);
		if (charset != null) handler.setEncoding(charset);
		if (skipNormalizer) handler.deactivateNormalizer();
		else handler.activateNormalizer();
		
		return handler;
	}
	
	/**Holt sich einen StackTrace als String
	 * @param e
	 * @return
	 */
	public static String getStackTrace(Exception e) {
		StringWriter w = new StringWriter();
		e.printStackTrace(new PrintWriter(w));
		return w.toString();
	}
}

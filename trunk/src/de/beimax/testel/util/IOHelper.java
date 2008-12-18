/**
 * Datei: URIHelper.java
 * Paket: de.beimax.testel.util
 * Projekt: TestEl
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
package de.beimax.testel.util;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

//import com.ibm.icu.text.CharsetDetector;
//hat leider nicht so viel gebracht...

/** Hilfsmethoden für URI/URL-Bearbeitung
 * @author mkalus
 *
 */
public class IOHelper {
	private static final Logger logger = Logger.getLogger(IOHelper.class.getName());

	/**Erstelle valide URL aus URI-String
	 * @param uri
	 * @return
	 */
	static public URL createURL(String uri) throws MalformedURLException {
		URI normalized;
		URL url;
		
		//zuerst in URI umwandeln und normalisieren
		try {
			normalized = new URI(uri);
		} catch (URISyntaxException e) {
			logger.warning("Konnte " + uri + " nicht in URI verwandeln.");
			throw new MalformedURLException();
		}
		normalized = normalized.normalize();
		
		//Umwandeln in URL
		try {
			url = new URL(normalized.toString());
		} catch(MalformedURLException mfu) {
			int idx = uri.indexOf(':');
			if(idx == -1 || idx == 1) {
				// try file
				url = new URL("file:" + normalized.toString());
			}
			else {
				logger.warning("Konnte " + uri + " nicht in URL verwandeln.");
				throw mfu;
			}
		}
		
		logger.finer(uri + " umgewandelt in " + url.toString() + ".");
		return url;
	}
	
	/**Gibt den Pfadteil einer URL zurück
	 * @param url
	 * @return
	 * @throws MalformedURLException
	 */
	static public String getPath(URL url) throws MalformedURLException {
		String protocol = url.getProtocol();
		String path = url.getPath();
		String out = "";
		
		//im Falle von Dateien
		if (protocol.equals("file")) {
			File tester = new File(path);
			if (tester.isDirectory()) out = path;
			else out = path.substring(0, path.lastIndexOf(File.separator)+1);
		} else if (protocol.equals("http")) {
			out = url.toExternalForm();
			out = out.substring(0, out.lastIndexOf("/")+1);
		} else {
			logger.warning("Unbekanntes Protokoll " + protocol + " in URL " + url.toString() + ".");
			throw new MalformedURLException();
		}
//		TODO: FTP?
		
		logger.finer("Pfad von " + url.toString() + " ist " + out + ".");
		return out;
	}
	
	/**Gibt true zurück, falls URL HTTP-Aufruf ausdrückt
	 * @param url
	 * @return
	 */
	static public boolean isHTTP(URL url) {
		if (url.getProtocol().equalsIgnoreCase("http")) return true;
		return false;
	}
	
	/**Gibt true zurück, falls URL eine Datei oder Verzeichnis bezeichnet
	 * @param url
	 * @return
	 */
	static public boolean isFile(URL url) {
		if (url.getProtocol().equalsIgnoreCase("file")) return true;
		return false;
	}
	
	/**Gibt Pfadanteil zurück
	 * @param file
	 * @return
	 * @throws MalformedURLException
	 */
	static public String getPath(String file) throws MalformedURLException {
		return getPath(createURL(file));
	}
	
	/**Gibt Dateinamen-Anteil zurück
	 * @param url
	 * @return
	 * @throws MalformedURLException
	 */
	static public String getFileName(URL url) throws MalformedURLException {
		String protocol = url.getProtocol();
		String path = url.getPath();
		String out = "";
		
		//im Falle von Dateien
		if (protocol.equals("file")) {
			out = path.substring(path.lastIndexOf(File.separator)+1);
		} else if (protocol.equals("http")) {
			out = url.toExternalForm();
			out = out.substring(out.lastIndexOf("/")+1);
		} else {
			logger.warning("Unbekanntes Protokoll " + protocol + " in URL " + url.toString() + ".");
			throw new MalformedURLException();
		}
//		TODO: FTP?

		logger.finer("Dateiname von " + url.toString() + " ist " + out + ".");
		return out;
	}
	
	/**Einfache Methode, um absolute Pfade zu erkennen.
	 * @param url
	 * @return
	 */
	static public boolean isAbsolute(URL url) {
//		TODO: nicht besonders raffiniert, kann leicht was falsches sagen...
//		TODO: Windows-Systeme abchecken...
		String protocol = url.getProtocol();
		
		if (protocol.equals("file")) {
			if (url.getPath().charAt(0) == File.separatorChar) return true;
		} else if (protocol.equals("http")) {
			return true;
		}
		
		return false;
	}
	
	/**Einfache Methode, um absolute Pfade zu erkennen
	 * @param url
	 * @return
	 */
	static public boolean isAbsolute(String url) {
		URL myurl;
		try {
			myurl = createURL(url);
		} catch (MalformedURLException e) {
			return false; //naja, nicht schön, aber eine fehlerhafte Angabe ist wahrscheinlich auch nicht absolut, oder?
		}
		return isAbsolute(myurl);
	}
	
	/**Lädt eine URL (HTTP oder file) in einen Stream - Redirects werden verfolgt, d.h.
	 * das neue Dokument wird dann geladen.
	 * @param url
	 * @param ioType Enthält entweder null oder den Mime-Typen, den das Request zurückgibt
	 * @param ioEncoding enhält entweder Encoding oder null
	 * @return
	 * @throws IOException
	 */
	public static InputStream loadURL(URL url) throws IOException {
		//Laden der URL
		URLConnection connection = getConnection(url);
		InputStream in = connection.getInputStream();

		return in;
	}
	
	/**Erstellt eine Connection
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static URLConnection getConnection(URL url) throws IOException {
		//Laden der URL
		logger.info("Lade URI=[" + url.toString() + "].");
		URLConnection connection = url.openConnection();
		connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible;) Testel");
		connection.setRequestProperty("Cookie", "");
		if(connection instanceof HttpURLConnection) {
			HttpURLConnection hc = (HttpURLConnection) connection;
			hc.setInstanceFollowRedirects(true);
			int responseCode = hc.getResponseCode();
			logger.finer("HTTP Antwortcode: " + responseCode);
		}
		
		return connection;
	}
	
	/**Holt von einer offenen Connection den Mime-Type
	 * @param url
	 * @param connection
	 * @return
	 */
	public static String getType(URLConnection connection) throws IOException {
		return connection.getContentType();
	}
	
	/**Speichert einen Stream in eine Datei ab. Der Eingabestrom wird nach dem Auslesen
	 * außerdem geschlossen!
	 * @param inStream EingabeStrom
	 * @param file Ausgabedatei
	 * @throws IOException
	 */
	public static void saveStream(InputStream inStream, File file)
			throws IOException {
		//in String umwandeln, damit Encoding gerichtet wird
		String doc = streamtoString(inStream);
		stringtoFile(doc, file);
	}
	
	/**
	 * Wandelt einen Eingabestrom in einen String um. Es wird das Standard-Encodig
	 * eingesetzt.
	 * 
	 * @param in
	 *            Eingabestrom, sollte Marks unterstützen, wenn möglich.
	 * @return String-Repräsentation der Datei
	 * @throws IOException
	 */
	public static String streamtoString(InputStream in) throws IOException {
		//Inputstream in StringBuilder-Buffer laden und zurückgeben
		String string;
		StringBuilder outputBuilder = new StringBuilder();
		if (in != null) {
			BufferedReader reader;
			
//			if (in.markSupported()) { //falls Character Set erkannt werden kann
//				//automatische Encoding-Erkennung...
//				CharsetDetector detector = new CharsetDetector();
//
//				Reader r = detector.getReader(in, null);
//				reader = new BufferedReader(r);
//				logger.fine("Stream wird ausgelesen - Encoding automatisch erkannt (" + detector.detect().getName() + ").");
//			} //falls nicht:
//			else {
				reader = new BufferedReader(new InputStreamReader(in));
//					logger.fine("Stream wird ausgelesen - Standardencoding wird verwendet (Stream gab markSupported = false aus).");
//			}
			while (null != (string = reader.readLine())) {
				outputBuilder.append(string).append('\n');
			}
		}
		//System.out.println(outputBuilder.toString());

		return outputBuilder.toString();
	}

	/**
	 * Wandelt einen Eingabestrom in einen String um. Das charset des Eingabestroms ist
	 * festgelegt.
	 * 
	 * @param in  Eingabestrom, muss keine Marks unterstützen.
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	public static String streamtoString(InputStream in, String charset) throws IOException {
		String string;
		StringBuilder outputBuilder = new StringBuilder();

		if (in != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
			logger.fine("Stream wird ausgelesen - Encoding " + charset + " wird verwendet");
			while (null != (string = reader.readLine())) {
				outputBuilder.append(string).append('\n');
			}
		}
		
		return outputBuilder.toString();
	}
	
    /**Lese File als String ein
     * @param filePath
     * @return
     * @throws java.io.IOException
     */
    public static String filetoString(File file)
			throws java.io.IOException {
    	StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			fileData.append(buf, 0, numRead);
		}
		reader.close();
		logger.info("Datei " + file + " gelesen.");
		return fileData.toString();
	}
    
    /**Lese gziped File als String ein
     * @param file
     * @return
     * @throws java.io.IOException
     */
    public static String gzipFiletoString(File file) throws java.io.IOException {
		InputStream in = new GZIPInputStream(new FileInputStream(file));
		
		//Inputstream in StringBuilder-Buffer laden und zurückgeben
		String string;
		StringBuilder outputBuilder = new StringBuilder();
		if (in != null) {
			BufferedReader reader;
			
			reader = new BufferedReader(new InputStreamReader(in));
			logger.info("GZiped Stream wird ausgelesen.");
			while (null != (string = reader.readLine())) {
				outputBuilder.append(string).append('\n');
			}
		}
		in.close();

		return outputBuilder.toString();
    }

    /**Speichert einen String als Datei ab
     * @param str
     * @param file
     * @throws java.io.IOException
     */
    public static void stringtoFile(String str, File file) throws java.io.IOException {
    	FileWriter writer = new FileWriter(file, false);
		writer.write(str);
		writer.close();
    }
    
    /**Speichert einen String als gziped Datei ab
     * @param str
     * @param file
     * @throws java.io.IOException
     */
    public static void stringtoGzipFile(String str, File file) throws java.io.IOException {
		final int BLOCKSIZE = 8192;
		byte[] buffer = new byte[ BLOCKSIZE ]; 
		
		OutputStream gzos = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(file, false)));
		ByteArrayInputStream is = new ByteArrayInputStream(str.getBytes());
		
		for ( int length; (length = is.read(buffer, 0, BLOCKSIZE)) != -1; ) {
			//System.out.print(length);
	        gzos.write( buffer, 0, length );
		}
		is.close();
		gzos.close();   	
    }
    
	/**
	 * Gibt true zurück, falls doc als HTML-Dokument identifiziert werden kann.
	 * 
	 * @param doc
	 *            Eingangsdokument, kann auch der Anfang sein (erste Zeile)
	 * @return true, falls doc als HTML-Dokument identifiziert werden kann.
	 */
	public static boolean HTMLcheck(String doc) {
		if (doc == null) return false;
		if (doc.substring(0, 5).equals("<html")) return true;
		if (doc.substring(0, 5).equals("<HTML")) return true;
		if (doc.substring(0, 14).equals("<!DOCTYPE HTML")) return true;
		return false;
	}
	
	/**Gibt true zurück, falls Verzeichnis existiert und beschreibbar ist, ansonsten Exception.
	 * @param dir
	 * @param mustWrite true, falls man schreiben will, false sonse
	 * @return
	 * @throws IOException
	 */
	public static boolean checkDir(String dir, boolean mustWrite) throws IOException {
		File file = new File(dir);
		if (!file.exists()) throw new IOException("Konnte Verzeichnis " + dir + " nicht finden!");
		if (!file.isDirectory()) throw new IOException(dir + " ist kein Verzeichnis!");
		if (!file.canRead()) throw new IOException("Verzeichnis " + dir + " ist nicht lesbar!");
		if (mustWrite && !file.canWrite()) throw new IOException("Verzeichnis " + dir + " ist nicht beschreibbar!");
		return true;
	}
	
	/**Wie checkDir, aber mustWrite wird als true angenommen
	 * @param dir
	 * @return
	 * @throws IOException
	 */
	public static boolean checkDir(String dir) throws IOException {
		return checkDir(dir, true);
	}
	
	/**Gibt eine alphabetisch sortierte Dateiliste zurück
	 * @param dir
	 * @return
	 */
	public static File[] getSortedFileList(File dir) {
		File[] files = dir.listFiles();
		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return f1.getName().compareTo(f2.getName());
			}
		});
		return files;
	}	
}

/**
 * Datei: HTMLTransformer.java
 * Paket: de.beimax.testel.mime.html.util
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
package de.beimax.testel.mime.html.util;

import java.awt.Color;
import java.awt.Image;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import org.lobobrowser.html.domimpl.*;
import org.lobobrowser.html.gui.HtmlPanel;
import org.lobobrowser.html.parser.DocumentBuilderImpl;
import org.lobobrowser.html.parser.InputSourceImpl;
import org.lobobrowser.html.style.CSS2PropertiesImpl;
import org.lobobrowser.html.style.HtmlValues;
import org.lobobrowser.html.style.RenderState;
import org.lobobrowser.html.test.SimpleHtmlRendererContext;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import de.beimax.testel.config.Config;

/**
 * @author mkalus
 *
 */
public class HTMLTransformer {
	private static final Logger logger = Logger.getLogger(HTMLTransformer.class.getName());
	
	private HTMLDocumentImpl domdocument;
	private SimpleHtmlRendererContext rendererContext;

	/**
	 * @param args
	 */
//	public static void main(String[] args) throws Exception {
//		HTMLTransformer transformer = new HTMLTransformer();
//		transformer.load("file:test/res_1");
//		transformer.toStream(System.out);
//		
//		System.exit(0);
//	}
//	
	public HTMLTransformer(String document, URL url) throws IOException, SAXException, InterruptedException {
		InputSourceImpl is = new InputSourceImpl(new StringReader(document), url.toString());
		
		HtmlPanel htmlPanel = new HtmlPanel();
		htmlPanel.setPreferredWidth(800);
		rendererContext = new SimpleHtmlRendererContext(
				htmlPanel);

//		final JFrame frame = new JFrame();
//		frame.getContentPane().add(htmlPanel);
//		EventQueue.invokeLater(new Runnable() {
//			public void run() {
//				frame.pack();
//				frame.setVisible(true);
//			}
//		});
//
		domdocument = (HTMLDocumentImpl) new DocumentBuilderImpl(
				rendererContext.getUserAgentContext()).parse(is);
		htmlPanel.setDocument(domdocument, rendererContext);
		rendererContext.navigate(url, "_top");
		String lobosleep = Config.getConfig("Lobo-Sleep");
		int sleep;
		if (lobosleep == null) sleep = 10000; //10s
		else {
			try {
				sleep = Integer.parseInt(lobosleep);
			} catch (NumberFormatException e) {
				sleep = 10000; //10s
			}
		}
		logger.info("Thread schläft " + sleep + "ms, um Lobo Zeit zum parsen zu geben!");
		Thread.sleep(sleep);
	}

	public String transform() throws IOException {
		//private Klasse aufrufen zum Erstellen des DOM-Durchlaufs,
		//falls mehrere Threads...
		DocumentTraverser traverser = new DocumentTraverser();
		return traverser.traverse();
	}
	
	private class DocumentTraverser {
		private StringWriter out;
		private HashSet<String> noClosingTags;
		private HashSet<String> ignoreTags;
		private HashSet<String> ignoreAttribs;
		private HashSet<String> noAttribs;
		private HashMap<String, Integer> counters;
		String addContentBefore;
		
		public DocumentTraverser() {
			out = new StringWriter();
			noClosingTags = new HashSet<String>();
			ignoreTags = new HashSet<String>();
			ignoreAttribs = new HashSet<String>();
			noAttribs = new HashSet<String>();
			counters = new HashMap<String, Integer>();
			addContentBefore = null;
			init();
		}
		
		private void init() {
			//keine Schließtags hier:
			noClosingTags.add("br");
			noClosingTags.add("img");
			noClosingTags.add("hr");
			
			//Tags, die nicht weiter besucht werden
			ignoreTags.add("script");
			
			//Attribute, die nicht weiter verwendet werden
			//style und class werden über den CSS-Parser eingefügt!
			ignoreAttribs.add("style");
			ignoreAttribs.add("class");
			
			//Elemente, die keine Attribute haben dürfen
			noAttribs.add("br");
		}
		
		public String traverse() throws IOException {
			logger.info("Durchlaufe HTML-Dokument und bearbeite CSS-Daten.");
			out.write("<html><head>");
			if (domdocument.getTitle() != null) out.write("<title>" + domdocument.getTitle() + "</title>");
			
			out.write("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head>");
			
			writeStartTag((HTMLElementImpl) domdocument.getBody());
			traverseAllNodes(domdocument.getBody());
			out.write("</body></html>");
			return out.toString();
		}

		private void traverseAllNodes(Node node) throws IOException {
//			boolean eol = false;
//			
			if (node.hasChildNodes()) {
				NodeList nl = node.getChildNodes();
				for (int i = 0; i < nl.getLength(); i++) {
					Node childnode = nl.item(i);
					String myname = childnode.getNodeName().toLowerCase();
					
					//Textcontent als solchen ausgeben und weiter...
					if (myname.equals("#text")) {
						//< und > durch Entities ersetzen, da es sonst zu Fehlern kommt.
						String text = childnode.getNodeValue().trim();
						text = text.replaceAll("<", "&lt;");
						text = text.replaceAll(">", "&gt;");
						
						//System.out.println(text);
						out.write(text);
						logger.finer("#Text-Node: " + text);
						continue;
					} else if (myname.equals("#comment")) continue; //Kommentare überspringen
					
					//ignorierte Tags überspringen
					if (ignoreTags.contains(myname)) continue;
					
					if (childnode instanceof HTMLElementImpl) {
						HTMLElementImpl htmlelem = (HTMLElementImpl) childnode;
//						if (htmlelem.getRenderState().getDisplay() == RenderState.DISPLAY_INLINE) eol = false;
//						else eol = true;
						writeStartTag(htmlelem);
						if (addContentBefore != null) { //Content-Style-Anweisung
							out.write(addContentBefore);
							addContentBefore = null;
						}
						//Nach Blockelementen einen Umbruch machen
					} else {
						logger.severe("Unbekannte HTML-Node: " + myname);
					}

					traverseAllNodes(childnode);
					
					writeClosingTag(myname);
//					if (eol) out.write('\n');
				}
			}
		}
		
		private void writeStartTag(HTMLElementImpl node) throws IOException {
			String tag = node.getNodeName().toLowerCase();
			
			out.write(("<" + tag));
			
			if (!noAttribs.contains(tag))
				writeAttributes(node);
			
			if (noClosingTags.contains(tag)) //schließende Tags abfangen
				out.write("/ >");
			else out.write(">");
			logger.finer("Start-Tag: " + tag);
		}
		
		private void writeClosingTag(String tag) throws IOException {
			if (noClosingTags.contains(tag)) return; //kein Schließtag
			out.write(("</" + tag + ">"));
			logger.finer("End-Tag: " + tag);
		}
		
		private void writeAttributes(HTMLElementImpl node) throws IOException {
			NamedNodeMap attr = node.getAttributes();
			
			//Ein TestEl-Tag - sollte normalerweise nicht passieren, kann aber...
			boolean testelnode = false;
			if (node.getNodeName().startsWith("testel:")) testelnode = true;
			
			for (int i = 0; i < attr.getLength(); i++) {
				String attrname = attr.item(i).getNodeName().toLowerCase();
				
				if (!testelnode && ignoreAttribs.contains(attrname)) continue; //in ignore-Liste?
				
				String value = attr.item(i).getNodeValue();
				//Bilder mit URL speichern...
				if (node instanceof HTMLImageElementImpl && attrname.equals("src")) {
					HTMLImageElementImpl xnode = (HTMLImageElementImpl) node;
					value = xnode.getFullURL(xnode.getSrc()).toString();
				}
				
				out.write((" " + attrname + "=\"" + value + "\""));
				logger.finest("Tag-Attribute: " + attrname + "=" + value);
			}
			
			//so, jetzt noch CSS hinzufügen, sofern keine TestEl-Node
			if (!testelnode) writeCSSAttrib(node);
		}
		
		private void writeCSSAttrib(HTMLElementImpl node) throws IOException {
			CSS2PropertiesImpl css = node.getCurrentStyle();
			RenderState state = node.getRenderState();
			String mystyle = "";
			boolean addTextCSS = true;
			
			//verschiedene Typen von Nodes checken
			//Bilder:
			if (node instanceof HTMLImageElementImpl) {
				int width, height;

				HTMLImageElementImpl xnode = (HTMLImageElementImpl) node;
				//so, zuerst schauen wir, ob das Bild eine fixe Höhe oder Breite hat
				height = xnode.getHeight();
				width = xnode.getWidth();
				//alternative Höhen - 800x600 als Annahmen, weil hier nur das Verhältnis interessant ist!
				int dw = HtmlValues.getOldSyntaxPixelSize(node.getAttribute("width"), 800, -1);
				int dh = HtmlValues.getOldSyntaxPixelSize(node.getAttribute("height"), 600, -1);
				if (dw != -1) width = dw; //übernehmen, falls vorhanden
				if (dh != -1) height = dh; //übernehmen, falls vorhanden
				//nicht angegeben: Bild laden:
				if (height == 0 || width == 0) {
					URL imgurl = xnode.getFullURL(xnode.getSrc());
					logger.info("Lade Bild: " + imgurl);
					
					//Image-Icon von swing verwenden...
					ImageIcon img = new ImageIcon(imgurl);
					
					Image image = img.getImage();
					//Größenverhältnisse anpassen
					if (height == 0 && width == 0) { //einfach: keine Anpassung
						width = image.getWidth(img.getImageObserver());
						height = image.getHeight(img.getImageObserver());
					} else if (height == 0 && width != 0) {
						double ratio = image.getWidth(img.getImageObserver()) / width;
						height = (int) (image.getHeight(img.getImageObserver()) / ratio);
					} else if (height != 0 && width == 0) {
						double ratio = image.getHeight(img.getImageObserver()) / height;
						width = (int) (image.getWidth(img.getImageObserver()) / ratio);
					}
				}
				//Größen sind nun bekannt: eintragen
				mystyle += createCSS("height", String.valueOf(height));
				mystyle += createCSS("width", String.valueOf(width));
				addTextCSS = false; //keine Textauszeichnung
			}
			
			if (addTextCSS) {
				//Font-Zeug
				mystyle += createCSS("font-family", state.getFont().getFamily());
				mystyle += createCSS("font-size", Integer.toString(state.getFont().getSize()));
				if (state.getFont().isBold()) mystyle += createCSS("font-weight", "bold");
				else mystyle += createCSS("font-weight", "normal");
				if (state.getFont().isItalic()) mystyle += createCSS("font-style", "italic");
				else mystyle += createCSS("font-style", "normal");
				if ((state.getTextDecorationMask() & RenderState.MASK_TEXTDECORATION_UNDERLINE) != 0)
					mystyle += createCSS("text-decoration", "underline");
				else if ((state.getTextDecorationMask() & RenderState.MASK_TEXTDECORATION_LINE_THROUGH) != 0)
					mystyle += createCSS("text-decoration", "line-through");
				else mystyle += createCSS("text-decoration", "none"); //ignoriert eine Reihe von Elementen, wie blink, etc.
			}

			//Color-Zeug
			//mystyle += createCSS("background-color", encodeColor(state.getBackgroundColor()));
			//mystyle += createCSS("color", encodeColor(state.getColor()));
			//state.getBackgroundInfo() - nicht implementiert
			//ausgeblendet, da eigentlich uninteressant...

			//TODO: noch implementieren, vielleicht, je nachdem, ob es notwendig ist...
			//state.getAlignXPercent()
			//state.getAlignYPercent()
			//state.getCount(counter, nesting)
			//state.getPaddingInsets()
			//state.getMarginInsets()
			//state.getDisplay() -> interessant!
			//     RenderState.DISPLAY_INLINE
			//state.getWhiteSpace()
			
			//Counters, komplizierter...
			if (css.getCounterReset() != null)
				createNewCounter(css.getCounterReset());
			
			if (css.getCounterIncrement() != null)
				incrementCounter(css.getCounterIncrement());
			
			//Content-Erstellung, vor allem für 
			if (css.getContent() != null) parseContentBefore(css.getContent());

			//System.out.println(css.getColor());
			
			if (mystyle.equals("")) return;
			
			out.write((" style=\"" + mystyle + "\""));
			logger.finest("Style-Attribute: " + mystyle);
		}
		
		private String createCSS(String key, String value) {
			if (value != null) return key + ":" + value + ";";
			return "";
		}
		
		@SuppressWarnings("unused")
		private String encodeColor(Color col) {
			if (col == null) return null;
			
			String red = Integer.toHexString(col.getRed());
			String green = Integer.toHexString(col.getGreen());
			String blue = Integer.toHexString(col.getBlue());
			
			if (red.length() == 1) red = "0" + red;
			if (green.length() == 1) green = "0" + green;
			if (blue.length() == 1) blue = "0" + blue;
			
			return "#" + red + green + blue;
		}
		
		private void createNewCounter(String key) {
			counters.put(key, 0);
			logger.finer("Neuer Counter " + key + " erstellt.");
		}
		
		private int incrementCounter(String key) {
			//neuen Counter erstellen, falls noch nicht existent
			if (!counters.containsKey(key)) createNewCounter(key);
			
			int num = counters.get(key);
			counters.put(key, ++num);
			logger.finer("Counter " + key + " hat nun die Nummer " + num + ".");
			return num;
		}
		
		private void parseContentBefore(String content) {
			Pattern p = Pattern.compile("counter.*?\\((.*?)\\)");
			Matcher m = p.matcher(content);
			//neuer String + Anführungszeichen weg
			String newcontent = new String(content.substring(1, content.length() -1));
			
			//Counter durch Zahlen ersetzen
			while ( m.find() ) {
			    String wrap = content.substring(m.start(), m.end());
			    String key = content.substring(m.start(1), m.end(1)).trim();
			    if (!counters.containsKey(key)) createNewCounter(key);
			    String val = Integer.toString(counters.get(key));
			    
			    newcontent = newcontent.replace(wrap, val);
			}
			
			//so counters abgearbeitet, jetzt nur noch Anführungzeichen tokenizen
			StringReader r = new StringReader(newcontent);
			StreamTokenizer st = new StreamTokenizer(r);
			st.parseNumbers();
			st.quoteChar('"');
			
			addContentBefore = "";
			try {
				for (int tval; (tval = st.nextToken()) != StreamTokenizer.TT_EOF;)
					if (tval == StreamTokenizer.TT_NUMBER) 
						addContentBefore += (int) st.nval;
					else addContentBefore += st.sval;
			} catch (IOException e) {}
		}

	}
}

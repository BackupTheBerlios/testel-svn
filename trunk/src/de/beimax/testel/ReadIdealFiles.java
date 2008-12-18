package de.beimax.testel;

import java.io.File;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.beimax.testel.mime.html.util.HTMLTokenizer;
import de.beimax.testel.util.IOHelper;

/**Liest die Dateien aus dem Ideal-Verzeichnis und erstellt entsprechende
 * 
 * @author mkalus
 *
 */
public class ReadIdealFiles {
	public static void main(String[] args) throws Exception {
		if (args.length != 1) throw new Exception("ideal-Directory als einziges Argument erwartet!");
		
		Pattern p = Pattern.compile("class=\"([^\"]*)\"");
		File idealDir = new File(args[0]);
		
		if (!idealDir.isDirectory()) throw new Exception("Kein Vorlagen/ideal-Directory");
		
		//Dateien aus dem Verzeichnis lesen
		String[] files = idealDir.list();
		
		for (int i = 0; i < files.length; i++) {
			if (!files[i].endsWith(".html")) continue;
			
			System.out.println("Bearbeite: " + files[i]);
			//Stack bilden:
			Stack<StackInfo> stack = new Stack<StackInfo>();
			LinkedList<StackInfo> finalStack = new LinkedList<StackInfo>();
			
			StringBuffer buffer = new StringBuffer(IOHelper.filetoString(new File(args[0] + "/" + files[i])));
			
			HTMLTokenizer reader = new HTMLTokenizer(new StringReader(buffer.toString()));
			
			int type;
			while ((type = reader.nextToken()) != HTMLTokenizer.TT_EOF) {
				
				if (type == HTMLTokenizer.TT_TAG) {
					String name = reader.getToken();
					if (name.contains("testel:")) {
						//System.out.println(reader.getToken());
						if (name.charAt(0) == '/') {
							StackInfo info = stack.pop();
							if (!name.equals('/' + info.type))
								throw new Exception("Matches passen nicht zueinander: " + name + " = /" + info.type + "? " + info.brow + ": " + info.bcol + " " + reader.getBeginningRow() + ":" + reader.getBeginningCol());
							info.epos = reader.getBeginningPos(); //Beginning, weil wir ja das Tag dann wegwerfen
							info.ecol = reader.getBeginningCol();
							info.erow = reader.getBeginningRow();
							finalStack.add(info);
						} else {
							String wholeTag = buffer.subSequence(reader.getBeginningPos(), reader.getEndPos()).toString();
							Matcher m = p.matcher(wholeTag);
							m.find();
							//System.out.println(wholeTag.substring(m.start(1), m.end(1)));
							StackInfo info = new StackInfo();
							info.type = name.substring(0, name.indexOf(' '));
							info.className = wholeTag.substring(m.start(1), m.end(1));
							info.bpos = reader.getBeginningPos();
							info.bcol = reader.getBeginningCol();
							info.brow = reader.getBeginningRow();
							stack.add(info);
						}
						//System.out.println(buffer.subSequence(reader.getBeginningPos(), reader.getEndPos()));
						buffer = buffer.replace(reader.getBeginningPos(), reader.getEndPos(), "");
						reader = new HTMLTokenizer(new StringReader(buffer.toString()));
					}
				}
			}
			
			if (!stack.isEmpty()) throw new Exception("Stack wurde nicht geleert: " + stack.size());
			
			//neuen XML-String erzeugen
			StringBuffer xml = new StringBuffer();
			//Header-Zeug vom String
			xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
			xml.append("<testel:externaldesc origURL=\"--\" saveURL=\"--\" >\n");
			
			StackInfo info;
			while ((info = finalStack.poll()) != null) {
				xml.append("  <");
				xml.append(info.type);
				xml.append(" class=\"");
				xml.append(info.className);
				xml.append("\" bpos=\"");
				xml.append(info.bpos); 
				xml.append("\" brow=\"");
				xml.append(info.brow);
				xml.append("\" bcol=\"");
				xml.append(info.bcol);
				xml.append("\" epos=\"");
				xml.append(info.epos);
				xml.append("\" erow=\"");
				xml.append(info.erow);
				xml.append("\" ecol=\"");
				xml.append(info.ecol);
				
				xml.append("\" />\n");
			}
			
			xml.append("</testel:externaldesc>\n");
			
			IOHelper.stringtoFile(xml.toString(), new File(args[0] + "/" + files[i] + ".xml"));
		}
	}
	
	private static class StackInfo {
		String type;
		String className;
		int bpos, bcol, brow;
		int epos, ecol, erow;
	}

}

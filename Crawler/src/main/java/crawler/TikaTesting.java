package crawler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.jsoup.Jsoup;
import org.xml.sax.SAXException;

public class TikaTesting {

	public static void main(final String[] args) throws IOException, SAXException, TikaException {

		// detecting the file type
		BodyContentHandler handler = new BodyContentHandler();
		Metadata metadata = new Metadata();
		ParseContext pcontext = new ParseContext();

		InputStream stream = new ByteArrayInputStream(
				Jsoup.connect("http://www.google.com").get().toString().getBytes(StandardCharsets.UTF_8));
		Tika tika = new Tika();
		tika.setMaxStringLength(10 * 1024 * 1024);
		
		
		// HTML parser
		HtmlParser htmlparser = new HtmlParser();
		htmlparser.parse(stream, handler, metadata, pcontext);
		System.out.println("Contents of the document:" + handler.toString());
		System.out.println("Metadata of the document:");
		String[] metadataNames = metadata.names();

		for (String name : metadataNames) {
			System.out.println(name + ": " + metadata.get(name));
		}
	}
}

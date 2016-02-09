package crawler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcernException;

/*
Description: Crawling n-level will get you all links in that level. 
*/
public class CrawlerExtractionCLI {

	static MongoClient mongoClient = new MongoClient();
	@SuppressWarnings("deprecation")
	static DB database = mongoClient.getDB("IR");
	static DBCollection collection = database.getCollection("data");

	// inserts into default database
	public static void insertDB(String URL, Integer LEVEL, boolean CRAWLED, Integer ERROR) {
		DBObject document = new BasicDBObject().append("_id", URL).append("LEVEL", LEVEL).append("CRAWLED", CRAWLED)
				.append("ERROR", ERROR);
		try {
			collection.insert(document);
		} catch (DuplicateKeyException dke) {
		} catch (WriteConcernException e) {
		}

	}

	public static void extractToDB(String _url, Document document) {
		// set limit to 10mb
		BodyContentHandler handler = new BodyContentHandler(10 * 1024 * 1024);
		Metadata metadata = new Metadata();
		ParseContext pcontext = new ParseContext();

		// convert Jsoup Doc to inputStream
		InputStream stream = new ByteArrayInputStream(document.toString().getBytes(StandardCharsets.UTF_8));

		// cracking the html page
		HtmlParser htmlparser = new HtmlParser();
		// LOL ALL THESE SWALLOWED EXCEPTIONS :D -Eric PLS
		try {
			htmlparser.parse(stream, handler, metadata, pcontext);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (TikaException e) {
			e.printStackTrace();
		}

		// TODO implement below so it is inserted into the database
		// insert metadata in db
		String[] metadataNames = metadata.names();

		for (String name : metadataNames) {
			collection.update(new BasicDBObject("_id", _url),
					new BasicDBObject("$set", new BasicDBObject(name.toUpperCase(), metadata.get(name))));
		}

		// contents
		// System.out.println("Contents of the document:" +
		// handler.toString().trim().replaceAll("\\s{2,}", " "));

		try {
			stream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void crawler(String URL, int depth) throws HttpStatusException {
		int currentDepth = 0;

		while (currentDepth < depth) {
			// System.out.println("Current Depth: " + currentDepth);
			if (currentDepth == 0) { // initial crawler
				insertDB(URL, currentDepth, false, null);
			}

			DBCursor linksToCrawl = collection.find(new BasicDBObject("CRAWLED", false).append("LEVEL", currentDepth));

			for (DBObject link : linksToCrawl) {
				Integer error = null;
				Document doc = null;

				try {
					doc = Jsoup.connect(link.get("_id").toString()).get();
				} catch (NullPointerException e) {
				} catch (HttpStatusException e) {
					error = e.getStatusCode();
				} catch (IOException e) {

				}
				// Extracts all links in current page
				if (error == null && doc != null) { // no errors
					extractToDB(link.get("_id").toString(), doc);

					// extracts all links
					Elements links = doc.select("a[href]");
					// inserts crawled links to mongodb
					for (Element crawledLinks : links) {

						// handles "www.a.com" and "www.a.com/" being crawed
						// again, omits the "/" on all links
						int sizeOfLink = crawledLinks.attr("abs:href").toLowerCase().trim().toString().length();
						if (sizeOfLink != 0) {
							if (crawledLinks.attr("abs:href").toLowerCase().trim().substring(sizeOfLink - 1, sizeOfLink)
									.equals("/")) {
								insertDB(
										crawledLinks.attr("abs:href").toLowerCase().trim().substring(0, sizeOfLink - 1),
										currentDepth + 1, false, null);
							} else {
								insertDB(crawledLinks.attr("abs:href").toLowerCase().trim(), currentDepth + 1, false,
										null);
							}
						}
					}
				}

				// update current link to crawled
				collection.update(new BasicDBObject("_id", link.get("_id").toString()),
						new BasicDBObject("$set", new BasicDBObject("CRAWLED", true).append("ERROR", error)));

			}
			currentDepth++;
			System.out.println("Depth " + currentDepth + " is done!");
		}
	}

	public static void main(String[] args) throws HttpStatusException {
		crawler("https://www.symantec.com", 1);
		mongoClient.close();
	}
}

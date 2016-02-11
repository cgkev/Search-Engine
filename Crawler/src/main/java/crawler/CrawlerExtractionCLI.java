package crawler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
	static DBCollection md = database.getCollection("data");

	// inserts into default database
	public static void insertDB(String URL, Integer LEVEL, boolean CRAWLED, Integer ERROR) {
		DBObject document = new BasicDBObject().append("_id", URL).append("LEVEL", LEVEL).append("CRAWLED", CRAWLED)
				.append("ERROR", ERROR);
		try {
			md.insert(document);
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

		// Parsing the html page
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

		// insert metadata in db
		String[] metadataNames = metadata.names();

		for (String name : metadataNames) {
			md.update(new BasicDBObject("_id", _url),
					new BasicDBObject("$set", new BasicDBObject(name.toUpperCase(), metadata.get(name))));
		}

		// insert content
		md.update(new BasicDBObject("_id", _url), new BasicDBObject("$set",
				new BasicDBObject("CONTENT", handler.toString().trim().replaceAll("\\s{2,}", " "))));
		
		try {
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void crawler(String URL, int depth, boolean extractToDB) throws HttpStatusException {
		int currentDepth = 0;

		
		while (currentDepth <= depth) {
			if (currentDepth == 0) { // initial crawler
				insertDB(URL, currentDepth, false, null);
			}

			DBCursor linksToCrawl = md.find(new BasicDBObject("CRAWLED", false).append("LEVEL", currentDepth));

			// loops through the query 
			for (DBObject link : linksToCrawl) {
				Integer error = null;
				Document doc = null;

				// html header time
				// URLConnection connection = null;
				// try {
				// connection = new
				// URL(link.get("_id").toString()).openConnection();
				// } catch (IOException e1) {
				// // TODO Auto-generated catch block
				// e1.printStackTrace();
				// }
				// System.out.println("1 " + connection.getHeaderFields());

				try {
					doc = Jsoup.connect(link.get("_id").toString()).get();
				} catch (NullPointerException e) {
				} catch (HttpStatusException e) {
					error = e.getStatusCode();
				} catch (IOException e) {

				}

				// Extracts all links in current page
				// checks if there are any connection errors or blank webpage
				if (error == null && doc != null) { // no errors

					// extract metadata to DB
					if (extractToDB) {
						extractToDB(link.get("_id").toString(), doc);
					}
					
					// extracts all links
					Elements links = doc.select("a[href]");
					
					
					// inserts links that are found on the current document to mongodb 
					for (Element crawledLinks : links) {

						// handles "www.a.com" and "www.a.com/" being crawled
						// again, omits the "/" on all links if present. 
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

				// update current link to crawled and add any errors 
				md.update(new BasicDBObject("_id", link.get("_id").toString()),
						new BasicDBObject("$set", new BasicDBObject("CRAWLED", true).append("ERROR", error)));

			}
			System.out.println("Depth " + currentDepth + " is done!");
			currentDepth++;
		}
		mongoClient.close();
	}

	public static void main(String[] args) throws HttpStatusException {

		// ----------Start of CLI----------
		Options options = new Options();

		// True means a field is required, false means just a flag
		options.addOption("d", true, "Depth to Crawl");
		options.addOption("u", true, "URL To Crawl");
		options.addOption("e", false, "Enable Extraction");

		CommandLine cmd = null;
		CommandLineParser parser = new DefaultParser();
		try {
			// uses args and options from above.
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// save data to a object
		int depth = Integer.parseInt(cmd.getOptionValue("d"));
		String URL = cmd.getOptionValue("u");
		// ----------End of CLI----------

		// LETS CRAWL! GO! 
		crawler(URL, depth, cmd.hasOption("e"));

		System.out.println("Crawling is completed!");
	}
}

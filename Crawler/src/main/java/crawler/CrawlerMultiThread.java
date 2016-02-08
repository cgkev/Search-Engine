package crawler;

import java.io.IOException;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcernException;

public class CrawlerMultiThread {

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
		}
		 catch(WriteConcernException e){
		}

	}
	
	public static void crawler(String URL, int depth) throws HttpStatusException {
		int currentDepth = 0;

		
		while (currentDepth < depth) {
//			System.out.println("Current Depth: " + currentDepth);
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
		crawler("http://www.justfuckinggoogleit.com", 1);
		mongoClient.close();
	}
}

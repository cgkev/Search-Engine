package searching_features;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class SearchFeatures {
	static MongoClient mongoClient = new MongoClient();
	@SuppressWarnings("deprecation")
	static DB database = mongoClient.getDB("IR");
	static DBCollection rankDB = database.getCollection("rank");
	static DBCollection indexDB = database.getCollection("index");

	public static String extractHtml(File fileName) throws IOException, SAXException, TikaException {
		BodyContentHandler handler = new BodyContentHandler(10 * 1024 * 1024);
		Metadata metadata = new Metadata();
		FileInputStream inputstream = new FileInputStream(fileName);
		ParseContext pcontext = new ParseContext();

		HtmlParser htmlparser = new HtmlParser();
		htmlparser.parse(inputstream, handler, metadata, pcontext);
		
		return handler.toString();
	}

	// parses "x and y" or "x or y" to computer readable
	public static List<Search> boolDriver(String boolSearch) throws IOException, SAXException, TikaException {
		String[] data = boolSearch.toLowerCase().split(" ");

		if (data[1].equals("and")) {
			return boolSearch(data[0], data[2], true);
		}

		else {
			return boolSearch(data[0], data[2], false);

		}

	}

	// isAnd = true = AND
	// is And = false = OR
	@SuppressWarnings("unchecked")
	public static List<Search> boolSearch(String word1, String word2, boolean isAnd) throws IOException, SAXException, TikaException {
		List<Search> search = new ArrayList<Search>();

		// and boolean
		if (isAnd) {
			HashSet<String> word1mp = new HashSet<String>();
			HashSet<String> word2mp = new HashSet<String>();

			List<Search> tempAll = new ArrayList<Search>();

			DBCursor w1 = indexDB.find(new BasicDBObject("Term", word1));
			ArrayList<Integer> position = new ArrayList<Integer>();

			for (DBObject link : w1) {
				File file = new File(link.get("URL").toString());

				String body = extractHtml(file);

				DBCursor rankingCursor = rankDB.find(new BasicDBObject("_id", link.get("URL").toString().trim()));
				position = (ArrayList<Integer>) link.get("Position");
				for (DBObject rank : rankingCursor) {

					if (link.get("URL").toString().equals(rank.get("_id"))) {
						double totalLink = (double) rank.get("PageRank") + (double) link.get("TFIDF");
						word1mp.add(link.get("URL").toString());
						tempAll.add(new Search(link.get("URL").toString(), (double) rank.get("PageRank"),
								(double) link.get("TFIDF"), totalLink, position, body));
					}

				}

			}

			DBCursor w2 = indexDB.find(new BasicDBObject("Term", word2));
			ArrayList<Integer> position2 = new ArrayList<Integer>();

			for (DBObject link : w2) {
				File file = new File(link.get("URL").toString());

				String body = extractHtml(file);

				DBCursor rankingCursor = rankDB.find(new BasicDBObject("_id", link.get("URL").toString().trim()));

				position2 = (ArrayList<Integer>) link.get("Position");

				for (DBObject rank : rankingCursor) {

					if (link.get("URL").toString().equals(rank.get("_id"))) {
						double totalLink = (double) rank.get("PageRank") + (double) link.get("TFIDF");
						word2mp.add(link.get("URL").toString());
						tempAll.add(new Search(link.get("URL").toString(), (double) rank.get("PageRank"),
								(double) link.get("TFIDF"), totalLink, position2, body));

					}

				}

			}

			word1mp.retainAll(word2mp);

			for (int i = 0; i < tempAll.size(); i++) {
				if (word1mp.contains(tempAll.get(i).getURL())) {
					search.add(tempAll.get(i));
				}

			}

		}
		// OR boolean
		else {

			DBCursor w1 = indexDB.find(new BasicDBObject("Term", word1));
			ArrayList<Integer> position1 = new ArrayList<Integer>();

			for (DBObject link : w1) {
				File file = new File(link.get("URL").toString());

				String body = extractHtml(file);
				DBCursor rankingCursor = rankDB.find(new BasicDBObject("_id", link.get("URL").toString().trim()));
				position1 = (ArrayList<Integer>) link.get("Position");
				for (DBObject rank : rankingCursor) {

					if (link.get("URL").toString().equals(rank.get("_id"))) {
						double totalLink = (double) rank.get("PageRank") + (double) link.get("TFIDF");

						search.add(new Search(link.get("URL").toString(), (double) rank.get("PageRank"),
								(double) link.get("TFIDF"), totalLink, position1, body));
					}

				}

			}

			DBCursor w2 = indexDB.find(new BasicDBObject("Term", word2));
			ArrayList<Integer> position2 = new ArrayList<Integer>();

			for (DBObject link : w2) {
				File file = new File(link.get("URL").toString());

				String body = extractHtml(file);
				DBCursor rankingCursor = rankDB.find(new BasicDBObject("_id", link.get("URL").toString().trim()));
				position2 = (ArrayList<Integer>) link.get("Position");
				for (DBObject rank : rankingCursor) {

					if (link.get("URL").toString().equals(rank.get("_id"))) {
						double totalLink = (double) rank.get("PageRank") + (double) link.get("TFIDF");

						search.add(new Search(link.get("URL").toString(), (double) rank.get("PageRank"),
								(double) link.get("TFIDF"), totalLink, position2, body));
					}

				}

			}
		}

		return search;
	}

	public static void main(String[] args) throws IOException, SAXException, TikaException {

		System.out.println("[URL] [TFIDF] [Link Analysis] [Total]");

		List<Search> search = boolDriver("sterling and township");

		for (int i = 0; i < search.size(); i++) {

			System.out.println(search.get(i).getURL() + " == " + search.get(i).getTfidf() + " == "
					+ search.get(i).getLinkAnalysis() + " == " + search.get(i).getTotalRanking());
			System.out.println(search.get(i).getPosition());
		}
		mongoClient.close();
	}

}

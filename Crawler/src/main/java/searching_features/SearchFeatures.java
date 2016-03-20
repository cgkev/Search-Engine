package searching_features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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

	// parses "x and y" or "x or y" to computer readable
	public static List<Search> boolDriver(String boolSearch) {
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
	public static List<Search> boolSearch(String word1, String word2, boolean isAnd) {
		List<Search> search = new ArrayList<Search>();

		// and boolean
		if (isAnd) {
			HashSet<String> word1mp = new HashSet<String>();
			HashSet<String> word2mp = new HashSet<String>();

			List<Search> tempAll = new ArrayList<Search>();

			DBCursor w1 = indexDB.find(new BasicDBObject("Term", word1));

			for (DBObject link : w1) {
				DBCursor rankingCursor = rankDB.find(new BasicDBObject("_id", link.get("URL").toString().trim()));
				for (DBObject rank : rankingCursor) {

					if (link.get("URL").toString().equals(rank.get("_id"))) {
						double totalLink = (double) rank.get("PageRank") + (double) link.get("TFIDF");
						word1mp.add(link.get("URL").toString());
						tempAll.add(new Search(link.get("URL").toString(), (double) rank.get("PageRank"),
								(double) link.get("TFIDF"), totalLink));
					}

				}

			}

			DBCursor w2 = indexDB.find(new BasicDBObject("Term", word2));

			for (DBObject link : w2) {
				DBCursor rankingCursor = rankDB.find(new BasicDBObject("_id", link.get("URL").toString().trim()));
				for (DBObject rank : rankingCursor) {

					if (link.get("URL").toString().equals(rank.get("_id"))) {
						double totalLink = (double) rank.get("PageRank") + (double) link.get("TFIDF");
						word2mp.add(link.get("URL").toString());
						tempAll.add(new Search(link.get("URL").toString(), (double) rank.get("PageRank"),
								(double) link.get("TFIDF"), totalLink));

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

			for (DBObject link : w1) {
				DBCursor rankingCursor = rankDB.find(new BasicDBObject("_id", link.get("URL").toString().trim()));
				for (DBObject rank : rankingCursor) {

					if (link.get("URL").toString().equals(rank.get("_id"))) {
						double totalLink = (double) rank.get("PageRank") + (double) link.get("TFIDF");

						search.add(new Search(link.get("URL").toString(), (double) rank.get("PageRank"),
								(double) link.get("TFIDF"), totalLink));
					}

				}

			}

			DBCursor w2 = indexDB.find(new BasicDBObject("Term", word2));

			for (DBObject link : w2) {
				DBCursor rankingCursor = rankDB.find(new BasicDBObject("_id", link.get("URL").toString().trim()));
				for (DBObject rank : rankingCursor) {

					if (link.get("URL").toString().equals(rank.get("_id"))) {
						double totalLink = (double) rank.get("PageRank") + (double) link.get("TFIDF");

						search.add(new Search(link.get("URL").toString(), (double) rank.get("PageRank"),
								(double) link.get("TFIDF"), totalLink));
					}

				}

			}
		}

		return search;
	}

	public static void main(String[] args) {

		System.out.println("[URL] [TFIDF] [Link Analysis] [Total]");

		List<Search> search = boolDriver("sterling or township");

		for (int i = 0; i < search.size(); i++) {

			System.out.println(search.get(i).getURL() + " == " + search.get(i).getTfidf() + " == "
					+ search.get(i).getLinkAnalysis() + " == " + search.get(i).getTotalRanking());
		}
		mongoClient.close();
	}

}

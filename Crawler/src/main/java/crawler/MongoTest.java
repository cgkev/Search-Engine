package crawler;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcernException;

public class MongoTest {
	static MongoClient mongoClient = new MongoClient();
	@SuppressWarnings("deprecation")
	static DB database = mongoClient.getDB("IR");
	static DBCollection collection = database.getCollection("data");

	public static void main(String[] args) {

		String test = "HELLO WORLD";

		DBObject document = new BasicDBObject().append("test", test);
		try {
			collection.insert(document);

		} catch (DuplicateKeyException dke) {
		} catch (WriteConcernException e) {

		}
	}
}

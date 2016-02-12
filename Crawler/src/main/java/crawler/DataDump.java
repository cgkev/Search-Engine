package crawler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class DataDump {

	public static void main(String[] args) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter("dataDump.json"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		MongoClient client = new MongoClient();
		MongoDatabase database = client.getDatabase("IR");
		MongoCollection<Document> collection = database.getCollection("data");

		for (Document doc : collection.find()) {
			try {
				writer.write(doc.toJson());
				writer.newLine();

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		client.close();
		
		System.out.println("Data Dump Completed!");

	}
}

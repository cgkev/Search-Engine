package ranking;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcernException;

// Write ranking here 

public class Ranking {
	static MongoClient mongoClient = new MongoClient();
	@SuppressWarnings("deprecation")
	static DB database = mongoClient.getDB("IR");
	static DBCollection md = database.getCollection("la");

	public static List<String> traverseAllFiles(String parentDirectory) throws IOException {
		Path startPath = Paths.get(parentDirectory);
		List<String> pathsOfHtml = new ArrayList<String>();
		Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				// Validates if files name has .html
				if (file.getFileName().toString().contains(".html")) {
					// add to List
					pathsOfHtml.add(file.toString());
				}
				return FileVisitResult.CONTINUE;
			}
		});

		return pathsOfHtml;
	}

	// inserts into default database
	public static void insertDB(String URL, int incoming, int outgoing) {
		DBObject document = new BasicDBObject().append("_id", URL).append("INCOMING", incoming).append("OUTGOING",
				outgoing);
		try {
			md.insert(document);
		} catch (DuplicateKeyException dke) {
		} catch (WriteConcernException e) {
		}
	}

	public static void linkAnalysis(String path) throws IOException {

		HashMap<String, ArrayList<String>> incomingList = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<String>> outgoingList = new HashMap<String, ArrayList<String>>();

		Map<String, Double> pageRank = new HashMap<String, Double>();

		List<String> paths = traverseAllFiles(path);

		// Goes through all documents
		for (int i = 0; i < paths.size(); i++) {
			pageRank.put(paths.get(i), 1 / 3.0);

			File input = new File(paths.get(i));
			Document doc = Jsoup.parse(input, "UTF-8");
			Elements links = doc.select("a[href]");
			int outgoingLink = 0;

			// adds all links inside document -> to Set
			Set<String> set = new HashSet<String>();
			for (Element crawledLinks : links) {
				set.add(crawledLinks.attr("href").toString());
			}

			// check if links are real
			// all links inside document
			ArrayList<String> outgoing = new ArrayList<String>();

			Iterator<String> iterator = set.iterator();
			while (iterator.hasNext()) {
				String link = iterator.next();

				if (link.length() != 0) {
					File linkCheck = new File(link);

					if (linkCheck.exists()) {
						outgoing.add(link);
						if (incomingList.get(link) == null) {
							ArrayList<String> incoming = new ArrayList<String>();
							incoming.add(paths.get(i));
							incomingList.put(link, incoming);
						} else {
							ArrayList<String> incoming = incomingList.get(link);
							incoming.add(paths.get(i));
							incomingList.put(link, incoming);
						}

					}
				}
			}
			
			outgoingList.put(paths.get(i), outgoing);
		}

		System.out.println("[link] [outgoing] [incoming] [pageRank]");
		for (String link : outgoingList.keySet())

		{
			String key = link.toString();
			System.out.println("Document: " + key);

			ArrayList<String> valueOfOutgoing = outgoingList.get(link);
			ArrayList<String> valueOfIncoming = incomingList.get(link);
			System.out.println(" \n Outgoing:");

			if (incomingList.containsKey(link)) {
				for (int i = 0; i < valueOfOutgoing.size(); i++) {
					System.out.println(valueOfOutgoing.get(i));
				}

				System.out.println(" \n Incoming:");

				for (int i = 0; i < valueOfIncoming.size(); i++) {
					System.out.println(valueOfIncoming.get(i));
				}
			}
			// contains no incoming links
			else {
				for (int i = 0; i < valueOfOutgoing.size(); i++) {
					System.out.println(valueOfOutgoing.get(i));
				}
			}
			System.out.println("----------------------------------------------------------");
		}

	}

	public static void main(String[] args) throws IOException {
		linkAnalysis("/Users/kevin/Desktop/testing");
		// linkAnalysis("C:/Users/LittleMonster/Desktop/testing");

		mongoClient.close();
	}

}

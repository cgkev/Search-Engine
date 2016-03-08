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
import java.util.List;
import java.util.Map;

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
		Map<String, Integer> incoming = new HashMap<String, Integer>();
		Map<String, Integer> outgoing = new HashMap<String, Integer>();

		List<String> paths = traverseAllFiles(path);

		for (int i = 0; i < paths.size(); i++) {

			File input = new File(paths.get(i));
			Document doc = Jsoup.parse(input, "UTF-8");
			Elements links = doc.select("a[href]");
			int outgoingLink = 0;
			for (Element crawledLinks : links) {
				int sizeOfLink = crawledLinks.attr("href").toString().length();
				if (sizeOfLink != 0) {
					File link = new File(crawledLinks.attr("href").toString());
					if (link.exists()) {
						outgoingLink++;
						if (incoming.get(crawledLinks.attr("href").toString()) == null) {
							incoming.put(crawledLinks.attr("href").toString(), 1);
						} else {
							incoming.put(crawledLinks.attr("href").toString(),
									incoming.get(crawledLinks.attr("href").toString() + 1));
						}
					}

				}
			}
				outgoing.put(paths.get(i), outgoingLink);
			
		}

		System.out.println("[link] [outgoing] [incoming]");
		for (String link : outgoing.keySet()) {
			String key = link.toString();
			String value = outgoing.get(link).toString();
			
			if(incoming.containsKey(link)){
				System.out.println(key + " " + value + " " + incoming.get(link).toString());
			}
			//contains no incoming links 
			else{
				System.out.println(key + " " + value + " 0");
			}
			
			
		}
	}

	public static void main(String[] args) throws IOException {
		linkAnalysis("/Users/kevin/Desktop/testing");

		mongoClient.close();
	}

}

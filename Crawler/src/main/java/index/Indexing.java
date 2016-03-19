package index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcernException;

// Write Indexing here

// ********--Citing Sources--******** 
// Kevin
// Walking the file tree: https://docs.oracle.com/javase/tutorial/essential/io/walk.html#invoke
// Walking the file tree: http://stackoverflow.com/questions/10014746/the-correct-way-to-use-filevisitor-in-java

public class Indexing {

	static MongoClient mongoClient = new MongoClient();
	@SuppressWarnings("deprecation")
	static DB database = mongoClient.getDB("IR");

	static DBCollection md = database.getCollection("index");

	static BulkWriteOperation bulkWriteOperation = md.initializeUnorderedBulkOperation();

	// <Term, Document>
	private static HashMap<String, HashSet<String>> idf = new HashMap<String, HashSet<String>>();

	public static final String[] ENGLISH_STOP_WORDS = { "a", "an", "and", "are", "as", "at", "be", "but", "by", "for",
			"if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then",
			"there", "these", "they", "this", "to", "was", "will", "with" };

	public static void insertDB(String term, String URL, double TFIDF) {

		DBObject document = new BasicDBObject().append("Term", term).append("URL", URL).append("TFIDF", TFIDF);

		try {

			bulkWriteOperation.insert(document);
		} catch (DuplicateKeyException dke) {
		} catch (WriteConcernException e) {
		}

	}

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

	public static HashMap<String, Integer> parseDocument(String text, String documentPath) {

		Scanner in = new Scanner(text);

		HashMap<String, Integer> tempTF = new HashMap<String, Integer>();

		while (in.hasNext()) {

			String word = in.next().toLowerCase();

			// If it is NOT an empty space or a STOP WORD; continue
			if (word != "" && word.matches("^[a-z].*$") && !word.matches("^\\p{Punct}")
					&& (isStopWord(word) == false)) {

				Integer count = tempTF.get(word);

				// If word isn't already in the map, initialize it at 1
				if (count == null) {
					count = 1;
				} else { // If word is already in the map, increment
					count += 1;
				}

				tempTF.put(word, count);

				if (!idf.containsKey(word)) {
					HashSet<String> documents = new HashSet<String>();
					documents.add(documentPath);
					idf.put(word, documents);
				} else {
					HashSet<String> documents = idf.get(word);
					documents.add(documentPath);
				}
			}
		}
		return tempTF;
	}

	public static void printHashMap(HashMap<String, Integer> map) {
		for (String key : map.keySet()) {
			System.out.println(key + " : " + map.get(key));
		}
	}

	public static void printWordPosition(HashMap<String, ArrayList<Integer>> map) {
		for (String key : map.keySet()) {
			System.out.println(key + " : " + map.get(key));
		}
	}

	public static boolean isStopWord(String word) {

		boolean isStopWord = false;

		for (int i = 0; i < ENGLISH_STOP_WORDS.length; i++) {
			if (ENGLISH_STOP_WORDS[i].equals(word)) {
				isStopWord = true;
				break;
			}
		}
		return isStopWord;
	}

	public static String extractHtml(File fileName) throws IOException, SAXException, TikaException {
		BodyContentHandler handler = new BodyContentHandler(10 * 1024 * 1024);
		Metadata metadata = new Metadata();
		FileInputStream inputstream = new FileInputStream(fileName);
		ParseContext pcontext = new ParseContext();

		HtmlParser htmlparser = new HtmlParser();
		htmlparser.parse(inputstream, handler, metadata, pcontext);

		return handler.toString();
	}

	public static void index(String Path) throws IOException, SAXException, TikaException {

		System.out.println("Indexing started");
		List<String> pathsToIndex = traverseAllFiles(Path);

		// Goes through each HTML document
		double tf = 0;
		double idfval = 0.0;
		// HashMap<String, Double> tfcalc = new HashMap<String, Double>();

		// HashMap<URL, HashMap<Word, tfCalc>>
		HashMap<String, HashMap<String, Double>> tfCalc = new HashMap<String, HashMap<String, Double>>();

		// HashMap<Word, HashMap<URL, TFIDF>>
		HashMap<String, Double> idfcalc = new HashMap<String, Double>();

		for (int i = 0; i < pathsToIndex.size(); i++) {
			File file = new File(pathsToIndex.get(i));
			String content = extractHtml(file);
			HashMap<String, Integer> parsedHashMap = parseDocument(content, pathsToIndex.get(i));

			// For each word, find the number of time the TERM occurs in the
			// document (AND) divide it by the number of terms in the document.
			HashMap<String, Double> tfMap = new HashMap<String, Double>();
			for (String word : parsedHashMap.keySet()) {
				tf = (double) parsedHashMap.get(word) / parsedHashMap.size();

				tfMap.put(word, tf);
			}
			tfCalc.put(pathsToIndex.get(i), tfMap);

		}
		// Gets the term in the IDF keyset.
		for (String key : idf.keySet()) {
			idfval = Math.log10((double) pathsToIndex.size() / idf.get(key).size());
			// System.out.println("Word: " + key + "\nSize: " +
			// idf.get(key).size() + "\nIDF: " + idfval);
			// System.out.println();
			idfcalc.put(key, idfval);
		}

		// HashMap<URL, hashMap<Term, TFIDF>>
		HashMap<String, HashMap<String, Double>> tfidfMap = new HashMap<String, HashMap<String, Double>>();

		HashMap<String, Double> tfidfGlobal2 = new HashMap<String, Double>();

		double tfidfcalc = 0.0;
		for (String URL : tfCalc.keySet()) {
			// System.out.println(URL);
			HashMap<String, Double> tfidfLocal = new HashMap<String, Double>();

			for (String key : tfCalc.get(URL).keySet()) {

				if (idfcalc.containsKey(key)) {
					tfidfcalc = idfcalc.get(key) * tfCalc.get(URL).get(key);

					// Term, TFIDF
					tfidfLocal.put(key, tfidfcalc);
					tfidfGlobal2.put(key, tfidfcalc);
				}
			}
			tfidfMap.put(URL, tfidfLocal);
		}

		System.out.println("Indexing finished");

		System.out.println("Inserting to BULK Processor");

		double maxTFIDF = Collections.max(tfidfGlobal2.values());

		// HM< TERM , HM<URL, TFIDF> >
		// Term, URL
		// HashMap<String, HashSet<String>> idf = new HashMap<String,
		// HashSet<String>>

		// HashSet<URL, TFIDF>>();
		// URL, TFIDF for that word.
		HashMap<HashSet<String>, Double> map = new HashMap<HashSet<String>, Double>();
		for (String URL : tfidfMap.keySet()) {
			for (String TERM : tfidfMap.get(URL).keySet()) {
				double normalizedTFIDF = tfidfMap.get(URL).get(TERM) / maxTFIDF;
				insertDB(TERM, URL, normalizedTFIDF);
				// insertDB(TERM, HashMapOf URL -> TFIDF);
			}
		}
		// for(String k : map.keySet()){
		// System.out.println(k + " " + map.get(k));
		// }

		// for(String k : idf.keySet()){
		// System.out.println(k + " " + idf.get(k));
		// }

		System.out.println("Inserting to BULK Processor done");

	}

	public static void main(String[] args) throws IOException, SAXException, TikaException {
		// Find the URL that correlates to the word in the collection.
		// DBCursor linksToCrawl = md.find(new BasicDBObject("URL", "hi"));
		// for (DBObject link : linksToCrawl) {
		// System.out.println(link.get("Term").toString());
		// }
		// Insert directory here
		String PATH = "/Users/kevin/Desktop/munged";

		// String PATH = "C:\\Users\\LittleMonster\\Desktop\\UrlLinks";
		// String PATH =
		// "C:\\Users\\LittleMonster\\Documents\\CSULA\\WINTER2016\\CS454\\en";

		index(PATH);
		System.out.println("Excute Bulk to mongo");

		bulkWriteOperation.execute();
		System.out.println("Excute Bulk to mongo done");

		mongoClient.close();

	}
}
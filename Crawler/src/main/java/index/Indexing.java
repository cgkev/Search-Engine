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
    static DBCollection md = database.getCollection("data");

    // <URL, HashMap<term, frequency>>
    private static HashMap<String, HashMap<String, Integer>> termFrequency = new HashMap<String, HashMap<String, Integer>>();

    // <Term, Document>
    private static HashMap<String, HashSet<String>> idf = new HashMap<String, HashSet<String>>();

    private static HashMap<String, ArrayList<Integer>> position = new HashMap<String, ArrayList<Integer>>();

    public static final String[] ENGLISH_STOP_WORDS = { "a", "an", "and", "are", "as", "at", "be", "but", "by", "for",
	    "if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then",
	    "there", "these", "they", "this", "to", "was", "will", "with" };

    public static void insertDB(String term, HashSet<String> URL) {
	// DBObject document = new BasicDBObject().append("URL",
	// URL).append("WORD", term).append("Position", null)
	// .append("TFIDF", TFIDF);

	// Need to store term, list of documents and tfidf

	DBObject document = new BasicDBObject().append("_id", term).append("URL", URL).append("TFIDF", null);

	try {
	    md.insert(document);
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

	List<String> pathsToIndex = traverseAllFiles(Path);

	// Goes through each HTML document
	double tf = 0;
	double idfval = 0.0;

	int counter = 0;

	HashMap<String, Double> tfcalc = new HashMap<String, Double>();

	HashMap<String, Double> dfcalc = new HashMap<String, Double>();

	HashMap<String, Double> tfidf = new HashMap<String, Double>();

	for (int i = 0; i < pathsToIndex.size(); i++) {
	    counter++;
	    System.out.println(counter);
	    File file = new File(pathsToIndex.get(i));
	    String content = extractHtml(file);
	    HashMap<String, Integer> parsedHashMap = parseDocument(content, pathsToIndex.get(i));

	    termFrequency.put(pathsToIndex.get(i), parsedHashMap);

	    // For each word, find the number of time the TERM occurs in the
	    // document (AND) divide it by the number of terms in the document.

	    for (String m : parsedHashMap.keySet()) {
		tf = (double) parsedHashMap.get(m) / parsedHashMap.size();
		tfcalc.put(m, tf);
		// System.out.println("WORD: " + m + " - TF: " + tf);
	    }
	    // for (Map.Entry<String, Integer> entry : parsedHashMap.entrySet())
	    // {
	    // tf = (double) entry.getValue() / parsedHashMap.size();
	    // tfcalc.put(entry, tf);
	    // // System.out.println("WORD: " + entry.getKey() + " - TF: " +
	    // // tf);
	    // }
	}
	System.out.println("starting insert");
	for (String key : idf.keySet()) {
	    idfval = Math.log10((double) pathsToIndex.size() / idf.get(key).size());
	    insertDB(key, idf.get(key));
	    dfcalc.put(key, idfval);
	    // System.out.println("Total path to index : " +
	    // pathsToIndex.size());
	    // System.out.println("Word: " + key + "\n Number of Documents with
	    // term T: " + idf.get(key).size());
	    // System.out.println(pathsToIndex.size() / idf.get(key).size());
	    // System.out.println("IDF: " + idfval);
	    // System.out.println();
	}
//	System.out.println("insert done");
//	int update = 0;
	double tfidfcalc = 0.0;
//	System.out.println("tfidf update");
	for (String word : tfcalc.keySet()) {
//	    update++;
	    if (dfcalc.containsKey(word)) {
		tfidfcalc = dfcalc.get(word) * tfcalc.get(word);
		// System.out.println("Word: " + word + "\n tfidfcalc
		// "+tfidfcalc);
		// System.out.println();
		tfidf.put(word, tfidfcalc);
//		System.out.println("updating: " + update);
		BasicDBObject TFIDFObject = new BasicDBObject().append("$set",
			new BasicDBObject().append("TFIDF", tfidf.get(word)));
		md.update(new BasicDBObject("_id", word), TFIDFObject);
	    }
	}
//	System.out.println("tfidf done");

	// System.out.println(tfidf.size());

	// for (String key : tfidf.keySet()) {
	// System.out.println(key + " : " + tfidf.get(key));
	// }

	// System.out.println("Term Frequency: ");
	// for (String key : termFrequency.keySet()) {
	// System.out.println(key + " : " + termFrequency.get(key));
	// }
	//
	// System.out.println();
	//
	// System.out.println("IDF");
	// for (String key : idf.keySet()) {
	// System.out.println(key + " : " + idf.get(key));
	// }

    }

    public static void main(String[] args) throws IOException, SAXException, TikaException {
	// Insert directory here
//	String PATH = "C:\\Users\\LittleMonster\\Desktop\\UrlLinks";
	 String PATH =
	 "C:\\Users\\LittleMonster\\Documents\\CSULA\\WINTER2016\\CS454\\en";

	index(PATH);
	mongoClient.close();
	// Find the URL that correlates to the word in the collection.
	// DBCursor linksToCrawl = md.find(new BasicDBObject("WORD", "hi"));
	// for (DBObject link : linksToCrawl) {
	// System.out.println(link.get("URL").toString());
	// }

    }
}
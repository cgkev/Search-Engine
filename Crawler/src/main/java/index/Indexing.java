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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
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

// Varunya
// Stop words: http://www.ranks.nl/stopwords

public class Indexing {

    static MongoClient mongoClient = new MongoClient();
    @SuppressWarnings("deprecation")
    static DB database = mongoClient.getDB("IR");

    static DBCollection md = database.getCollection("index");

    // HashMap<Term, HashSet<Documents>>
    private static HashMap<String, HashSet<String>> idf = new HashMap<String, HashSet<String>>();

    // HashMap<UniqueUrl, ArrayList<Word Positions>>
    static HashMap<String, ArrayList<Integer>> position = new HashMap<String, ArrayList<Integer>>();

    
    public static final String[] ENGLISH_STOP_WORDS = { "a", "about", "above", "after", "again", "against", "all", "am",
	    "an", "any", "and", "are", "aren't", "as", "at", "be", "because", "been", "before", "being", "below",
	    "between", "both", "but", "by", "can't", "cannot", "couldn't", "did", "didn't", "do", "does", "doesn't",
	    "doing", "don't", "down", "during", "each", "few", "from", "further", "for", "had", "hadnt", "has",
	    "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers",
	    "herself", "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "is", "isn't", "it",
	    "it's", "its", "itself", "if", "in", "into", "let's", "me", "more", "most", "mustn't", "my", "myself", "no",
	    "not", "nor", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out",
	    "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so", "some",
	    "such", "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there",
	    "there's", "these", "they", "they'd", "they'll", "they're", "they've", "those", "through", "this", "to",
	    "too", "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were",
	    "weren't", "what", "what's", "when", "when's", "where", "where's", "which", "while", "who", "who's", "whom",
	    "why", "why's", "won't", "would", "wouldn't", "will", "with", "you", "you'd", "you'll", "you're", "you've",
	    "your", "yours", "yourself", "yourselves"};

    public static void insertDB(String term, String URL, double TFIDF, ArrayList<Integer> arrayList) {

	DBObject document = new BasicDBObject().append("Term", term).append("URL", URL).append("TFIDF", TFIDF)
		.append("Position", arrayList);

	try {

	    // bulkWriteOperation.insert(document);
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

	int positionCounter = 0;

	while (in.hasNext()) {

	    ArrayList<Integer> wordPosition = new ArrayList<Integer>();

	    String word = in.next().toLowerCase();

	    // If it is NOT an empty space or a STOP WORD; continue
	    if (word != "" && word.matches("^[a-z].*$") && !word.matches("^\\p{Punct}")
		    && (isStopWord(word) == false)) {

		Integer count = tempTF.get(word);

		// If the HashMap does not contain this word then add position
		if (!position.containsKey(documentPath.concat(word))) {

		    wordPosition.add(positionCounter);

		    position.put(documentPath.concat(word), wordPosition);

		} else { // If the word already exists in the HashMap, grab all
			 // the values and add new value

		    wordPosition.addAll(position.get(documentPath.concat(word)));

		    wordPosition.add(positionCounter);

		    position.put(documentPath.concat(word), wordPosition);

		}

		positionCounter++;

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

	System.out.println("Indexing");

	List<String> pathsToIndex = traverseAllFiles(Path);

	double tf = 0;
	double idfval = 0.0;

	// HashMap<URL, HashMap<Word, tfCalc>>
	HashMap<String, HashMap<String, Double>> tfCalc = new HashMap<String, HashMap<String, Double>>();

	// HashMap<Word, HashMap<URL, TFIDF>>
	HashMap<String, Double> idfcalc = new HashMap<String, Double>();

	// For each document...
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
	    idfcalc.put(key, idfval);
	}

	// HashMap<URL, HashMap<Term, TFIDF>>
	HashMap<String, HashMap<String, Double>> tfidfMap = new HashMap<String, HashMap<String, Double>>();

	HashMap<String, Double> tfidfGlobal2 = new HashMap<String, Double>();

	double tfidfcalc = 0.0;
	for (String URL : tfCalc.keySet()) {

	    HashMap<String, Double> tfidfLocal = new HashMap<String, Double>();

	    for (String key : tfCalc.get(URL).keySet()) {

		if (idfcalc.containsKey(key)) {

		    tfidfcalc = idfcalc.get(key) * tfCalc.get(URL).get(key);

		    tfidfLocal.put(key, tfidfcalc);

		    tfidfGlobal2.put(key, tfidfcalc);
		}
	    }
	    tfidfMap.put(URL, tfidfLocal);
	}

	System.out.println("Indexing finished");

	double maxTFIDF = Collections.max(tfidfGlobal2.values());

	// Unique Key for Word Position
	String appendedUrlTerm = "";

	for (String URL : tfidfMap.keySet()) {

	    BulkWriteOperation bulkWriteOperation = md.initializeUnorderedBulkOperation();

	    for (String TERM : tfidfMap.get(URL).keySet()) {

		double normalizedTFIDF = tfidfMap.get(URL).get(TERM) / maxTFIDF;

		appendedUrlTerm = URL.concat(TERM);

		if (position.containsKey(appendedUrlTerm)) {
		    DBObject document = new BasicDBObject().append("Term", TERM).append("URL", URL)
			    .append("TFIDF", normalizedTFIDF).append("Position", position.get(appendedUrlTerm));

		    try {
			bulkWriteOperation.insert(document);

		    } catch (DuplicateKeyException dke) {
		    } catch (WriteConcernException e) {
		    }

		}

	    }

	    bulkWriteOperation.execute();

	}

    }

    public static void main(String[] args) throws IOException, SAXException, TikaException {
	// Find the URL that correlates to the word in the collection.
	// DBCursor linksToCrawl = md.find(new BasicDBObject("URL", "hi"));
	// for (DBObject link : linksToCrawl) {
	// System.out.println(link.get("Term").toString());
	// }
	
	// Insert directory here
	// String PATH = "/Users/kevin/Desktop/munged";
	// String PATH = "C:\\Users\\LittleMonster\\Desktop\\UrlLinks";
	// String PATH =
	// "C:\\Users\\LittleMonster\\Documents\\CSULA\\WINTER2016\\CS454\\en";
	
	String PATH = "C:\\Users\\LittleMonster\\Desktop\\munged";
	index(PATH);

	mongoClient.close();

    }
}
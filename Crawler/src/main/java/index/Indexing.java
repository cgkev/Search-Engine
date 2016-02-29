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
import java.util.List;
import java.util.Map;
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
import com.mongodb.DBCursor;
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

    private static HashMap<String, Double> termFrequency = new HashMap<String, Double>();
    private static HashMap<String, ArrayList<Integer>> wordPosition = new HashMap<String, ArrayList<Integer>>();

    public static final String[] ENGLISH_STOP_WORDS = { "a", "an", "and", "are", "as", "at", "be", "but", "by", "for",
	    "if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then",
	    "there", "these", "they", "this", "to", "was", "will", "with" };

    public static void insertDB(String URL, String term, Double termFrequency) {
	DBObject document = new BasicDBObject().append("URL", URL).append("WORD", term).append("TF", termFrequency)
		.append("IDF", null).append("TFIDF", null);
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

    public static double wordCount(String text) {

	int numberOfTerms = 0;

	Scanner in = new Scanner(text);
	while (in.hasNext()) {

	    // Convert the word into lower case *unique*
	    String word = convertToLowerCase(in.next());

	    // If it is NOT an empty space or a STOP WORD; continue
	    if (word != "" && (isStopWord(word) == false)) {
		numberOfTerms++;
		Double count = termFrequency.get(word);

		// If word isn't already in the map, initialize it at 1
		if (count == null) {
		    count = 1.0;
		} else { // If word is already in the map, increment
		    count += 1.0;
		}

		termFrequency.put(word, count);
	    }

	}

	// printHashMap(termFrequency);
	// printWordPosition(wordPosition);

	return numberOfTerms;
    }

    public static void printHashMap(HashMap<String, Double> map) {
	for (String key : map.keySet()) {
	    System.out.println(key + " : " + map.get(key));
	}
    }

    public static double calculateTF(HashMap<String, Double> map, double numberOfTerms) {
	double TF = 0;
	for (Map.Entry<String, Double> entry : map.entrySet()) {
	    TF = entry.setValue(entry.getValue() / numberOfTerms);
	}
	return TF;
    }

    public static double calculateIDF(double totalDocuments, double totalDocWithTerm) {
	return Math.log10(totalDocuments / totalDocWithTerm);
    }

    public static double calculateTFIDF(double TF, double IDF) {
	double TFIDF = TF * IDF;
	return TFIDF;
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

    public static String convertToLowerCase(String word) {

	String lowerCasedWord = "";

	for (int i = 0; i < word.length(); i++) {
	    char c = word.charAt(i);
	    if (Character.isLetter(c)) {
		lowerCasedWord = lowerCasedWord + c;
	    }
	}

	return lowerCasedWord.toLowerCase();
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

	for (int i = 0; i < pathsToIndex.size(); i++) {
	    File file = new File(pathsToIndex.get(i));
	    String content = extractHtml(file);

	    calculateTF(termFrequency, wordCount(content));

	    // moving TF to DB
	    for (Map.Entry<String, Double> entry : termFrequency.entrySet()) {
		insertDB(pathsToIndex.get(i).toString(), entry.getKey(), entry.getValue());
	    }

	    termFrequency.clear();
	}

	DBCursor curs = md.find();
	while (curs.hasNext()) {
	    DBObject obj = curs.next();

	    String word = (String) obj.get("WORD");
	    Double freq = (Double) obj.get("TF");

	    DBCursor findWord = md.find(new BasicDBObject("WORD", word));
	    double IDF = calculateIDF((double) pathsToIndex.size(), (double) findWord.size());
	    double TFIDF = calculateTFIDF(IDF, freq);

	    // Find "word" with name and update IDF value
	    BasicDBObject IDFObject = new BasicDBObject().append("$set", new BasicDBObject().append("IDF", IDF));

	    md.update(new BasicDBObject("WORD", word), IDFObject, false, true);

	    BasicDBObject TFIDFObject = new BasicDBObject().append("$set", new BasicDBObject().append("TFIDF", TFIDF));
	    md.update(new BasicDBObject("WORD", word), TFIDFObject, false, true);
	}

    }

    public static void main(String[] args) throws IOException, SAXException, TikaException {
	String PATH = "C:\\Users\\LittleMonster\\Desktop\\UrlLinks";

	index(PATH);

	// Find the URL that correlates to the word in the collection.
	DBCursor linksToCrawl = md.find(new BasicDBObject("WORD", "hi"));
	for (DBObject link : linksToCrawl) {
	    System.out.println(link.get("URL").toString());
	}

    }
}

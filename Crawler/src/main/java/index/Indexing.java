package index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

// Write Indexing here

public class Indexing {

    private static HashMap<String, Integer> map = new HashMap<String, Integer>();
    private static TreeMap<String, Integer> frequencies = new TreeMap<String, Integer>();
    // Another table for term, list of string
    // Another table for term, positions

    public Indexing() {
    };

    public static void main(String[] args) throws IOException, SAXException, TikaException {

	File file = new File("(15810)_1994_JR1_9064.html");
	String content = extractHtml(file);

	int numberOfTerms = wordCount(content);
//	System.out.println(termFrequency(frequencies, "an", numberOfTerms));
	 System.out.println(wordCount(content));
//	 String a = "of";
//	 System.out.println(isStopWord(a));
	// System.out.println(content);
    }

    public static final String[] ENGLISH_STOP_WORDS = { "a", "an", "and", "are", "as", "at", "be", "but", "by", "for",
	    "if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then",
	    "there", "these", "they", "this", "to", "was", "will", "with" };

    public static int wordCount(String text) {
	int numberOfTerms = 0;
	Scanner in = new Scanner(text);
	while (in.hasNext()) {
	    numberOfTerms++;
//	    System.out.println("in.next()"+in.next().toLowerCase());
	    // Convert the word into lower case *unique*
	    String word = convertToLowerCase(in.next());
	    
//	    System.out.println("Word: " + word + " |||| Is a stop word: " + isStopWord(word));
	    
	    // If it is NOT an empty space or a STOP WORD; continue
	    if (word != "" && (isStopWord(word) == false)) {
		// System.out.println("Word: " + "'"+word+"'" + " ---
		// isStopWord: " + isStopWord(word));
		Integer count = map.get(word);
		// If word isn't already in the map, initialize it at 1
		if (count == null) {
		    count = 1;
		} else { // If word is already in the map, increment
		    count += 1;
		}

		map.put(word, count);
		frequencies.clear();
		frequencies.putAll(map);
	    }
	}
	for (String key : map.keySet()) {
	    System.out.println(key + " : " + map.get(key));
	}
	return numberOfTerms;
    }

    public static double termFrequency(TreeMap<String, Integer> map, String term, int numberofterms) {
	DecimalFormat formatter = new DecimalFormat("#0.00000000");
	double tf = 0;
	tf = ((map.get(term).doubleValue()) / numberofterms);
	System.out.println("map get term value: " + map.get(term));
	System.out.println("number of terms: " + numberofterms);
	System.out.println("tf: " + formatter.format(tf));
	return tf;
    }

    // Checks to see if the word is a stop word
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
}

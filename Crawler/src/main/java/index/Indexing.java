package index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

// Write Indexing here

public class Indexing {

	private static HashMap<String, Double> termFrequency = new HashMap<String, Double>();
	private static HashMap<String, ArrayList<Integer>> wordPosition = new HashMap<String, ArrayList<Integer>>();
	// Term, URL
	private static HashMap<String, ArrayList<String>> TermURL = new HashMap<String, ArrayList<String>>();

	public static void main(String[] args) throws IOException, SAXException, TikaException {

		File file = new File("(15810)_1994_JR1_9064.html");
		String content = extractHtml(file);
		calculateTF(termFrequency, wordCount(content));
	}

	public static final String[] ENGLISH_STOP_WORDS = { "a", "an", "and", "are", "as", "at", "be", "but", "by", "for",
			"if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then",
			"there", "these", "they", "this", "to", "was", "will", "with" };

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

	public static void calculateTF(HashMap<String, Double> map, double numberOfTerms) {

		for (Map.Entry<String, Double> entry : map.entrySet()) {
			entry.setValue((entry.getValue() / numberOfTerms) - entry.getValue());
		}
	}

	public static double inverseDocumentFrequency(HashMap<String, Integer> map) {
		// number of doc = size of ocllection
		//
		return 0;

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

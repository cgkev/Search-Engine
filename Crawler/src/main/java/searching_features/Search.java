package searching_features;

import java.util.ArrayList;

public class Search implements Comparable<Search> {

	String URL;
	double linkAnalysis;
	double tfidf;
	double totalRanking;
	ArrayList<Integer> position = new ArrayList<Integer>();
	String body;

	public Search(String URL, double linkAnalysis, double tfidf, double totalRanking, ArrayList<Integer> position,
			String body) {
		this.URL = URL;
		this.linkAnalysis = linkAnalysis;
		this.tfidf = tfidf;
		this.totalRanking = totalRanking;
		this.position = position;
		this.body = body;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getURL() {
		return URL;
	}

	public void setURL(String uRL) {
		URL = uRL;
	}

	public double getLinkAnalysis() {
		return linkAnalysis;
	}

	public void setLinkAnalysis(double linkAnalysis) {
		this.linkAnalysis = linkAnalysis;
	}

	public double getTfidf() {
		return tfidf;
	}

	public void setTfidf(double tfidf) {
		this.tfidf = tfidf;
	}

	public double getTotalRanking() {
		return totalRanking;
	}

	public void setTotalRanking(double totalRanking) {
		this.totalRanking = totalRanking;
	}

	public ArrayList<Integer> getPosition() {
		return position;
	}

	public void setPosition(ArrayList<Integer> position) {
		this.position = position;
	}

	@Override
	public int compareTo(Search o) {
		return Double.compare(totalRanking, o.getTotalRanking());
	}

}

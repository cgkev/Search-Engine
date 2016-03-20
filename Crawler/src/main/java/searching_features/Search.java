package searching_features;

public class Search implements Comparable<Search>{

	String URL;
	double linkAnalysis;
	double tfidf;
	double totalRanking;
	
	public Search(String URL, double linkAnalysis, double tfidf, double totalRanking){
		this.URL = URL;
		this.linkAnalysis = linkAnalysis;
		this.tfidf = tfidf;
		this.totalRanking = totalRanking;
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

	@Override
	public int compareTo(Search o) {
		return Double.compare(totalRanking, o.getTotalRanking());
	}
}




/* --
COMP4321 Lab2 Exercise
Student Name:
Student ID:
Section:
Email:
*/
import java.util.*;
import org.htmlparser.beans.StringBean;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.beans.LinkBean;
import org.htmlparser.tags.TitleTag;
import java.net.URL;

public class Crawler
{
	private String rootUrl;
	private Map<Object, String[]> docIdToUrl = new HashMap<Object, String[]>();// Maps docId → URL, Title
	private Map<String, Integer> urlToDocId; // Maps URL → docId
	private Map<Integer, Vector<String>> docIdToTerms; // Maps docId → List of terms
	private int nextDocId;
	private int[][] linkMatrix;

	public Crawler(String rootUrl) {
		this.rootUrl = rootUrl;
		this.docIdToUrl = new HashMap<Object, String[]>();
		this.urlToDocId = new HashMap<>();
		this.docIdToTerms = new HashMap<>();
		this.nextDocId = 1;
	}

	public Map<Integer, List<Integer>> crawlAllLinks() throws ParserException {
		Map<Integer, List<Integer>> indexedDocs = new HashMap<>();
		Queue<String> urlQueue = new LinkedList<>();
		Set<String> visitedUrls = new HashSet<>();

		urlQueue.add(rootUrl);
		visitedUrls.add(rootUrl);
		assignDocId(rootUrl);

		while (!urlQueue.isEmpty()) {
			String currentUrl = urlQueue.poll();
			LinkBean lb = new LinkBean();
			lb.setURL(currentUrl);
			URL[] urlArray = lb.getLinks();

			Vector<String> terms = extractWords(currentUrl);
			docIdToTerms.put(urlToDocId.get(currentUrl), terms);

			List<Integer> linkedDocIds = new ArrayList<>();
			for (URL link : urlArray) {
				String linkStr = link.toString();
				if (!visitedUrls.contains(linkStr)) {
					visitedUrls.add(linkStr);
					urlQueue.add(linkStr);
					assignDocId(linkStr);
				}
				if (urlToDocId.containsKey(linkStr)) {
					linkedDocIds.add(urlToDocId.get(linkStr));
					Vector<String> linkWords = extractWords(linkStr);
					docIdToTerms.put(urlToDocId.get(linkStr), linkWords);
				} else {
					System.err.println("Doc ID not found for URL: " + linkStr);
				}
			}

			// Store document linkage in indexedDocs
			if (urlToDocId.containsKey(currentUrl)) {
				indexedDocs.put(urlToDocId.get(currentUrl), linkedDocIds);
			} else {
				System.err.println("Doc ID not found for current URL: " + currentUrl);
			}
		}

		int n = nextDocId-1;
		linkMatrix = new int[n][n];

		for (Map.Entry<Integer, List<Integer>> entry : indexedDocs.entrySet()) {
			int currentDocId = entry.getKey();
			List<Integer> linkedDocIds = entry.getValue();

			for (Integer linkedDocId : linkedDocIds) {
				linkMatrix[currentDocId - 1][linkedDocId - 1] = 1;
			}
		}

		return indexedDocs; // Returns docId-based structure

	}
	//get the link Matrix
	public int[][] getLinkMatrix() {
		return linkMatrix;
	}

	// Extracts the page title
	public String extractTitle(String url) throws ParserException {
		Parser parser = new Parser(url);
		NodeList nodeList = parser.extractAllNodesThatMatch(new NodeClassFilter(TitleTag.class));

		if (nodeList.size() > 0) {
			return nodeList.elementAt(0).toPlainTextString().trim(); // Extract & trim title
		}

		return "Title not found"; // Return fallback if no title exists
	}

	public Vector<String> extractWords(String url) throws ParserException {
		Vector<String> words = new Vector<>();
		StringBean sb = new StringBean();
		sb.setURL(url);
		String text = sb.getStrings();

		// Extract title using the `extractTitle` method
		String title = extractTitle(url);

		// Remove title by starting after its length
		if (!title.equals("Title not found")) {
			int titleIndex = text.indexOf(title);
			if (titleIndex != -1) { // Ensure the title exists in the text
				text = text.substring(title.length());
			}
		}

		// Split the text into words, excluding punctuation
		String[] wordsArray = text.split("[\\W]+");

		for (String word : wordsArray) {
			words.add(word);
		}

		return words;
	}


	// Assigns a unique docId to each URL (bidirectional mapping)
	private void assignDocId(String url) throws ParserException {
		if (!urlToDocId.containsKey(url)) {
			urlToDocId.put(url, nextDocId);
			docIdToUrl.put(nextDocId, new String[]{url,extractTitle(url)});
			nextDocId++;
		}
	}

	// Retrieves URL from docId
	public String[] getUrlFromDocId(int docId) {
		return docIdToUrl.getOrDefault(docId, new String[]{"URL not found", null});
	}

	// Retrieves docId from URL
	public int getDocIdFromUrl(String url) {
		return urlToDocId.getOrDefault(url, -1); // Returns -1 if URL is not found
	}

	// Retrieves words of the doc
	public Vector<String> getWordFromDocId(int docId) {
		Vector<String> defaultResult = new Vector<>();
		defaultResult.add("URL not found");
		return docIdToTerms.getOrDefault(docId, defaultResult);
	}

	// Prints all doc ID ↔ URL mappings
	public void printDocMapping() {
		for (Map.Entry<Object, String[]> entry : docIdToUrl.entrySet()) {
			System.out.println("Doc " + entry.getKey() + " -> " + entry.getValue());
		}
	}

}

	

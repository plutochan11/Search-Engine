# Search Engine Project 🔍

A Java-based web search engine with crawling, indexing, and ranking capabilities. The system processes web pages, builds inverted indexes, and ranks documents using **cosine similarity** and **PageRank**.

---

## Features ✨
- **Web Crawler**: Extracts URLs, titles, and content from web pages.
- **Inverted Index**: Efficiently stores terms with document IDs, frequencies, and positions.
- **Text Processing**: Stopword removal and Porter stemming.
- **Ranking Algorithms**:
  - **Cosine Similarity (TF-IDF)**: Measures query-document relevance.
  - **PageRank**: Computes page importance based on link structure.
- **Search Interface**: Returns top 10 results with contextual snippets.
  - **Pure cosine similarity** (TF-IDF)  
  - **Combined score** (cosine similarity × PageRank).
---

## Components 🧩
| File               | Description                                                                 |
|--------------------|-----------------------------------------------------------------------------|
| `Crawler.java`     | Crawls web pages, extracts content, and builds document mappings.           |
| `InvertedIndex.java` | Persistent inverted index using JDBM (stores postings: docID, freq, positions). |
| `StopStem.java`    | Removes stopwords and applies stemming to terms.                            |
| `CosSim.java`      | Calculates cosine similarity between queries and documents.                 |
| `PageRank.java`    | Computes PageRank scores for pages using the link matrix.                   |
| `SearchEngine.java` | Combines ranking algorithms and returns search results.                     |
| `Main.java`        | Orchestrates crawling, indexing, and interactive searching.                 |

---
graph LR
  Crawler -->|docIdToUrl| Main
  Main -->|termToTermId| SearchEngine
  CosSim -->|cosineScores| SearchEngine
  PageRank -->|pageRankScores| SearchEngine
  
## Data Structures 📊
- **`TermInfo`**: Tracks term metadata (ID, document frequency).
- **`WordInfo`**: Records term positions and frequencies per document.
- **`Posting`**: Inverted index entry (`docID`, `frequency`, `positions`).

---

## Usage 🚀

### Prerequisites
- Java 8+
- Maven (for dependency management)
- Libraries: `JDBM`, `HTMLParser`

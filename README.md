# WOW! Search Engine

A web search engine with crawling, indexing, and ranking capabilities, featuring a Spring Boot backend and Vue.js frontend.

## Project Structure

The project consists of two main components:

1. **Backend**: A Spring Boot application that handles web crawling, indexing, and search functionality
2. **Frontend**: A Vue.js application that provides a user-friendly interface for searching

## Prerequisites

### Backend Dependencies
- Java JDK 17 or higher
- Maven 3.6 or higher
- Spring Boot 2.7.x
- External libraries:
  - JDBM (for data storage)
  - HTMLParser (for web page parsing)
  - Custom crawler library (included in the project)

### Frontend Dependencies
- Node.js 16 or higher
- npm 7 or higher
- Vue.js 3.x
- Axios (for API requests)

## Installation and Setup

### Backend Setup

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd "Code 2"
   ```

2. Navigate to the backend directory:
   ```bash
   cd search-engine
   ```

3. Install Maven dependencies:
   ```bash
   mvn install
   ```

4. Build the application:
   ```bash
   mvn package
   ```

### Frontend Setup

1. Navigate to the frontend directory:
   ```bash
   cd "../Wow! UI"
   ```

2. Install npm dependencies:
   ```bash
   npm install
   ```

## Running the Application

### Start the Backend Server

1. Navigate to the backend directory:
   ```bash
   cd search-engine
   ```

2. Run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```

   The backend server will start and initialize the search engine (crawling, indexing, and computing PageRank). This might take a few minutes depending on the size of the website being crawled.

   The API will be available at: `http://localhost:8080/search-engine/api`

### Start the Frontend Development Server

1. Navigate to the frontend directory:
   ```bash
   cd "../Wow! UI"
   ```

2. Run the development server:
   ```bash
   npm run dev
   ```

3. Access the application in your web browser at the URL displayed in the terminal (typically `http://localhost:5173`)

## API Endpoints

The backend exposes the following REST API endpoints:

- **GET /search-engine/api/search**
  - Parameters:
    - `query`: The search query text
    - `rankBy`: Ranking method (`cosine` or `combined`)
  - Returns: List of search results with relevance scores and snippets

- **GET /search-engine/api/documents**
  - Returns: List of all indexed documents with basic information

- **GET /search-engine/api/documents/{docId}**
  - Parameters:
    - `docId`: Document identifier
  - Returns: Detailed information about a specific document

## Configuration

### Backend Configuration

The backend configuration can be modified in `search-engine/src/main/resources/application.properties`:

```properties
# Server configuration
server.port=8080
server.servlet.context-path=/search-engine

# Search engine configuration
search.engine.root-url=https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm
search.engine.stopwords-path=src/main/resources/stopwords.txt
search.engine.body-index-db=recordmanager2
search.engine.body-index-name=bodyIndex
search.engine.pagerank-iterations=5
search.engine.pagerank-damping-factor=0.8
```

### Frontend Configuration

The API base URL can be configured in `Wow! UI/src/components/SearchEngine.vue`:

```javascript
// API base URL
const API_BASE_URL = 'http://localhost:8080/search-engine/api';
```

## Features

- Web crawling and indexing
- Text processing with stopword removal and Porter stemming
- Vector space model for document retrieval
- PageRank algorithm for link analysis
- Cosine similarity scoring
- Combined ranking (PageRank × Cosine Similarity)
- Context snippets for search results
- Responsive web UI with modern design

## Notes

- The search engine initializes on application startup, which may take some time depending on the size of the crawled website
- The frontend automatically detects if the backend is still initializing and shows appropriate warnings
- CORS is enabled on the backend to allow requests from the frontend

## Development

### Backend Development

The Spring Boot application follows a standard structure:
- `src/main/java/hk/ust/csit5930/SearchEngineApplication.java`: Main application class
- `src/main/java/hk/ust/csit5930/controller/`: REST controllers
- `src/main/java/hk/ust/csit5930/service/`: Business logic services
- `src/main/java/hk/ust/csit5930/utils/`: Utility classes
- `src/main/java/hk/ust/csit5930/models/`: Data models

### Frontend Development

The Vue.js application is structured as follows:
- `src/components/SearchEngine.vue`: Main search component
- `src/App.vue`: Root Vue component
- `src/main.js`: Entry point

## Troubleshooting

### Common Issues

1. **Backend fails to start**
   - Check Java version (must be 17+)
   - Ensure ports 8080 is available
   - Verify Maven dependencies are correctly installed

2. **Frontend fails to connect to backend**
   - Ensure backend is running
   - Check CORS configuration in backend
   - Verify API base URL in frontend code

3. **Search returns no results**
   - Ensure the search engine has completed initialization
   - Try using different search terms
   - Check for errors in the backend console

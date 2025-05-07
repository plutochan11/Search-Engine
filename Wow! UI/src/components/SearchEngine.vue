<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import axios from 'axios';

// API base URL
const API_BASE_URL = 'http://localhost:8080/search-engine/api';

const query = ref('');
const allResults = ref([]); 
const isLoading = ref(false);
const errorMessage = ref('');
const hasSearched = ref(false);
const resultsPerPage = 10;
const currentPage = ref(1);
const isLoadingMore = ref(false);
const showSuggestions = ref(false);
const rankingMethod = ref('combined'); // Default to combined ranking
const isApiInitialized = ref(false); // Track if the API is initialized

// All possible suggested queries
const allSuggestedQueries = [
  "machine learning",
  "artificial intelligence",
  "neural networks",
  "data science",
  "natural language processing",
  "computer vision",
  "deep learning",
  "big data",
  "cloud computing",
  "blockchain",
  "web development",
  "cybersecurity",
  "virtual reality",
  "augmented reality",
  "quantum computing"
];

// Randomly select 4 suggestions
const getRandomSuggestions = () => {
  const shuffled = [...allSuggestedQueries].sort(() => 0.5 - Math.random());
  return shuffled.slice(0, 4);
};

// Suggestions shown to the user - randomly selected on component mount
const suggestedQueries = ref(getRandomSuggestions());

// Computed property for visible results based on current page
const visibleResults = computed(() => {
  return allResults.value.slice(0, currentPage.value * resultsPerPage);
});

// Computed property to check if there are more results to load
const hasMoreResults = computed(() => {
  return visibleResults.value.length < allResults.value.length;
});

// Check if the API is initialized when the component loads
const checkApiStatus = async () => {
  try {
    isLoading.value = true;
    // Use the documents endpoint to check if the API is ready
    const response = await axios.get(`${API_BASE_URL}/documents`);
    isApiInitialized.value = response.status === 200;
    isLoading.value = false;
  } catch (error) {
    console.error('API not initialized yet:', error);
    isApiInitialized.value = false;
    errorMessage.value = 'The search engine is still initializing. Please wait a moment and try again.';
    isLoading.value = false;
  }
};

// Watch for changes to the ranking method
watch(rankingMethod, () => {
  if (hasSearched.value && query.value) {
    search();
  }
});

const search = async () => {
  if (!query.value.trim()) return;
  
  isLoading.value = true;
  errorMessage.value = '';
  hasSearched.value = true;
  currentPage.value = 1; // Reset to first page on new search
  showSuggestions.value = false; // Hide suggestions when searching
  
  try {
    // Check if API is initialized before making the search request
    if (!isApiInitialized.value) {
      await checkApiStatus();
      
      if (!isApiInitialized.value) {
        throw new Error('Search engine is still initializing. Please try again in a moment.');
      }
    }
    
    // Make real API call to the Spring Boot backend
    const response = await axios.get(`${API_BASE_URL}/search`, {
      params: { 
        query: query.value,
        rankBy: rankingMethod.value
      }
    });
    
    // Map the API response to match our frontend data structure
    allResults.value = response.data.map(result => {
      return {
        docId: result.docId,
        score: result.score,
        pageRank: result.pageRankScore,
        combinedScore: result.score, // For combined ranking, the score is already combined
        title: result.title || 'Untitled Document',
        url: result.url,
        lastModified: new Date().toISOString().split('T')[0], // Placeholder
        size: 'Unknown',
        keywords: [], // Placeholder
        snippets: result.snippets || [],
        contextSnippet: createContextSnippet(result.snippets),
        pageSize: result.pageSize || 0, // Placeholder for page size
        topTerms: result.topTerms || [], // Placeholder for top terms
        parentUrls: result.parentUrls || [], // Placeholder for parent URLs
        childUrls: result.childUrls || [] // Placeholder for child URLs
      };
    });
    
    if (allResults.value.length === 0) {
      errorMessage.value = 'No results found for your query.';
    }
    
  } catch (error) {
    console.error('Error searching:', error);
    errorMessage.value = error.message || 'An error occurred while searching. Please try again.';
    allResults.value = [];
  } finally {
    isLoading.value = false;
  }
};

// Create a context snippet object from the list of snippets
const createContextSnippet = (snippets) => {
  if (!snippets || snippets.length === 0) {
    return {
      before: "No context available",
      highlight: "",
      after: ""
    };
  }
  
  const snippet = snippets[0]; // Use the first snippet
  const words = snippet.split(' ');
  const midIndex = Math.floor(words.length / 2);
  
  // Try to identify a potential highlight term from the query
  const queryTerms = query.value.toLowerCase().split(' ');
  let highlightIndex = words.findIndex(word => 
    queryTerms.some(term => word.toLowerCase().includes(term))
  );
  
  // If no match found, use the middle word
  if (highlightIndex === -1) highlightIndex = midIndex;
  
  const highlight = words[highlightIndex];
  const before = words.slice(0, highlightIndex).join(' ');
  const after = words.slice(highlightIndex + 1).join(' ');
  
  return {
    before,
    highlight,
    after
  };
};

// Function to load more results
const loadMore = () => {
  if (isLoadingMore.value || !hasMoreResults.value) return;
  
  isLoadingMore.value = true;
  
  // Adding a small delay to simulate loading
  setTimeout(() => {
    currentPage.value += 1;
    isLoadingMore.value = false;
  }, 300);
};

// Apply a suggested query
const applySuggestion = (suggestion) => {
  query.value = suggestion;
  search();
};

// Focus input and show suggestions
const focusInput = (event) => {
  if (!hasSearched.value && query.value === '') {
    showSuggestions.value = true;
  }
};

// Clear input and results to return to initial state
const clearSearch = () => {
  query.value = '';
  allResults.value = [];
  hasSearched.value = false;
};

// Infinite scrolling
const handleScroll = () => {
  if (!hasMoreResults.value || isLoading.value || isLoadingMore.value) return;
  
  const scrollPosition = window.innerHeight + window.pageYOffset;
  const pageBottom = document.documentElement.offsetHeight - 300; // 300px before bottom
  
  if (scrollPosition >= pageBottom) {
    loadMore();
  }
};

// Format file size to human-readable format
const formatFileSize = (sizeInBytes) => {
  if (sizeInBytes < 1024) {
    return sizeInBytes + ' B';
  } else if (sizeInBytes < 1024 * 1024) {
    return (sizeInBytes / 1024).toFixed(2) + ' KB';
  } else if (sizeInBytes < 1024 * 1024 * 1024) {
    return (sizeInBytes / (1024 * 1024)).toFixed(2) + ' MB';
  } else {
    return (sizeInBytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
  }
};

// Format date to readable string
const formatDate = (dateString) => {
  if (!dateString) return 'Unknown';
  const date = new Date(dateString);
  return date.toLocaleDateString();
};

// Attach/detach scroll listener
onMounted(() => {
  window.addEventListener('scroll', handleScroll);
  // Set random suggestions on mount
  suggestedQueries.value = getRandomSuggestions();
  // Check if the API is initialized
  checkApiStatus();
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<template>
  <div class="search-container">
    <h1 class="wow-title">WOW! Search Engine</h1>
    
    <div class="search-form">
      <div class="search-input-container">
        <div class="search-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
        </div>
        <input 
          type="text"
          v-model="query"
          @keyup.enter="search"
          @focus="focusInput"
          placeholder="Enter search query (use quotes for phrases)"
          class="search-input"
        />
        <div class="search-action-buttons" v-if="query">
          <button class="clear-button" @click="clearSearch" title="Clear search">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
      </div>
      <button @click="search" class="search-button" :disabled="isLoading">
        {{ isLoading ? 'Searching...' : 'Search' }}
      </button>
    </div>
    
    <!-- API initialization warning -->
    <div v-if="!isApiInitialized" class="api-warning">
      <div class="api-warning-icon">⚠️</div>
      <div class="api-warning-message">
        The search engine is still initializing. You can search, but results may be limited.
      </div>
    </div>
    
    <!-- Ranking method selector with centered radio buttons -->
    <div v-if="hasSearched && visibleResults.length > 0" class="ranking-selector">
      <span class="ranking-label">Ranking method:</span>
      <div class="ranking-options">
        <label>
          <input type="radio" v-model="rankingMethod" value="cosine" />
          <span>Cosine Similarity only</span>
        </label>
        <label>
          <input type="radio" v-model="rankingMethod" value="combined" />
          <span>Combined (PageRank × CosSim)</span>
        </label>
      </div>
    </div>
    
    <!-- Suggested queries section -->
    <div v-if="showSuggestions && !hasSearched" class="suggested-queries">
      <h3>Popular searches:</h3>
      <div class="suggestion-tags">
        <button 
          v-for="(suggestion, index) in suggestedQueries" 
          :key="index" 
          class="suggestion-tag"
          @click="applySuggestion(suggestion)"
        >
          {{ suggestion }}
        </button>
      </div>
    </div>
    
    <div v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </div>
    
    <div v-if="isLoading" class="loading">
      Searching...
    </div>
    
    <div v-else-if="visibleResults.length > 0" class="results">
      <h2>Search Results ({{ allResults.length }} found)</h2>
      
      <div v-for="(result, index) in visibleResults" :key="index" class="result-item">
        <div class="result-header">
          <span class="result-score">
            {{ rankingMethod === 'combined' ? 
                `Score: ${result.combinedScore.toFixed(4)}` : 
                `Score: ${result.score.toFixed(4)}` }}
          </span>
          <a :href="result.url" target="_blank" class="result-title">{{ result.title }}</a>
        </div>
        
        <a :href="result.url" target="_blank" class="result-url">{{ result.url }}</a>
        
        <div class="result-metadata">
          <span>Last modified: {{ formatDate(result.lastModified) }}</span>
          <span class="metadata-separator">|</span>
          <span>Size: {{ formatFileSize(result.pageSize) }}</span>
        </div>
        
        <div class="result-keywords">
          <span v-for="(term, i) in result.topTerms" :key="i" class="keyword-item">
            {{ term.key }} ({{ term.value }}){{ i < result.topTerms.length - 1 ? '; ' : '' }}
          </span>
        </div>
        
        <div class="result-links">
          <div class="link-section">
            <h4>Parent Links:</h4>
            <ul class="link-list">
              <li v-for="(parentUrl, i) in result.parentUrls" :key="i">
                <a :href="parentUrl" target="_blank">{{ parentUrl }}</a>
              </li>
            </ul>
          </div>
          
          <div class="link-section">
            <h4>Child Links:</h4>
            <ul class="link-list">
              <li v-for="(childUrl, i) in result.childUrls" :key="i">
                <a :href="childUrl" target="_blank">{{ childUrl }}</a>
              </li>
            </ul>
          </div>
        </div>
      </div>
      
      <div v-if="hasMoreResults && isLoadingMore" class="loading-more">
        Loading more results...
      </div>
    </div>
    
    <div v-else-if="hasSearched && query" class="no-results">
      No results found for "{{ query }}".
    </div>
  </div>
</template>

<style scoped>
.search-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

/* Simplified title styling - no animation */
.wow-title {
  text-align: center;
  margin-bottom: 1.5rem;
  background: linear-gradient(90deg, #4285f4, #0f9d58);
  color: transparent;
  -webkit-background-clip: text;
  background-clip: text;
  font-size: 3rem;
  font-weight: bold;
}

/* Google-style search form */
.search-form {
  display: flex;
  margin-bottom: 1.5rem;
  width: 100%;
  max-width: 650px;
  margin-left: auto;
  margin-right: auto;
}

.search-input-container {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  border-radius: 24px 0 0 24px;
  background: white;
  border: 1px solid #dfe1e5;
  box-shadow: none;
  transition: box-shadow 0.3s, border-color 0.3s;
  height: 44px;
}

.search-input-container:hover {
  box-shadow: 0 1px 6px rgba(32,33,36,0.28);
  border-color: rgba(223,225,229,0);
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  margin: 0;
  padding: 0 0 0 8px;
  height: 100%;
  color: rgba(0,0,0,.87);
  word-wrap: break-word;
  outline: none;
  font-size: 16px;
}

.search-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 13px;
  color: #9aa0a6;
}

.search-action-buttons {
  display: flex;
  align-items: center;
  padding-right: 8px;
}

.clear-button {
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  color: #70757a;
  padding: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
}

.clear-button:hover {
  background-color: #f1f3f4;
}

.search-button {
  background-color: #4285f4;
  color: white;
  border: none;
  border-radius: 0 24px 24px 0;
  cursor: pointer;
  font-size: 16px;
  height: 44px;
  padding: 0 16px;
  transition: background-color 0.2s;
}

.search-button:hover {
  background-color: #357ae8;
}

.search-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

/* API initialization warning */
.api-warning {
  background-color: #fef7e0;
  border-radius: 8px;
  padding: 12px;
  margin: 0 auto 1.5rem;
  max-width: 650px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.api-warning-icon {
  font-size: 24px;
}

.api-warning-message {
  color: #5d4037;
  font-size: 14px;
}

/* Ranking method selector */
.ranking-selector {
  margin: 0 auto 1.5rem;
  padding: 10px 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  max-width: 650px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.ranking-label {
  font-weight: 500;
  color: #5f6368;
}

.ranking-options {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.ranking-options label {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-size: 14px;
  color: #3c4043;
}

.ranking-options input {
  margin-right: 5px;
}

.suggested-queries {
  margin-bottom: 2rem;
  padding: 15px;
  border-radius: 8px;
  background-color: #f8f9fa;
  max-width: 650px;
  margin-left: auto;
  margin-right: auto;
}

.suggested-queries h3 {
  margin-top: 0;
  margin-bottom: 12px;
  color: #5f6368;
  font-size: 14px;
  font-weight: 500;
}

.suggestion-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.suggestion-tag {
  background-color: white;
  border: 1px solid #dadce0;
  border-radius: 18px;
  color: #3c4043;
  cursor: pointer;
  font-size: 14px;
  padding: 8px 16px;
  transition: all 0.2s;
}

.suggestion-tag:hover {
  background-color: #f1f3f4;
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

/* Existing styles */
.loading, .no-results, .error-message {
  text-align: center;
  margin: 2rem 0;
  color: #666;
}

.error-message {
  color: #d93025;
}

.results h2 {
  margin-bottom: 1rem;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.5rem;
}

.result-item {
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #eee;
}

.result-header {
  display: flex;
  align-items: baseline;
  margin-bottom: 0.5rem;
  flex-wrap: wrap;
  gap: 8px;
}

.result-score {
  font-weight: bold;
  color: #1a73e8;
  margin-right: 8px;
  font-size: 14px;
}

.result-title {
  font-size: 18px;
  color: #1a0dab;
  text-decoration: none;
  font-weight: 500;
  margin-bottom: 8px;
  display: inline-block;
}

.result-title:hover {
  text-decoration: underline;
}

.result-url {
  display: block;
  color: #006621;
  font-size: 14px;
  margin-top: 6px;
  margin-bottom: 12px;
  text-decoration: none;
}

.result-url:hover {
  text-decoration: underline;
}

/* Context snippet styling - improved */
.result-context {
  margin: 0.5rem 0;
  padding: 10px 15px;
  border-radius: 8px;
  background-color: #f8f9fa;
  font-size: 15px;
  line-height: 1.6;
}

.context-snippet {
  color: #3c4043;
}

.context-highlight {
  background-color: #fbbc05;
  font-weight: 600;
  color: #202124;
  padding: 2px 4px;
  border-radius: 3px;
  margin: 0 2px;
}

/* Multiple snippets container */
.result-snippets {
  margin: 1rem 0;
}

/* Loading indicator at the bottom when auto-loading */
.loading-more {
  text-align: center;
  padding: 15px;
  color: #70757a;
  font-style: italic;
}

/* New styles for updated result format */
.result-metadata {
  display: flex;
  font-size: 13px;
  color: #70757a;
  margin: 8px 0;
}

.metadata-separator {
  margin: 0 8px;
}

.result-keywords {
  margin: 8px 0;
  font-size: 14px;
  color: #4d5156;
}

.keyword-item {
  display: inline-block;
}

.result-links {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.link-section {
  flex: 1;
  min-width: 250px;
}

.link-section h4 {
  font-size: 14px;
  margin: 0 0 8px 0;
  color: #202124;
  font-weight: 500;
}

.link-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.link-list li {
  margin-bottom: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.link-list a {
  color: #1a0dab;
  text-decoration: none;
  font-size: 13px;
}

.link-list a:hover {
  text-decoration: underline;
}
</style>
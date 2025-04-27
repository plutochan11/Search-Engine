<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import axios from 'axios';

const query = ref('');
const allResults = ref([]); 
const isLoading = ref(false);
const errorMessage = ref('');
const hasSearched = ref(false);
const resultsPerPage = 10;
const currentPage = ref(1);
const isLoadingMore = ref(false);
const showSuggestions = ref(false);

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

const search = async () => {
  if (!query.value.trim()) return;
  
  isLoading.value = true;
  errorMessage.value = '';
  hasSearched.value = true;
  currentPage.value = 1; // Reset to first page on new search
  showSuggestions.value = false; // Hide suggestions when searching
  
  try {
    // Simulating API call delay
    await new Promise(resolve => setTimeout(resolve, 800));
    
    // Generate 50 mock documents
    allResults.value = Array.from({ length: 50 }, (_, i) => {
      const scoreValue = Math.round((0.99 - (i * 0.02)) * 100) / 100;
      return {
        score: Math.max(scoreValue, 0.01),
        title: `Sample Document Title ${i + 1}`,
        url: `https://example.com/doc${i + 1}`,
        lastModified: `2025-${Math.floor(Math.random() * 4) + 1}-${Math.floor(Math.random() * 28) + 1}`,
        size: `${Math.floor(Math.random() * 100) + 1}.${Math.floor(Math.random() * 9) + 1} KB`,
        keywords: [
          { word: `keyword${i + 1}a`, frequency: Math.floor(Math.random() * 20) + 5 },
          { word: `keyword${i + 1}b`, frequency: Math.floor(Math.random() * 15) + 5 },
          { word: `keyword${i + 1}c`, frequency: Math.floor(Math.random() * 10) + 3 },
          { word: `keyword${i + 1}d`, frequency: Math.floor(Math.random() * 8) + 2 },
          { word: `keyword${i + 1}e`, frequency: Math.floor(Math.random() * 5) + 1 }
        ],
        parentLinks: Array.from({ length: Math.floor(Math.random() * 3) + 1 }, (_, j) => ({
          title: `Parent Page ${j + 1} for Doc ${i + 1}`,
          url: `https://example.com/parent${j + 1}/doc${i + 1}`
        })),
        childLinks: Array.from({ length: Math.floor(Math.random() * 4) + 1 }, (_, j) => ({
          title: `Child Link ${j + 1} for Doc ${i + 1}`,
          url: `https://example.com/doc${i + 1}/child${j + 1}`
        }))
      };
    });
    
    /* 
    // This is the actual API call that will be used when your backend is ready
    const response = await axios.get('YOUR_SEARCH_ENGINE_API_ENDPOINT', {
      params: { q: query.value }
    });
    allResults.value = response.data;
    */
    
  } catch (error) {
    console.error('Error searching:', error);
    errorMessage.value = 'An error occurred while searching. Please try again.';
  } finally {
    isLoading.value = false;
  }
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

// Attach/detach scroll listener
onMounted(() => {
  window.addEventListener('scroll', handleScroll);
  // Set random suggestions on mount
  suggestedQueries.value = getRandomSuggestions();
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
            <circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line>
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
              <line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
      </div>
      <button @click="search" class="search-button" :disabled="isLoading">
        {{ isLoading ? 'Searching...' : 'Search' }}
      </button>
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
          <span class="result-score">{{ result.score.toFixed(2) }}</span>
          <a :href="result.url" target="_blank" class="result-title">{{ result.title }}</a>
        </div>
        
        <a :href="result.url" target="_blank" class="result-url">{{ result.url }}</a>
        
        <div class="result-meta">
          Last modified: {{ result.lastModified }}, Size: {{ result.size }}
        </div>
        
        <div class="result-keywords">
          <span v-for="(keyword, kidx) in result.keywords" :key="kidx">
            {{ keyword.word }} {{ keyword.frequency }}{{ kidx < result.keywords.length - 1 ? '; ' : '' }}
          </span>
        </div>
        
        <div class="result-links" v-if="result.parentLinks && result.parentLinks.length > 0">
          <div v-for="(link, pidx) in result.parentLinks" :key="`p-${pidx}`" class="parent-link">
            <a :href="link.url" target="_blank">{{ link.title }}</a>
          </div>
        </div>
        
        <div class="result-links" v-if="result.childLinks && result.childLinks.length > 0">
          <div v-for="(link, cidx) in result.childLinks" :key="`c-${cidx}`" class="child-link">
            <a :href="link.url" target="_blank">{{ link.title }}</a>
          </div>
        </div>
      </div>
      
      <!-- Loading indicator at the bottom when auto-loading -->
      <div v-if="hasMoreResults && isLoadingMore" class="loading-more">
        Loading more results...
      </div>
    </div>
    
    <!-- Only show "No results" message if a search has been performed -->
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
  margin-bottom: 2rem;
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
  margin-bottom: 0.25rem;
}

.result-score {
  font-weight: bold;
  color: #1a73e8;
  margin-right: 8px;
}

.result-title {
  font-size: 18px;
  color: #1a0dab;
  text-decoration: none;
  font-weight: 500;
}

.result-title:hover {
  text-decoration: underline;
}

.result-url {
  display: block;
  color: #006621;
  font-size: 14px;
  margin-bottom: 0.5rem;
  text-decoration: none;
}

.result-url:hover {
  text-decoration: underline;
}

.result-meta {
  color: #70757a;
  font-size: 14px;
  margin-bottom: 0.5rem;
}

.result-keywords {
  color: #333;
  font-size: 14px;
  margin-bottom: 0.5rem;
}

.result-links {
  margin-top: 0.5rem;
  font-size: 14px;
}

.parent-link, .child-link {
  margin: 0.25rem 0;
}

.parent-link a, .child-link a {
  color: #1a0dab;
  text-decoration: none;
}

.parent-link a:hover, .child-link a:hover {
  text-decoration: underline;
}

/* Icons for parent and child links */
.parent-link::before {
  content: "➤ ";
  color: #70757a;
  transform: rotate(270deg);
  display: inline-block;
  margin-right: 4px;
}

.child-link::before {
  content: "➤ ";
  color: #70757a;
  transform: rotate(90deg);
  display: inline-block;
  margin-right: 4px;
}

.loading-more {
  text-align: center;
  padding: 15px;
  color: #70757a;
  font-style: italic;
}
</style>
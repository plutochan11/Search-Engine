package com.hkust.searchengine.service;

import hk.ust.csit5930.Spider;
import hk.ust.csit5930.model.Page;
import hk.ust.csit5930.model.Relationship;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling web crawling operations using the crawler dependency
 */
@Service
@Slf4j
public class CrawlerService {

    private final Spider spider;
    private Map<Integer, Page> pageCache;
    private Map<Integer, List<Integer>> parentChildRelationships;
    private Map<Integer, List<Integer>> childParentRelationships;
    private boolean isInitialized = false;

    public CrawlerService() {
        this.spider = new Spider();
        this.pageCache = new HashMap<>();
        this.parentChildRelationships = new HashMap<>();
        this.childParentRelationships = new HashMap<>();
    }

    /**
     * Initialize the crawler and crawl webpages starting from the root URL
     * @param rootUrl the starting URL for crawling
     * @param maxPages maximum number of pages to crawl
     * @param maxDepth maximum depth of crawling
     */
    public void initialize(String rootUrl, int maxPages, int maxDepth) {
        if (isInitialized) {
            log.info("Crawler already initialized, skipping initialization");
            return;
        }

        log.info("Initializing crawler with root URL: {}, maxPages: {}, maxDepth: {}", rootUrl, maxPages, maxDepth);
        
        try {
            // Start crawling
            spider.crawl();
            
            // Get all crawled pages
            List<Page> pages = spider.getAllPages();
            
            // Cache all pages for quick access
            for (Page page : pages) {
                pageCache.put(page.getId(), page);
            }
            
            // Build parent-child and child-parent relationship maps
            buildRelationshipMaps();
            
            log.info("Crawler initialization complete. Crawled {} pages.", pages.size());
            isInitialized = true;
        } catch (Exception e) {
            log.error("Error initializing crawler", e);
            throw new RuntimeException("Failed to initialize crawler", e);
        }
    }
    
    /**
     * Build parent-child and child-parent relationship maps from crawled data
     */
    private void buildRelationshipMaps() {
        List<Relationship> relationships = spider.getAllRelationships();
        
        for (Relationship relationship : relationships) {
            int fromPageId = relationship.getFromPageId();
            int toPageId = relationship.getToPageId();
            
            // Build parent -> children map
            parentChildRelationships
                .computeIfAbsent(fromPageId, k -> new ArrayList<>())
                .add(toPageId);
                
            // Build child -> parents map
            childParentRelationships
                .computeIfAbsent(toPageId, k -> new ArrayList<>())
                .add(fromPageId);
        }
    }
    
    /**
     * Check if the crawler has been initialized
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Get the total number of pages crawled
     * @return the number of pages
     */
    public int getPageCount() {
        return pageCache.size();
    }
    
    /**
     * Get a page by its ID
     * @param pageId the page ID
     * @return the page, or null if not found
     */
    public Page getPageById(int pageId) {
        return pageCache.get(pageId);
    }
    
    /**
     * Get all crawled pages
     * @return list of all pages
     */
    public List<Page> getAllPages() {
        return new ArrayList<>(pageCache.values());
    }
    
    /**
     * Get child pages for a given page ID
     * @param pageId the parent page ID
     * @return list of child page IDs
     */
    public List<Integer> getChildPageIds(int pageId) {
        return parentChildRelationships.getOrDefault(pageId, Collections.emptyList());
    }
    
    /**
     * Get parent pages for a given page ID
     * @param pageId the child page ID
     * @return list of parent page IDs
     */
    public List<Integer> getParentPageIds(int pageId) {
        return childParentRelationships.getOrDefault(pageId, Collections.emptyList());
    }
    
    /**
     * Get child pages for a given page ID
     * @param pageId the parent page ID
     * @return list of child pages
     */
    public List<Page> getChildPages(int pageId) {
        List<Integer> childIds = getChildPageIds(pageId);
        return childIds.stream()
                .map(this::getPageById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Get parent pages for a given page ID
     * @param pageId the child page ID
     * @return list of parent pages
     */
    public List<Page> getParentPages(int pageId) {
        List<Integer> parentIds = getParentPageIds(pageId);
        return parentIds.stream()
                .map(this::getPageById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Get the entire link matrix for PageRank calculation
     * @return 2D array representing the link matrix
     */
    public int[][] getLinkMatrix() {
        int pageCount = pageCache.size();
        int[][] matrix = new int[pageCount][pageCount];
        
        for (Map.Entry<Integer, List<Integer>> entry : parentChildRelationships.entrySet()) {
            int fromId = entry.getKey() - 1;  // Adjust to 0-based indexing
            
            for (Integer toId : entry.getValue()) {
                matrix[fromId][toId - 1] = 1;  // Adjust to 0-based indexing
            }
        }
        
        return matrix;
    }
    
    /**
     * Extract all words from a page
     * @param pageId the page ID
     * @return vector of words, or empty vector if page not found
     */
    public Vector<String> getWordsFromPage(int pageId) {
        Page page = pageCache.get(pageId);
        if (page == null) {
            return new Vector<>();
        }
        
        String content = page.getContent();
        if (content == null || content.isEmpty()) {
            return new Vector<>();
        }
        
        // Split the content by whitespace and filter out empty strings
        String[] words = content.split("\\s+");
        Vector<String> result = new Vector<>();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.add(word);
            }
        }
        
        return result;
    }
}
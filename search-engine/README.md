# Web Crawler Project

A Spring Boot application that crawls web pages and stores them in a structured database. This application can fetch up to 300 pages from the web and organizes them in a structured storage.

## Project Structure

This project follows the standard Spring Boot application structure:

- `controller`: REST API endpoints for interacting with the crawler
- `service`: Business logic for crawling and processing web pages
- `repository`: Data access layer for storing and retrieving pages
- `model`: Domain entities representing web pages and relationships
- `config`: Application configuration classes

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8.x or higher

### Running the Application

```bash
# Build the project
mvn clean package

# Run the application
java -jar target/search-engine-1.0-SNAPSHOT.jar
```

Alternatively, you can use Maven to run the application directly:

```bash
mvn spring-boot:run
```

## Features

- Web crawling with configurable entry points
- Storage of page content, title, and last modified date
- Tracking of parent-child relationships between pages
- RESTful API for interacting with the crawler
- H2 in-memory database with persistence

## API Endpoints

- `POST /api/crawler/start`: Start crawling from the default or specified URL
- `GET /api/crawler/pages`: Get all crawled pages
- `GET /api/crawler/pages/{url}`: Get a specific page by URL

## Configuration

Configuration options can be modified in `application.properties`:

- `crawler.max-pages`: Maximum number of pages to crawl (default: 300)
- `crawler.start-url`: Default starting URL for the crawler
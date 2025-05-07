package hk.ust.csit5930;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import hk.ust.csit5930.mapper.PageMapper;
import hk.ust.csit5930.model.Page;
import hk.ust.csit5930.model.Relationship;

/**
 * An operator that provides methods to interact with the H2 database.
 */
public class H2DBOperator {
    private SqlSessionFactory sqlSessionFactory;
    // private PageMapper pageMapper;

    /**
     * Initialises a SqlSessionFactory using the MyBatis configuration file.
     */
    public H2DBOperator() {
        // Lazy load the SQL session factory
        String resource = "mybatis-config.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            System.err.println("Failed to initialize SQL session factory: " + e.getMessage());
        }
    }

    public void setup() {
        H2DBSetup.setup();
    }

    /**
     * Inserts a new page into the database.
     *
     * @param url          The URL of the page.
     * @param title        The title of the page.
     * @param content      The content of the page.
     * @param lastModified The last modified timestamp of the page.
     * @param size         The size of the page content.
     */
    public void insert (String url, String title, String content, Timestamp lastModified, int size) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageMapper mapper = session.getMapper(PageMapper.class);
            Page page = new Page(url, title, content, lastModified, size);
            mapper.insert(page);
            session.commit();
        } catch (NullPointerException e) {
            System.err.println("Failed to open a session");
            e.printStackTrace();
        } catch (PersistenceException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Failed to insert page: " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    public int updateById (String url, String title, String content, Timestamp lastModified, int size) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageMapper mapper = session.getMapper(PageMapper.class);
            Page page = new Page(url, title, content, lastModified, size);
            return mapper.updateById(page);
        } catch (Exception e) {
            // System.err.println("Failed to update page: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    public List<Page> getAllPages() {
        List<Page> pages = null;
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageMapper mapper = session.getMapper(PageMapper.class);
            pages = mapper.getAllPages();
        } catch (Exception e) {
            System.err.println("Failed to update page: " + e.getMessage());
            System.exit(0);
        }
        return pages;
    }

    /**
     * Inserts a relationship between two pages into the database.
     *
     * @param parentUrl The URL of the parent page.
     * @param childUrl  The URL of the child page.
     */
    public void insertRelationship(String parentUrl, String childUrl) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageMapper mapper = session.getMapper(PageMapper.class);
            mapper.insertRelationship(parentUrl, childUrl);
            session.commit();
        } catch (Exception e) {
            System.err.println("Failed to insert relationship: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Relationship> getAllRelationships() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageMapper mapper = session.getMapper(PageMapper.class);
            return mapper.getAllRelationships();
        } catch (Exception e) {
            System.err.println("Failed to get all relationships: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // public void insertPlaceHolder(String url) {
    //     try (SqlSession session = sqlSessionFactory.openSession()) {
    //         PageMapper mapper = session.getMapper(PageMapper.class);
    //         mapper.insertPlaceHolder (url);
    //         session.commit();
    //     } catch (Exception e) {
    //         System.err.println("Failed to insert placeholder: " + e.getMessage());
    //         e.printStackTrace();
    //     }
    // }

    // public Page getPageById(int id) {
    //     try (SqlSession session = sqlSessionFactory.openSession()) {
    //         PageMapper mapper = session.getMapper(PageMapper.class);
    //         return mapper.getPageById(id);
    //     } catch (Exception e) {
    //         System.err.println("Failed to get page by ID: " + e.getMessage());
    //         return null;
    //     }
    // }


    /**
     * Retrieves the page ID for a given URL.
     *
     * @param url The URL of the page.
     * @return The page ID, or -1 if not found.
     */
    public int getPageId(String url) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageMapper mapper = session.getMapper(PageMapper.class);
            return mapper.getPageId(url);
        } catch (Exception e) {
            System.err.println("Failed to get page id by URL: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Retrieves the page ID for a given URL (alias for getPageId).
     *
     * @param url The URL of the page.
     * @return The page ID, or -1 if not found.
     */
    public int getIdFromUrl(String url) {
        return getPageId(url);
    }

    public String getContent(int id) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageMapper mapper = session.getMapper(PageMapper.class);
            return mapper.getContent(id);
        } catch (Exception e) {
            System.err.println("Failed to get content by ID: " + e.getMessage());
            return null;
        }
    }

    public String getUrl(int id) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageMapper mapper = session.getMapper(PageMapper.class);
            return mapper.getUrl(id);
        } catch (Exception e) {
            System.err.println("Failed to get URL by ID: " + e.getMessage());
            return null;
        }
    }

    public String getTitle(int id) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageMapper mapper = session.getMapper(PageMapper.class);
            return mapper.getTitle(id);
        } catch (Exception e) {
            System.err.println("Failed to get title by ID: " + e.getMessage());
            return null;
        }
    }
    
    public Page getPage(int id) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageMapper mapper = session.getMapper(PageMapper.class);
            return mapper.getPage(id);
        } catch (Exception e) {
            System.err.println("Failed to get page by ID: " + e.getMessage());
            return null;
        }
    }
}
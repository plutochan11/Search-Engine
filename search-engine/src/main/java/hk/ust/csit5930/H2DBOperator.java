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

public class H2DBOperator {
    private SqlSessionFactory sqlSessionFactory;
    // private PageMapper pageMapper;

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

    public void insert (String url, String title, String content, Timestamp lastModified) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageMapper mapper = session.getMapper(PageMapper.class);
            Page page = new Page(url, title, content, lastModified);
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

    public int updateById (String url, String title, String content, Timestamp lastModified) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageMapper mapper = session.getMapper(PageMapper.class);
            Page page = new Page(url, title, content, lastModified);
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
            pages = mapper.getAll();
        } catch (Exception e) {
            System.err.println("Failed to update page: " + e.getMessage());
            System.exit(0);
        }
        return pages;
    }

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

    // public Page getPageByUrl(String url) {
    //     try (SqlSession session = sqlSessionFactory.openSession()) {
    //         PageMapper mapper = session.getMapper(PageMapper.class);
    //         return mapper.getPageByUrl(url);
    //     } catch (Exception e) {
    //         System.err.println("Failed to get page by URL: " + e.getMessage());
    //         return null;
    //     }
    // }
}
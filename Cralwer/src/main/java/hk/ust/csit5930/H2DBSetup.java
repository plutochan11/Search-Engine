package hk.ust.csit5930;

import java.io.InputStream;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import hk.ust.csit5930.mapper.DBSetupMapper;

public class H2DBSetup {
    private static SqlSessionFactory sqlSessionFactory;

    static {
        String resource = "mybatis-config.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (Exception e) {
            System.err.println("Failed to initialize SQL session factory: ");
            e.printStackTrace();
        }
    }

    public static void setup() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            DBSetupMapper mapper = session.getMapper(DBSetupMapper.class);
            mapper.createPagesTable();
            mapper.createRelationshipsTable();
            session.commit();
            System.out.println("Database setup completed successfully.");
        } catch (Exception e) {
            System.err.println("Failed to setup database: ");
            e.printStackTrace();
        }
    }
}
